package org.sasanlabs.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketMapping;
import org.sasanlabs.internal.utility.websocket.VulnerableAppWebSocketHandler;
import org.sasanlabs.internal.utility.websocket.VulnerableAppWebSocketHandshakeInterceptor;
import org.sasanlabs.service.vulnerability.websocket.WebSocketVulnerability;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

class VulnerableAppWebSocketConfigurationTest {

    public static class InvalidController {
        @VulnerableAppWebSocketMapping("LEVEL_1")
        public void wrongSignature(WebSocketSession session) {}
    }

    public static class WrongParameterCountController {
        @VulnerableAppWebSocketMapping("LEVEL_1")
        public String wrongSignature(WebSocketSession session) {
            return null;
        }
    }

    public static class NotPublicController {
        @VulnerableAppWebSocketMapping("LEVEL_1")
        String wrongSignature(WebSocketSession session, String message) {
            return null;
        }
    }

    public static class WrongReturnTypeController {
        @VulnerableAppWebSocketMapping("LEVEL_1")
        public Object wrongSignature(WebSocketSession session, String message) {
            return null;
        }
    }

    public static class WrongSessionTypeController {
        @VulnerableAppWebSocketMapping("LEVEL_1")
        public String wrongSignature(Object session, String message) {
            return null;
        }
    }

    public static class WrongMessageTypeController {
        @VulnerableAppWebSocketMapping("LEVEL_1")
        public String wrongSignature(WebSocketSession session, Object message) {
            return null;
        }
    }

    private final EnvUtils envUtils = mock(EnvUtils.class);
    private final WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
    private final WebSocketHandlerRegistration registration =
            mock(WebSocketHandlerRegistration.class, RETURNS_SELF);

    private void controllers(Object controller) {
        when(envUtils.getAllClassesAnnotatedWithVulnerableAppWebSocketController())
                .thenReturn(Map.of("WebSocketVulnerability", controller));
    }

    @Test
    void registerWebSocketHandlers_derivesThePathOfEveryLevelFromTheAnnotations() {
        controllers(new WebSocketVulnerability());
        when(registry.addHandler(any(), anyString())).thenReturn(registration);

        new VulnerableAppWebSocketConfiguration(envUtils).registerWebSocketHandlers(registry);

        for (String level : new String[] {"LEVEL_1", "LEVEL_2", "LEVEL_3"}) {
            verify(registry)
                    .addHandler(
                            any(VulnerableAppWebSocketHandler.class),
                            eq("/WebSocketVulnerability/" + level));
        }
    }

    @Test
    void registerWebSocketHandlers_appliesTheAllowedOriginsOfEachLevel() {
        controllers(new WebSocketVulnerability());
        when(registry.addHandler(any(), anyString())).thenReturn(registration);

        new VulnerableAppWebSocketConfiguration(envUtils).registerWebSocketHandlers(registry);

        verify(registration, times(2)).setAllowedOrigins(new String[] {});
        verify(registration).setAllowedOrigins("*");
    }

    @Test
    void registerWebSocketHandlers_registersNothingWithoutControllers() {
        when(envUtils.getAllClassesAnnotatedWithVulnerableAppWebSocketController())
                .thenReturn(Collections.emptyMap());

        new VulnerableAppWebSocketConfiguration(envUtils).registerWebSocketHandlers(registry);

        verify(registry, never()).addHandler(any(), anyString());
    }

    @Test
    void registerWebSocketHandlers_rejectsAMethodWithTheWrongSignature() {
        controllers(new InvalidController());

        assertThatThrownBy(
                        () ->
                                new VulnerableAppWebSocketConfiguration(envUtils)
                                        .registerWebSocketHandlers(registry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("wrongSignature");
    }

    @Test
    void registerWebSocketHandlers_rejectsEveryKindOfWrongSignature() {
        for (Object controller :
                new Object[] {
                    new NotPublicController(),
                    new WrongParameterCountController(),
                    new WrongReturnTypeController(),
                    new WrongSessionTypeController(),
                    new WrongMessageTypeController()
                }) {
            controllers(controller);

            assertThatThrownBy(
                            () ->
                                    new VulnerableAppWebSocketConfiguration(envUtils)
                                            .registerWebSocketHandlers(registry))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("wrongSignature");
        }
    }

    @Test
    void registerWebSocketHandlers_addsTheHandshakeInterceptorToEveryLevel() {
        controllers(new WebSocketVulnerability());
        when(registry.addHandler(any(), anyString())).thenReturn(registration);

        new VulnerableAppWebSocketConfiguration(envUtils).registerWebSocketHandlers(registry);

        verify(registration, times(3))
                .addInterceptors(any(VulnerableAppWebSocketHandshakeInterceptor.class));
    }
}
