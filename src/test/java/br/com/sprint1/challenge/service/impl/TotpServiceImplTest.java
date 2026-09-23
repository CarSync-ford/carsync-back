package br.com.sprint1.challenge.service.impl;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpServiceImplTest {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final TotpServiceImpl totpService = new TotpServiceImpl();

    @Test
    void generateSecret_retornaBase32NaoVazio() {
        String secret = totpService.generateSecret();

        assertNotNull(secret);
        assertTrue(secret.matches("[A-Z2-7]+"), "secret deve ser Base32 valido");
    }

    @Test
    void generateSecret_cadaChamadaGeraSecretDiferente() {
        String first = totpService.generateSecret();
        String second = totpService.generateSecret();

        assertFalse(first.equals(second));
    }

    @Test
    void verifyCode_codigoValido_retornaTrue() throws Exception {
        String secret = totpService.generateSecret();
        String validCode = computeReferenceTotp(secret, currentTimeWindow());

        assertTrue(totpService.verifyCode(secret, validCode));
    }

    @Test
    void verifyCode_codigoInvalido_retornaFalse() {
        String secret = totpService.generateSecret();

        assertFalse(totpService.verifyCode(secret, "000000"));
    }

    @Test
    void verifyCode_secretInvalido_retornaFalseSemLancarExcecao() {
        assertFalse(totpService.verifyCode("not-a-valid-secret!!!", "123456"));
    }

    private long currentTimeWindow() {
        return System.currentTimeMillis() / 1000 / 30;
    }

    // Implementacao de referencia (RFC 6238) independente, usada so para validar
    // que TotpServiceImpl produz/aceita o mesmo codigo que o algoritmo padrao geraria.
    private String computeReferenceTotp(String secret, long timeWindow) throws Exception {
        byte[] key = base32Decode(secret);
        byte[] data = new byte[8];
        long tw = timeWindow;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (tw & 0xFF);
            tw >>= 8;
        }

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        int truncatedHash = 0;
        for (int i = 0; i < 4; i++) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }
        truncatedHash &= 0x7FFFFFFF;
        return String.format("%06d", truncatedHash % 1000000);
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
