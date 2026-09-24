package br.com.sprint1.challenge.service.impl;

import br.com.sprint1.challenge.service.TotpService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Implementação RFC 6238 (TOTP) usada pelo fluxo de MFA. Extraída de
 * {@code AuthServiceImpl} (SRP: geração/verificação de código de uso único é
 * uma responsabilidade própria, independente de autenticação/lockout/refresh).
 */
@Service
public class TotpServiceImpl implements TotpService {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    @Override
    public String generateSecret() {
        byte[] randomBytes = new byte[20];
        new SecureRandom().nextBytes(randomBytes);
        return base32Encode(randomBytes);
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        try {
            long timeWindow = System.currentTimeMillis() / 1000 / 30;
            return verifyTotp(secret, code, timeWindow);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyTotp(String secret, String code, long timeWindow) {
        try {
            byte[] key = base32Decode(secret);
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (timeWindow & 0xFF);
                timeWindow >>= 8;
            }

            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xF;
            int truncatedHash = 0;
            for (int i = 0; i < 4; i++) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }

            truncatedHash &= 0x7FFFFFFF;
            int totp = truncatedHash % 1000000;
            String expectedCode = String.format("%06d", totp);

            // Tolera 1 janela de tempo antes/depois (clock drift)
            return expectedCode.equals(code)
                || String.format("%06d", ((truncatedHash + 1) % 1000000)).equals(code)
                || String.format("%06d", ((truncatedHash - 1 + 1000000) % 1000000)).equals(code);
        } catch (Exception e) {
            return false;
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }

    private byte[] base32Decode(String encoded) {
        String sanitized = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int bits = sanitized.length() * 5;
        byte[] result = new byte[bits / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : sanitized.toCharArray()) {
            int val = ALPHABET.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return result;
    }
}
