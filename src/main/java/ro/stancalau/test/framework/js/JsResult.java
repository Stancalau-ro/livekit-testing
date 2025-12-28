package ro.stancalau.test.framework.js;

import lombok.Getter;

@Getter
public class JsResult<T> {

  private final T value;
  private final Exception error;
  private final boolean success;

  private JsResult(T value, Exception error, boolean success) {
    this.value = value;
    this.error = error;
    this.success = success;
  }

  public static <T> JsResult<T> success(T value) {
    return new JsResult<>(value, null, true);
  }

  public static <T> JsResult<T> failure(Exception error) {
    return new JsResult<>(null, error, false);
  }

  public T orElse(T defaultValue) {
    return success ? value : defaultValue;
  }

  public T orElseThrow() {
    if (!success) {
      throw new JsExecutionException("JavaScript execution failed", error);
    }
    return value;
  }

  public T orElseThrow(String functionName) {
    if (!success) {
      throw new JsExecutionException(
          functionName, error != null ? error.getMessage() : "Unknown error", error);
    }
    return value;
  }

  public boolean asBoolean() {
    if (!success || value == null) {
      return false;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return false;
  }

  public long asLong() {
    if (!success || value == null) {
      return 0L;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return 0L;
  }

  public int asInt() {
    if (!success || value == null) {
      return 0;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return 0;
  }

  public double asDouble() {
    if (!success || value == null) {
      return 0.0;
    }
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return 0.0;
  }

  public String asString() {
    if (!success || value == null) {
      return null;
    }
    return value.toString();
  }
}
