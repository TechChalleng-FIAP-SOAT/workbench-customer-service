package com.workbench.workshop.application.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueia temporariamente um e-mail depois de muitas senhas erradas seguidas, para
 * dificultar tentativa e erro no {@code /api/users/auth/validate} (rota pública).
 *
 * <p>shortcut: contagem em memória, por réplica — com N pods o limite efetivo é N×;
 * trocar por contador compartilhado (tabela/Redis) se o serviço ficar exposto à internet.
 */
@Component
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_TIME = Duration.ofMinutes(15);
    /** Teto de e-mails acompanhados: muitos e-mails inventados não podem esgotar a memória. */
    static final int MAX_TRACKED = 10_000;

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    /** Construtor usado pelo Spring (relógio do sistema). */
    @Autowired
    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    /** Para testes: relógio controlado. */
    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isLocked(String email) {
        Attempts a = attempts.get(email);
        return a != null && a.lockedUntil != null && clock.instant().isBefore(a.lockedUntil);
    }

    public void recordFailure(String email) {
        if (attempts.size() >= MAX_TRACKED) {
            forgetUnlocked();
        }
        attempts.compute(email, (k, a) -> {
            Attempts next = (a == null || (a.lockedUntil != null && !clock.instant().isBefore(a.lockedUntil)))
                    ? new Attempts(0, null) : a;
            int failures = next.failures + 1;
            return new Attempts(failures, failures >= MAX_FAILURES ? clock.instant().plus(LOCK_TIME) : null);
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(email);
    }

    int trackedCount() {
        return attempts.size();
    }

    // Mantém só os bloqueios em vigor; contagens parciais são descartadas.
    private void forgetUnlocked() {
        Instant now = clock.instant();
        attempts.values().removeIf(a -> a.lockedUntil == null || !now.isBefore(a.lockedUntil));
    }

    private record Attempts(int failures, Instant lockedUntil) {}
}
