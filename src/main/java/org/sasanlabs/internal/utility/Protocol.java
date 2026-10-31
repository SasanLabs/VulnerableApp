package org.sasanlabs.internal.utility;

/**
 * Transport protocol a level's endpoint is reached over. Scanners and benchmark tooling need this
 * to tell a WebSocket handshake URL apart from a plain HTTP endpoint, since the two need different
 * attack tooling and a {@code GET} {@link org.springframework.web.bind.annotation.RequestMethod} on
 * a WebSocket level does not mean an ordinary HTTP GET works against it.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public enum Protocol {
    HTTP,
    WS
}
