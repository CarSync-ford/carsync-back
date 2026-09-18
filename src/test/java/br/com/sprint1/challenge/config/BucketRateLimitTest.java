package br.com.sprint1.challenge.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class BucketRateLimitTest {

    @Test
    void shouldAllowUpToLimitAndRejectAfter() {
        Bucket bucket = Bucket.builder()
            .addLimit(Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofSeconds(1)).build())
            .build();

        for (int i = 0; i < 10; i++) {
            assertTrue(bucket.tryConsume(1), "Request " + (i + 1) + " should be allowed");
        }

        assertFalse(bucket.tryConsume(1), "11th request should be rejected");
    }
}
