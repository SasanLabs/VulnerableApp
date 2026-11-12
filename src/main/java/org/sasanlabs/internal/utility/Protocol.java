package org.sasanlabs.internal.utility;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Transport protocol a level's endpoint is reached over. Scanners and benchmark tooling need this
 * to tell a WebSocket handshake URL apart from a plain HTTP endpoint, since the two need different
 * attack tooling and a {@code GET} {@link org.springframework.web.bind.annotation.RequestMethod} on
 * a WebSocket level does not mean an ordinary HTTP GET works against it.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Protocol {
    HTTP,
    WS;

    /**
     * Binds JSON only from the exact constant name, so numeric ordinals such as 0 or "0" are
     * rejected. {@code @JsonFormat(shape = STRING)} alone does not change enum deserialization in
     * Jackson 2.13.
     */
    @JsonCreator
    public static Protocol fromName(String name) {
        return valueOf(name);
    }

    /**
     * Returns the given protocol, or {@link #HTTP} when it is null. Findings and ground-truth
     * payloads from before protocols were reported omit the field, and they describe HTTP.
     */
    public static Protocol orDefault(Protocol protocol) {
        return protocol != null ? protocol : HTTP;
    }

    /**
     * Returns the request method when it applies to this protocol, or null when it does not. Only
     * {@link #HTTP} uses a method; a WebSocket level is always opened with a GET handshake, so its
     * method says nothing about which frames were tested.
     *
     * @param method the request method, which may be null
     * @return {@code method} for {@link #HTTP}, otherwise null
     */
    public String getMethodIfApplicable(String method) {
        return this == HTTP ? method : null;
    }
}
