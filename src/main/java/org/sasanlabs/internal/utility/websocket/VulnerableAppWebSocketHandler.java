package org.sasanlabs.internal.utility.websocket;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketMapping;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Bridges the WebSocket endpoint of a level to the method annotated with {@link
 * VulnerableAppWebSocketMapping}, so that a vulnerability only has to write that method.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public class VulnerableAppWebSocketHandler extends TextWebSocketHandler {

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
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
        if (reply != null && session.isOpen()) {
            session.sendMessage(new TextMessage(reply));
        }
    }
}
