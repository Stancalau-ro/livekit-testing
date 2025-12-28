package ro.stancalau.test.framework.util;

import lombok.Getter;

@Getter
public class TestTimeoutException extends RuntimeException {

  private final String operation;
  private final String context;
  private final long elapsedMs;

  public TestTimeoutException(String operation, String context, long elapsedMs) {
    super(buildMessage(operation, context, elapsedMs));
    this.operation = operation;
    this.context = context;
    this.elapsedMs = elapsedMs;
  }

  public TestTimeoutException(String operation, String context, long elapsedMs, Throwable cause) {
    super(buildMessage(operation, context, elapsedMs), cause);
    this.operation = operation;
    this.context = context;
    this.elapsedMs = elapsedMs;
  }

  private static String buildMessage(String operation, String context, long elapsedMs) {
    StringBuilder sb = new StringBuilder();
    sb.append("TEST TIMEOUT (not container startup): ");
    sb.append(operation);
    if (context != null && !context.isEmpty()) {
      sb.append(" [").append(context).append("]");
    }
    sb.append(" after ").append(elapsedMs).append("ms");
    return sb.toString();
  }
}
