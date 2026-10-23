package com.taekwondo.examenes.auth;

import com.taekwondo.examenes.common.TooManyAttemptsException;
import com.taekwondo.examenes.security.RateLimitFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de inicios de sesión fallidos (RNF-07). Los correctos no cuentan: una clase entera entra a la vez
 * desde la misma dirección de su router. Hay dos cupos:
 * <ul>
 *   <li>por dirección: fallos por minuto contra cualquier cuenta, para un script que prueba muchas;</li>
 *   <li>por cuenta: al 5.º fallo seguido espera 30 s, venga de donde venga, para quien intenta adivinar
 *       la contraseña de alguien.</li>
 * </ul>
 * Acertar pone a cero los fallos de la cuenta. Se apaga con app.rate-limit.enabled=false.
 */
@Component
public class LoginThrottle {

    private static final String ACCOUNT_LOCKED =
            "Demasiados intentos fallidos con esta cuenta. Espera %d segundos y vuelve a intentarlo.";
    private static final Duration WINDOW = Duration.ofMinutes(1);
    /** Los fallos sueltos de una cuenta se olvidan pasado este tiempo sin más fallos. */
    private static final Duration FORGET_AFTER = Duration.ofMinutes(15);
    /** Por encima de tantas entradas guardadas se olvidan las que ya no cuentan. */
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final boolean enabled;
    private final int failuresPerMinute;
    private final int accountFailures;
    private final Duration accountWait;
    private final Clock clock;
    private final Map<String, Window> addresses = new ConcurrentHashMap<>();
    private final Map<String, AccountFailures> accounts = new ConcurrentHashMap<>();

    public LoginThrottle(@Value("${app.rate-limit.enabled:true}") boolean enabled,
                         @Value("${app.rate-limit.login-failures-per-minute:50}") int failuresPerMinute,
                         @Value("${app.rate-limit.login-account-failures:5}") int accountFailures,
                         @Value("${app.rate-limit.login-account-wait-seconds:30}") int accountWaitSeconds,
                         Clock clock) {
        this.enabled = enabled;
        this.failuresPerMinute = failuresPerMinute;
        this.accountFailures = accountFailures;
        this.accountWait = Duration.ofSeconds(accountWaitSeconds);
        this.clock = clock;
    }

    /** Antes de mirar la contraseña: 429 si la dirección o la cuenta tienen que esperar. */
    public void checkAllowed(String account, String address) {
        if (!enabled) return;
        Instant now = clock.instant();
        Window window = addresses.get(address);
        if (window != null && !window.isOverAt(now) && window.count() >= failuresPerMinute) {
            throw new TooManyAttemptsException(RateLimitFilter.TOO_MANY,Duration.between(now, window.end()));
        }
        AccountFailures failures = accounts.get(account);
        if (failures != null && failures.isLockedAt(now)) {
            throw new TooManyAttemptsException(ACCOUNT_LOCKED.formatted(accountWait.toSeconds()),
                    Duration.between(now, failures.lockedUntil()));
        }
    }

    public void recordFailure(String account, String address) {
        if (!enabled) return;
        Instant now = clock.instant();
        addresses.compute(address,
                (key, current) -> current == null || current.isOverAt(now) ? new Window(now, 1) : current.next());
        accounts.compute(account, (key, current) -> {
            int consecutive = (current == null ? 0 : current.consecutive()) + 1;
            return consecutive >= accountFailures
                    ? new AccountFailures(0, now, now.plus(accountWait))
                    : new AccountFailures(consecutive, now, null);
        });
        forgetOld(now);
    }

    public void recordSuccess(String account) {
        if (!enabled) return;
        accounts.remove(account);
    }

    private void forgetOld(Instant now) {
        if (addresses.size() > CLEANUP_THRESHOLD) {
            addresses.values().removeIf(w -> w.isOverAt(now));
        }
        if (accounts.size() > CLEANUP_THRESHOLD) {
            accounts.values().removeIf(f -> !f.isLockedAt(now) && f.lastFailure().plus(FORGET_AFTER).isBefore(now));
        }
    }

    /** Fallos de una dirección en el minuto que empezó en start. */
    private record Window(Instant start, int count) {
        Instant end() {
            return start.plus(WINDOW);
        }

        boolean isOverAt(Instant now) {
            return !now.isBefore(end());
        }

        Window next() {
            return new Window(start, count + 1);
        }
    }

    /** Fallos seguidos de una cuenta; lockedUntil, si ahora tiene que esperar. */
    private record AccountFailures(int consecutive, Instant lastFailure, Instant lockedUntil) {
        boolean isLockedAt(Instant now) {
            return lockedUntil != null && now.isBefore(lockedUntil);
        }
    }
}
