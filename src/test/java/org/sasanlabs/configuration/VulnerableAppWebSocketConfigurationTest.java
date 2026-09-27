package org.sasanlabs.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketMapping;
import org.sasanlabs.internal.utility.websocket.VulnerableAppWebSocketHandler;
import org.sasanlabs.service.vulnerability.websocket.WebSocketVulnerability;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

class VulnerableAppWebSocketConfigurationTest {

    public static class InvalidController {
        @VulnerableAppWebSocketMapping("LEVEL_1")
        public void wrongSignature(WebSocketSession session) {}
    }

    private final EnvUtils envUtils = mock(EnvUtils.class);
    private final WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
    private final WebSocketHandlerRegistration registration =
            mock(WebSocketHandlerRegistration.class);

    private void controllers(Object controller) {
        when(envUtils.getAllClassesAnnotatedWithVulnerableAppWebSocketController())
                .thenReturn(Map.of("WebSocketVulnerability", controller));
    }

    @Test
    void registerWebSocketHandlers_derivesThePathFromTheAnnotations() {
        controllers(new WebSocketVulnerability());
        when(registry.addHandler(any(), anyString())).thenReturn(registration);

        new VulnerableAppWebSocketConfiguration(envUtils).registerWebSocketHandlers(registry);

        verify(registry)
                .addHandler(
                        any(VulnerableAppWebSocketHandler.class),
                        eq("/WebSocketVulnerability/LEVEL_1"));
    }

    @Test
    void registerWebSocketHandlers_appliesTheAllowedOriginsOfTheLevel() {
        controllers(new WebSocketVulnerability());
        when(registry.addHandler(any(), anyString())).thenReturn(registration);

        new VulnerableAppWebSocketConfiguration(envUtils).registerWebSocketHandlers(registry);

        verify(registration).setAllowedOrigins(new String[] {});
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
}
