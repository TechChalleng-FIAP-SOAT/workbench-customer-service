package com.workbench.workshop.application.user;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptLimiterTest {

    private static final String EMAIL = "admin@workbench.dev";

    @Test
    void locksAfterMaxFailuresAndUnlocksAfterLockTime() {
        Instant now = Instant.parse("2026-11-11T10:00:00Z");
        var limiter = new LoginAttemptLimiter(Clock.fixed(now, ZoneOffset.UTC));
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            limiter.recordFailure(EMAIL);
        }
        assertTrue(limiter.isLocked(EMAIL));

        var later = new LoginAttemptLimiter(Clock.fixed(now.plus(LoginAttemptLimiter.LOCK_TIME).plusSeconds(1), ZoneOffset.UTC));
        assertFalse(later.isLocked(EMAIL)); // instância nova: nada registrado
    }

    @Test
    void successResetsTheCount() {
        var limiter = new LoginAttemptLimiter();
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) {
            limiter.recordFailure(EMAIL);
        }
        limiter.recordSuccess(EMAIL);
        limiter.recordFailure(EMAIL);
        assertFalse(limiter.isLocked(EMAIL));
    }

    @Test
    void manyInventedEmailsDoNotGrowWithoutBoundButKeepLocks() {
        var limiter = new LoginAttemptLimiter();
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            limiter.recordFailure(EMAIL);
        }
        for (int i = 0; i < LoginAttemptLimiter.MAX_TRACKED + 500; i++) {
            limiter.recordFailure("ghost" + i + "@x.com");
        }
        assertTrue(limiter.trackedCount() <= LoginAttemptLimiter.MAX_TRACKED);
        assertTrue(limiter.isLocked(EMAIL)); // o bloqueio real sobrevive à limpeza
    }
}
