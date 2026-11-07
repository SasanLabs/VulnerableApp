package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.sasanlabs.internal.utility.Protocol;

/** Strict protocol values for benchmark requests, without changing other protocol consumers. */
public class BenchmarkProtocolDeserializer extends StdDeserializer<Protocol> {

    public BenchmarkProtocolDeserializer() {
        super(Protocol.class);
    }

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
