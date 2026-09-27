package org.sasanlabs.configuration;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.FrameworkConstants;
import org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketMapping;
import org.sasanlabs.internal.utility.websocket.VulnerableAppWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the WebSocket endpoint of every level found on the beans annotated with {@link
 * org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketController}, so that a WebSocket
 * vulnerability is defined with annotations the same way a REST one is.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@Configuration
@EnableWebSocket
public class VulnerableAppWebSocketConfiguration implements WebSocketConfigurer {

    private final EnvUtils envUtils;

    public VulnerableAppWebSocketConfiguration(EnvUtils envUtils) {
        this.envUtils = envUtils;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        Map<String, Object> nameVsController =
                envUtils.getAllClassesAnnotatedWithVulnerableAppWebSocketController();
        for (Map.Entry<String, Object> entry : nameVsController.entrySet()) {
            Object controller = entry.getValue();
            for (Method method : controller.getClass().getDeclaredMethods()) {
                VulnerableAppWebSocketMapping mapping =
                        method.getAnnotation(VulnerableAppWebSocketMapping.class);
                if (mapping == null) {
                    continue;
                }
                validateSignature(method);
                registry.addHandler(
                                new VulnerableAppWebSocketHandler(controller, method),
                                FrameworkConstants.SLASH
                                        + entry.getKey()
                                        + FrameworkConstants.SLASH
                                        + mapping.value())
                        .setAllowedOrigins(mapping.allowedOrigins());
            }
        }
    }

    private void validateSignature(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        boolean validSignature =
                Modifier.isPublic(method.getModifiers())
                        && method.getReturnType() == String.class
                        && parameterTypes.length == 2
                        && parameterTypes[0] == WebSocketSession.class
                        && parameterTypes[1] == String.class;
        if (!validSignature) {
            throw new IllegalStateException(
                    method
                            + " must be public, take (WebSocketSession, String) and return String"
                            + " to be used with @VulnerableAppWebSocketMapping");
        }
    }
}
