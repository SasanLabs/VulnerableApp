package org.sasanlabs.internal.utility.websocket;

/**
 * Wraps a failure from invoking a {@link
 * org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketMapping}-annotated method, so
 * the original exception (and whatever detail it carries) never reaches the client. {@link
 * VulnerableAppWebSocketHandler} logs the real cause with its context before throwing this in its
 * place.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public class WebSocketHandlingException extends RuntimeException {

    public WebSocketHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
