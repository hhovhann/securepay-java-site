package ae.securepay.website.guide;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-20T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void twentyQuestionsPerVisitorInAFixedTenMinuteWindow() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(clock);
        for (int i = 0; i < RateLimiter.LIMIT; i++) {
            assertFalse(limiter.limited("198.51.100.7"), "question " + (i + 1));
            clock.advance(Duration.ofSeconds(20));
        }
        assertTrue(limiter.limited("198.51.100.7"), "21st question");
        assertFalse(limiter.limited("198.51.100.8"), "another visitor is not affected");
        clock.advance(Duration.ofMinutes(4));
        assertFalse(limiter.limited("198.51.100.7"), "window has ended (later questions did not extend it)");
    }
}
