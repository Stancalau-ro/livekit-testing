package ro.stancalau.test.framework.util;

import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class BrowserPollingHelper {

    public static final long DEFAULT_DELAY_MS = 500;
    public static final long DEFAULT_TIMEOUT_MS = 30_000;

    public <T> T pollUntil(Supplier<T> supplier, Predicate<T> condition, long timeoutMs, long delayMs) {
        long timeoutAtMs = System.currentTimeMillis() + timeoutMs;
        T lastResult = null;
        int attempt = 0;
        while (System.currentTimeMillis() < timeoutAtMs) {
            attempt++;
            lastResult = supplier.get();
            if (condition.test(lastResult)) {
                return lastResult;
            }
            sleepUntilNextPoll(timeoutAtMs, delayMs);
        }
        log.warn("pollUntil timed out after {}ms ({} attempts)", timeoutMs, attempt);
        return lastResult;
    }

    public <T> T pollUntil(Supplier<T> supplier, Predicate<T> condition) {
        return pollUntil(supplier, condition, DEFAULT_TIMEOUT_MS, DEFAULT_DELAY_MS);
    }

    public <T> T pollUntilOrThrow(
            Supplier<T> supplier,
            Predicate<T> condition,
            long timeoutMs,
            long delayMs,
            String operation,
            String context) {
        long startTimeMs = System.currentTimeMillis();
        long timeoutAtMs = startTimeMs + timeoutMs;
        int attempt = 0;
        while (System.currentTimeMillis() < timeoutAtMs) {
            attempt++;
            try {
                T result = supplier.get();
                if (condition.test(result)) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("Poll attempt {} failed for {}: {}", attempt, operation, e.getMessage());
                throw new TestTimeoutException(
                        operation,
                        context + " (exception: " + e.getMessage() + ")",
                        System.currentTimeMillis() - startTimeMs,
                        e);
            }
            sleepUntilNextPoll(timeoutAtMs, delayMs);
        }
        throw new TestTimeoutException(operation, context, System.currentTimeMillis() - startTimeMs);
    }

    public <T> T pollUntilOrThrow(Supplier<T> supplier, Predicate<T> condition, String operation, String context) {
        return pollUntilOrThrow(supplier, condition, DEFAULT_TIMEOUT_MS, DEFAULT_DELAY_MS, operation, context);
    }

    public boolean pollForCondition(Supplier<Boolean> condition, long timeoutMs, long delayMs) {
        Boolean result = pollUntil(condition, Boolean.TRUE::equals, timeoutMs, delayMs);
        return Boolean.TRUE.equals(result);
    }

    public boolean pollForCondition(Supplier<Boolean> condition) {
        return pollForCondition(condition, DEFAULT_TIMEOUT_MS, DEFAULT_DELAY_MS);
    }

    public void pollForConditionOrThrow(
            Supplier<Boolean> condition, long timeoutMs, long delayMs, String operation, String context) {
        pollUntilOrThrow(condition, Boolean.TRUE::equals, timeoutMs, delayMs, operation, context);
    }

    public void pollForConditionOrThrow(Supplier<Boolean> condition, String operation, String context) {
        pollForConditionOrThrow(condition, DEFAULT_TIMEOUT_MS, DEFAULT_DELAY_MS, operation, context);
    }

    private void sleepUntilNextPoll(long timeoutAtMs, long delayMs) {
        long remainingMs = timeoutAtMs - System.currentTimeMillis();
        if (remainingMs > delayMs) {
            safeSleep(delayMs);
        } else if (remainingMs > 0) {
            safeSleep(remainingMs);
        }
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
