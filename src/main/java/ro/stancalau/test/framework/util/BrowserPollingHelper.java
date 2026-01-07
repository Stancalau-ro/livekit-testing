package ro.stancalau.test.framework.util;

import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionTimeoutException;

@Slf4j
@UtilityClass
public class BrowserPollingHelper {

    public static final long DEFAULT_DELAY_MS = 500;
    public static final long DEFAULT_TIMEOUT_MS = 30_000;

    public <T> T pollUntil(Supplier<T> supplier, Predicate<T> condition, long timeoutMs, long delayMs) {
        AtomicReference<T> lastResult = new AtomicReference<>();
        AtomicInteger attempt = new AtomicInteger(0);

        try {
            await().atMost(Duration.ofMillis(timeoutMs))
                    .pollInterval(Duration.ofMillis(delayMs))
                    .pollInSameThread()
                    .until(() -> {
                        attempt.incrementAndGet();
                        T result = supplier.get();
                        lastResult.set(result);
                        return condition.test(result);
                    });
            return lastResult.get();
        } catch (ConditionTimeoutException e) {
            log.warn("pollUntil timed out after {}ms ({} attempts)", timeoutMs, attempt.get());
            return lastResult.get();
        }
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
        AtomicReference<T> lastResult = new AtomicReference<>();
        AtomicReference<Exception> lastException = new AtomicReference<>();

        try {
            await().atMost(Duration.ofMillis(timeoutMs))
                    .pollInterval(Duration.ofMillis(delayMs))
                    .pollInSameThread()
                    .until(() -> {
                        try {
                            T result = supplier.get();
                            lastResult.set(result);
                            return condition.test(result);
                        } catch (Exception e) {
                            lastException.set(e);
                            throw e;
                        }
                    });
            return lastResult.get();
        } catch (ConditionTimeoutException e) {
            Exception cause = lastException.get();
            if (cause != null) {
                throw new TestTimeoutException(
                        operation,
                        context + " (exception: " + cause.getMessage() + ")",
                        System.currentTimeMillis() - startTimeMs,
                        cause);
            }
            throw new TestTimeoutException(operation, context, System.currentTimeMillis() - startTimeMs);
        }
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

    public void safeSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("Sleep interrupted after {}ms", millis);
        }
    }
}
