package org.sasanlabs.internal.utility.websocket;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketMapping;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Bridges the WebSocket endpoint of a level to the method annotated with {@link
 * VulnerableAppWebSocketMapping}, so that a vulnerability only has to write that method.
 *
 * <p>A failure inside the level's own method is logged here with enough context to debug it
 * (controller, level, session), then re-thrown as a {@link WebSocketHandlingException} rather than
 * the original exception. Spring's own {@code ExceptionWebSocketHandlerDecorator} catches whatever
 * reaches it from here and closes the session with the fixed, detail-free {@code SERVER_ERROR}
 * close status, so wrapping the cause keeps the logged stack trace and the client-visible outcome
 * cleanly separated without needing a second catch point for that.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public class VulnerableAppWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LogManager.getLogger(VulnerableAppWebSocketHandler.class);

    private final Object controller;
    private final Method levelMethod;

    public VulnerableAppWebSocketHandler(Object controller, Method levelMethod) {
        this.controller = controller;
        this.levelMethod = levelMethod;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        String reply;
        try {
            reply = (String) levelMethod.invoke(controller, session, message.getPayload());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            VulnerableAppWebSocketMapping mapping =
                    levelMethod.getAnnotation(VulnerableAppWebSocketMapping.class);
            LOGGER.error(
                    "Level method {}.{} (level {}) failed for session {}",
                    controller.getClass().getSimpleName(),
                    levelMethod.getName(),
                    mapping != null ? mapping.value() : "unknown",
                    session.getId(),
                    cause);
            throw new WebSocketHandlingException("WebSocket level method invocation failed", cause);
        }
        if (reply != null && session.isOpen()) {
            session.sendMessage(new TextMessage(reply));
        }
    }
}
