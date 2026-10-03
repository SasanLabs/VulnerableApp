package org.sasanlabs.internal.utility.websocket;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.Cookie;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Keeps the cookies of the handshake request on the {@link WebSocketSession}, already parsed by the
 * servlet container, so a level can read them with {@link #getCookie(WebSocketSession, String)}
 * instead of parsing the {@code Cookie} header itself.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public class VulnerableAppWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    static final String COOKIES_ATTRIBUTE = "vulnerableApp.handshakeCookies";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            Cookie[] cookies =
                    ((ServletServerHttpRequest) request).getServletRequest().getCookies();
            if (cookies != null) {
                Map<String, String> cookieValues = new HashMap<>();
                for (Cookie cookie : cookies) {
                    cookieValues.put(cookie.getName(), cookie.getValue());
                }
                attributes.put(COOKIES_ATTRIBUTE, cookieValues);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {}

    /**
     * @param session WebSocket session opened through the handshake
     * @param name name of the cookie
     * @return value of the cookie sent with the handshake request, {@code null} if there was none
     */
    @SuppressWarnings("unchecked")
    public static String getCookie(WebSocketSession session, String name) {
        Object cookies = session.getAttributes().get(COOKIES_ATTRIBUTE);
        return cookies == null ? null : ((Map<String, String>) cookies).get(name);
    }
}
