package ro.stancalau.test.framework.js;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

@Slf4j
public class JsExecutor {

  private static final String DEFAULT_NAMESPACE = "LiveKitTestHelpers";

  private final WebDriver driver;
  private final String namespace;

  public JsExecutor(WebDriver driver) {
    this(driver, DEFAULT_NAMESPACE);
  }

  public JsExecutor(WebDriver driver, String namespace) {
    this.driver = driver;
    this.namespace = namespace;
  }

  public <T> JsResult<T> execute(String functionName, Class<T> returnType, Object... args) {
    String script = buildScript(functionName, args.length);
    try {
      long startTime = System.currentTimeMillis();
      Object result = getExecutor().executeScript(script, args);
      long duration = System.currentTimeMillis() - startTime;
      log.debug("JS call {}() completed in {}ms", functionName, duration);
      return JsResult.success(convertResult(result, returnType));
    } catch (Exception e) {
      log.warn("JS call {}() failed: {}", functionName, e.getMessage());
      return JsResult.failure(e);
    }
  }

  public void executeVoid(String functionName, Object... args) {
    String script = buildVoidScript(functionName, args.length);
    try {
      long startTime = System.currentTimeMillis();
      getExecutor().executeScript(script, args);
      long duration = System.currentTimeMillis() - startTime;
      log.debug("JS void call {}() completed in {}ms", functionName, duration);
    } catch (Exception e) {
      log.warn("JS void call {}() failed: {}", functionName, e.getMessage());
      throw new JsExecutionException(functionName, e.getMessage(), e);
    }
  }

  public <T> JsResult<T> executeAsync(
      String functionName, Class<T> returnType, long timeoutMs, Object... args) {
    String script = buildAsyncScript(functionName, args.length, timeoutMs);
    try {
      driver.manage().timeouts().scriptTimeout(Duration.ofMillis(timeoutMs));
      long startTime = System.currentTimeMillis();
      Object result = getExecutor().executeAsyncScript(script, args);
      long duration = System.currentTimeMillis() - startTime;
      log.debug("JS async call {}() completed in {}ms", functionName, duration);
      return JsResult.success(convertResult(result, returnType));
    } catch (Exception e) {
      log.warn("JS async call {}() failed: {}", functionName, e.getMessage());
      return JsResult.failure(e);
    }
  }

  private String buildScript(String functionName, int argCount) {
    StringBuilder argsBuilder = new StringBuilder();
    for (int i = 0; i < argCount; i++) {
      if (i > 0) argsBuilder.append(", ");
      argsBuilder.append("arguments[").append(i).append("]");
    }
    return String.format("return window.%s.%s(%s);", namespace, functionName, argsBuilder);
  }

  private String buildVoidScript(String functionName, int argCount) {
    StringBuilder argsBuilder = new StringBuilder();
    for (int i = 0; i < argCount; i++) {
      if (i > 0) argsBuilder.append(", ");
      argsBuilder.append("arguments[").append(i).append("]");
    }
    return String.format("window.%s.%s(%s);", namespace, functionName, argsBuilder);
  }

  private String buildAsyncScript(String functionName, int argCount, long timeoutMs) {
    StringBuilder argsBuilder = new StringBuilder();
    for (int i = 0; i < argCount; i++) {
      argsBuilder.append("arguments[").append(i).append("], ");
    }
    return String.format(
        "var callback = arguments[arguments.length - 1];"
            + "window.%s.%s(%s%d)"
            + ".then(function(result) { callback(result); })"
            + ".catch(function(err) { callback(null); });",
        namespace, functionName, argsBuilder, timeoutMs);
  }

  private JavascriptExecutor getExecutor() {
    return (JavascriptExecutor) driver;
  }

  @SuppressWarnings("unchecked")
  private <T> T convertResult(Object result, Class<T> returnType) {
    if (result == null) {
      return getDefaultValue(returnType);
    }

    if (returnType == Boolean.class || returnType == boolean.class) {
      if (result instanceof Boolean) {
        return (T) result;
      }
      return (T) Boolean.FALSE;
    }

    if (returnType == Long.class || returnType == long.class) {
      if (result instanceof Number) {
        return (T) Long.valueOf(((Number) result).longValue());
      }
      return (T) Long.valueOf(0L);
    }

    if (returnType == Integer.class || returnType == int.class) {
      if (result instanceof Number) {
        return (T) Integer.valueOf(((Number) result).intValue());
      }
      return (T) Integer.valueOf(0);
    }

    if (returnType == Double.class || returnType == double.class) {
      if (result instanceof Number) {
        return (T) Double.valueOf(((Number) result).doubleValue());
      }
      return (T) Double.valueOf(0.0);
    }

    if (returnType == String.class) {
      return (T) result.toString();
    }

    if (returnType == Map.class && result instanceof Map) {
      return (T) result;
    }

    if (returnType == List.class && result instanceof List) {
      return (T) result;
    }

    if (returnType == Object.class) {
      return (T) result;
    }

    if (returnType == Void.class || returnType == void.class) {
      return null;
    }

    return (T) result;
  }

  @SuppressWarnings("unchecked")
  private <T> T getDefaultValue(Class<T> returnType) {
    if (returnType == Boolean.class || returnType == boolean.class) {
      return (T) Boolean.FALSE;
    }
    if (returnType == Long.class || returnType == long.class) {
      return (T) Long.valueOf(0L);
    }
    if (returnType == Integer.class || returnType == int.class) {
      return (T) Integer.valueOf(0);
    }
    if (returnType == Double.class || returnType == double.class) {
      return (T) Double.valueOf(0.0);
    }
    return null;
  }
}
