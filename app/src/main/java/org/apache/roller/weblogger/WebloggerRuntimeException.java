package org.apache.roller.weblogger;

/**
 * An unchecked exception wrapper for Weblogger failures.
 * Used to avoid polluting interfaces with checked WebloggerException.
 */
public class WebloggerRuntimeException extends RuntimeException {

    public WebloggerRuntimeException(String message) {
        super(message);
    }

    public WebloggerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebloggerRuntimeException(Throwable cause) {
        super(cause);
    }
}
