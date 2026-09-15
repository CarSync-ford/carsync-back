package br.com.sprint1.challenge.util;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests TotpUtil against RFC 6238 Appendix B test vectors (SHA1)
 * and validates window/anti-replay behavior.
 */
class TotpUtilTest {

    // RFC 6238 test secret: "12345678901234567890" (ASCII) = base32 GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    private static final byte[] RFC_KEY = "12345678901234567890".getBytes();

    // RFC 6238 Appendix B SHA1 test vectors: (time, expected_code)
    // time=59        → step=1  → code=287082
    // time=1111111109 → step=37037036 → code=081804
    // time=1111111111 → step=37037037 → code=050471
    // time=1234567890 → step=41152263 → code=005924
    // time=2000000000 → step=66666666 → code=279037

    @Test
    void rfcVector_time59() {
        assertEquals("287082", TotpUtil.generateCode(RFC_KEY, 1));
    }

    @Test
    void rfcVector_time1111111109() {
        assertEquals("081804", TotpUtil.generateCode(RFC_KEY, 37037036));
    }

    @Test
    void rfcVector_time1111111111() {
        assertEquals("050471", TotpUtil.generateCode(RFC_KEY, 37037037));
    }

    @Test
    void rfcVector_time1234567890() {
        assertEquals("005924", TotpUtil.generateCode(RFC_KEY, 41152263));
    }

    @Test
    void rfcVector_time2000000000() {
        assertEquals("279037", TotpUtil.generateCode(RFC_KEY, 66666666));
    }

    @Test
    void verify_currentStep_accepted() {
        long step = 100;
        Clock clock = Clock.fixed(Instant.ofEpochSecond(step * 30), ZoneOffset.UTC);
        String code = TotpUtil.generateCode(RFC_KEY, step);

        long result = TotpUtil.verify(RFC_SECRET, code, clock, null);
        assertEquals(step, result);
    }

    @Test
    void verify_previousStep_accepted() {
        long step = 100;
        Clock clock = Clock.fixed(Instant.ofEpochSecond(step * 30), ZoneOffset.UTC);
        String prevCode = TotpUtil.generateCode(RFC_KEY, step - 1);

        long result = TotpUtil.verify(RFC_SECRET, prevCode, clock, null);
        assertEquals(step - 1, result);
    }

    @Test
    void verify_nextStep_accepted() {
        long step = 100;
        Clock clock = Clock.fixed(Instant.ofEpochSecond(step * 30), ZoneOffset.UTC);
        String nextCode = TotpUtil.generateCode(RFC_KEY, step + 1);

        long result = TotpUtil.verify(RFC_SECRET, nextCode, clock, null);
        assertEquals(step + 1, result);
    }

    @Test
    void verify_twoStepsAway_rejected() {
        long step = 100;
        Clock clock = Clock.fixed(Instant.ofEpochSecond(step * 30), ZoneOffset.UTC);
        String farCode = TotpUtil.generateCode(RFC_KEY, step + 2);

        assertEquals(-1, TotpUtil.verify(RFC_SECRET, farCode, clock, null));
    }

    @Test
    void verify_numericNeighbor_notAccepted() {
        // A code that is numerically adjacent to a valid code should NOT pass
        long step = 100;
        Clock clock = Clock.fixed(Instant.ofEpochSecond(step * 30), ZoneOffset.UTC);
        String validCode = TotpUtil.generateCode(RFC_KEY, step);
        int numeric = Integer.parseInt(validCode);
        String neighbor = String.format("%06d", (numeric + 1) % 1000000);

        // The neighbor may or may not match a different step, but should not
        // be accepted as the same step's code
        if (!neighbor.equals(validCode)) {
            // Unless neighbor happens to match step-1 or step+1 (astronomically unlikely), reject
            String prevCode = TotpUtil.generateCode(RFC_KEY, step - 1);
            String nextCode = TotpUtil.generateCode(RFC_KEY, step + 1);
            if (!neighbor.equals(prevCode) && !neighbor.equals(nextCode)) {
                assertEquals(-1, TotpUtil.verify(RFC_SECRET, neighbor, clock, null));
            }
        }
    }

    @Test
    void verify_antiReplay_rejectsAlreadyUsedStep() {
        long step = 100;
        Clock clock = Clock.fixed(Instant.ofEpochSecond(step * 30), ZoneOffset.UTC);
        String code = TotpUtil.generateCode(RFC_KEY, step);

        // First use succeeds
        assertEquals(step, TotpUtil.verify(RFC_SECRET, code, clock, null));
        // Replay fails
        assertEquals(-1, TotpUtil.verify(RFC_SECRET, code, clock, step));
    }

    @Test
    void verify_antiReplay_rejectsOlderStep() {
        long step = 100;
        Clock clock = Clock.fixed(Instant.ofEpochSecond(step * 30), ZoneOffset.UTC);
        String prevCode = TotpUtil.generateCode(RFC_KEY, step - 1);

        // If lastUsedStep is current step, prev step is rejected
        assertEquals(-1, TotpUtil.verify(RFC_SECRET, prevCode, clock, step));
    }

    @Test
    void verify_nullCode_rejected() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(3000), ZoneOffset.UTC);
        assertEquals(-1, TotpUtil.verify(RFC_SECRET, null, clock, null));
    }

    @Test
    void verify_wrongLengthCode_rejected() {
        Clock clock = Clock.fixed(Instant.ofEpochSecond(3000), ZoneOffset.UTC);
        assertEquals(-1, TotpUtil.verify(RFC_SECRET, "12345", clock, null));
        assertEquals(-1, TotpUtil.verify(RFC_SECRET, "1234567", clock, null));
    }

    @Test
    void base32_roundTrip() {
        byte[] original = "Hello!".getBytes();
        String encoded = TotpUtil.base32Encode(original);
        byte[] decoded = TotpUtil.base32Decode(encoded);
        assertArrayEquals(original, decoded);
    }
}
