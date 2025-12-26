package ro.stancalau.test.framework.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;
import java.util.function.Supplier;

@Slf4j
@UtilityClass
public class BrowserPollingHelper {

    public static final int DEFAULT_MAX_ATTEMPTS = 10;
    public static final int EXTENDED_MAX_ATTEMPTS = 20;
    public static final long DEFAULT_DELAY_MS = 500;

    public <T> T pollUntil(Supplier<T> supplier, Predicate<T> condition, int maxAttempts, long delayMs) {
        T lastResult = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            lastResult = supplier.get();
            if (condition.test(lastResult)) {
                return lastResult;
            }
            if (attempt < maxAttempts - 1) {
                safeSleep(delayMs);
            }
        }
        return lastResult;
    }

    public <T> T pollUntil(Supplier<T> supplier, Predicate<T> condition) {
        return pollUntil(supplier, condition, DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY_MS);
    }

    public boolean pollForCondition(Supplier<Boolean> condition, int maxAttempts, long delayMs) {
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            Boolean result = condition.get();
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
            if (attempt < maxAttempts - 1) {
                safeSleep(delayMs);
            }
        }
        return false;
    }

    public void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Sleep interrupted after {}ms", millis);
        }
    }
}
