package org.sasanlabs.internal.utility.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketSession;

class VulnerableAppWebSocketHandshakeInterceptorTest {

    private final VulnerableAppWebSocketHandshakeInterceptor interceptor =
            new VulnerableAppWebSocketHandshakeInterceptor();

    private WebSocketSession sessionWith(Map<String, Object> attributes) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(attributes);
        return session;
    }

    @Test
    void beforeHandshake_keepsTheCookiesParsedByTheContainer() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setCookies(new Cookie("theme", "dark"), new Cookie("session", "abc=="));
        Map<String, Object> attributes = new HashMap<>();

        boolean proceed =
                interceptor.beforeHandshake(
                        new ServletServerHttpRequest(servletRequest), null, null, attributes);

        assertThat(proceed).isTrue();
        WebSocketSession session = sessionWith(attributes);
        assertThat(VulnerableAppWebSocketHandshakeInterceptor.getCookie(session, "session"))
                .isEqualTo("abc==");
        assertThat(VulnerableAppWebSocketHandshakeInterceptor.getCookie(session, "theme"))
                .isEqualTo("dark");
        assertThat(VulnerableAppWebSocketHandshakeInterceptor.getCookie(session, "missing"))
                .isNull();
    }

    @Test
    void beforeHandshake_withoutCookiesStoresNothingAndStillProceeds() {
        Map<String, Object> attributes = new HashMap<>();

        boolean proceed =
                interceptor.beforeHandshake(
                        new ServletServerHttpRequest(new MockHttpServletRequest()),
                        null,
                        null,
                        attributes);

        assertThat(proceed).isTrue();
        assertThat(attributes).isEmpty();
        assertThat(
                        VulnerableAppWebSocketHandshakeInterceptor.getCookie(
                                sessionWith(attributes), "session"))
                .isNull();
    }

    @Test
    void beforeHandshake_ignoresARequestThatIsNotAServletRequest() {
        Map<String, Object> attributes = new HashMap<>();

        boolean proceed =
                interceptor.beforeHandshake(mock(ServerHttpRequest.class), null, null, attributes);

        assertThat(proceed).isTrue();
        assertThat(attributes).isEmpty();
    }

    @Test
    void afterHandshake_doesNothing() {
        interceptor.afterHandshake(mock(ServerHttpRequest.class), null, null, null);
    }
}
