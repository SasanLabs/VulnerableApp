package org.sasanlabs.internal.utility.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sasanlabs.internal.utility.Variant;

/**
 * Registers a method of a {@link VulnerableAppWebSocketController} as the WebSocket endpoint of one
 * level, the WebSocket counterpart of {@link VulnerableAppRequestMapping}. The path is derived as
 * {@code /{controller name}/{level}}.
 *
 * <p>The annotated method has to be {@code public}, take a {@link
 * org.springframework.web.socket.WebSocketSession} and the received text message as {@link String},
 * and return the text to send back to the client or {@code null} to send nothing.
 *
 * <p>The cookies of the handshake request can be read with {@link
 * org.sasanlabs.internal.utility.websocket.VulnerableAppWebSocketHandshakeInterceptor#getCookie}.
 *
 * <p>A REST mapping on the exact same path answers the handshake of the WebSocket with a 405, so
 * any REST endpoint a level needs has to use a sub path such as {@code /{controller
 * name}/{level}/session}.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface VulnerableAppWebSocketMapping {

    /**
     * Specify the level of the vulnerability. A WebSocket end point is exposed for each level.
     *
     * @return level
     */
    String value();

    /**
     * Specify whether the implementation can be considered secure, as in, non-exploitable.
     *
     * @return variant
     */
    Variant variant() default Variant.UNSECURE;

    /**
     * Template name is used to construct the url for static resources like js/css/html, same as
     * {@link VulnerableAppRequestMapping#htmlTemplate()}.
     *
     * @return template name
     */
    String htmlTemplate() default "";

    /**
     * Origins allowed to open the WebSocket. When empty only the same origin is allowed, {@code
     * "*"} allows any origin.
     *
     * @return allowed origins
     */
    String[] allowedOrigins() default {};
}
