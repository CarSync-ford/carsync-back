package br.com.sprint1.challenge.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;

/**
 * TOTP (RFC 6238) utility with injectable Clock for testing.
 * Uses HMAC-SHA1, 30-second steps, 6-digit codes, and ±1 window for clock drift.
 */
public final class TotpUtil {

    private static final int STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int MOD = 1_000_000; // 10^6

    private TotpUtil() {}

    /**
     * Verify a TOTP code against the given secret and clock.
     * Accepts current step and ±1 window.
     * Returns the accepted time step, or -1 if invalid.
     */
    public static long verify(String base32Secret, String code, Clock clock, Long lastUsedStep) {
        if (code == null || code.length() != CODE_DIGITS) return -1;
        byte[] key = base32Decode(base32Secret);
        long currentStep = clock.instant().getEpochSecond() / STEP_SECONDS;

        for (int offset = -1; offset <= 1; offset++) {
            long step = currentStep + offset;
            if (lastUsedStep != null && step <= lastUsedStep) continue; // anti-replay
            String expected = generateCode(key, step);
            if (expected.equals(code)) return step;
        }
        return -1;
    }

    /** Generate the 6-digit TOTP code for a given step. Package-visible for test vectors. */
    public static String generateCode(byte[] key, long step) {
        byte[] data = new byte[8];
        long val = step;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) (val & 0xFF);
            val >>= 8;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xF;
            int truncated = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return String.format("%06d", truncated % MOD);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA1 unavailable", e);
        }
    }

    /** Decode RFC 4648 Base32 (no padding required). */
    public static byte[] base32Decode(String encoded) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        encoded = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int bits = encoded.length() * 5;
        byte[] result = new byte[bits / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;
        for (char c : encoded.toCharArray()) {
            int val = alphabet.indexOf(c);
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

    /** Encode bytes to RFC 4648 Base32 (no padding). */
    public static String base32Encode(byte[] data) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(alphabet.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(alphabet.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return sb.toString();
    }
}
