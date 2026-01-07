package ro.stancalau.test.framework.util;

public class WebDriverSessionDeadException extends RuntimeException {

    public WebDriverSessionDeadException(String message) {
        super(message);
    }

    public WebDriverSessionDeadException(String message, Throwable cause) {
        super(message, cause);
    }

    public static boolean isSessionDeadError(Throwable e) {
        if (e == null) {
            return false;
        }

        String className = e.getClass().getName().toLowerCase();
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        if (className.contains("nosuchsessionexception")) {
            return true;
        }

        if (className.contains("sessionnotcreatedexception")) {
            return true;
        }

        if (message.contains("session id is null")
                || message.contains("session not found")
                || message.contains("invalid session id")
                || message.contains("session deleted")
                || message.contains("no such session")
                || message.contains("session timed out")) {
            return true;
        }

        return isSessionDeadError(e.getCause());
    }
}
