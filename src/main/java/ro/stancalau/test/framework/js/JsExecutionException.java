package ro.stancalau.test.framework.js;

public class JsExecutionException extends RuntimeException {

  public JsExecutionException(String message) {
    super(message);
  }

  public JsExecutionException(String message, Throwable cause) {
    super(message, cause);
  }

  public JsExecutionException(String functionName, String message, Throwable cause) {
    super(String.format("JavaScript execution failed for '%s': %s", functionName, message), cause);
  }
}
