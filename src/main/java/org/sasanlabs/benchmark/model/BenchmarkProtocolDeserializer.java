package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.sasanlabs.internal.utility.Protocol;

/**
 * Strict protocol values for benchmark requests, without changing other protocol consumers.
 *
 * <p>Only the exact strings {@code HTTP} and {@code WS} are accepted. Jackson's normal null
 * handling leaves null/default and null-list-member policy to the containing benchmark model.
 */
public class BenchmarkProtocolDeserializer extends StdDeserializer<Protocol> {

    public BenchmarkProtocolDeserializer() {
        super(Protocol.class);
    }

    /**
     * Reads an exact protocol name instead of accepting Jackson's enum ordinal coercions.
     *
     * <p>Non-string tokens use Jackson's unexpected-token error path. Unknown, case-folded,
     * whitespace-padded or numeric strings use its invalid-string error path; values are not
     * trimmed or otherwise normalised here.
     *
     * @throws IOException if the input is invalid or cannot be read by the parser
     */
    @Override
    public Protocol deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return (Protocol) context.handleUnexpectedToken(Protocol.class, parser);
        }
        String value = parser.getText();
        try {
            return Protocol.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw context.weirdStringException(value, Protocol.class, "Expected HTTP or WS");
        }
    }
}
