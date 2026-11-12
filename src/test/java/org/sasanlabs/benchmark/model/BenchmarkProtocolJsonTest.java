package org.sasanlabs.benchmark.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sasanlabs.internal.utility.Protocol;

class BenchmarkProtocolJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void legacyJsonDefaultsToHttpWithoutAddingProtocolToFindings() throws Exception {
        String json =
                "{\"tool\":\"Legacy\",\"findings\":[{\"url\":\"/XSS/LEVEL_1\","
                        + "\"type\":\"REFLECTED_XSS\",\"method\":\"GET\"}]}";

        ScannerFindings input = mapper.readValue(json, ScannerFindings.class);

        assertThat(input.getScanType()).isEqualTo(ScanType.DAST);
        assertThat(input.getSupportedProtocols()).containsExactly(Protocol.HTTP);
        assertThat(input.getFindings().get(0).getProtocol()).isNull();
        JsonNode serializedFinding = mapper.valueToTree(input.getFindings().get(0));
        assertThat(serializedFinding).isEqualTo(mapper.readTree(json).get("findings").get(0));
    }

    @Test
    void explicitNullUsesTheSameDefaultsAsMissingFields() throws Exception {
        ScannerFindings input =
                mapper.readValue(
                        "{\"tool\":\"Legacy\",\"scanType\":null,\"supportedProtocols\":null,"
                                + "\"findings\":[{\"url\":\"/XSS/LEVEL_1\",\"protocol\":null}]}",
                        ScannerFindings.class);

        assertThat(input.getScanType()).isEqualTo(ScanType.DAST);
        assertThat(input.getSupportedProtocols()).containsExactly(Protocol.HTTP);
        assertThat(input.getFindings().get(0).getProtocol()).isNull();
        assertThat(mapper.valueToTree(input.getFindings().get(0)).has("protocol")).isFalse();
    }

    @Test
    void mixedProtocolJsonRoundTripsWithoutLosingFindingProtocolOrMethod() throws Exception {
        String json =
                "{\"tool\":\"Mixed\",\"scanType\":\"DAST\","
                        + "\"supportedProtocols\":[\"HTTP\",\"WS\"],\"findings\":["
                        + "{\"url\":\"/shared\",\"type\":\"REFLECTED_XSS\","
                        + "\"method\":\"POST\",\"protocol\":\"WS\"},"
                        + "{\"url\":\"/shared\",\"type\":\"REFLECTED_XSS\","
                        + "\"method\":\"GET\",\"protocol\":\"HTTP\"}]}";

        ScannerFindings input = mapper.readValue(json, ScannerFindings.class);

        assertThat(input.getSupportedProtocols()).containsExactly(Protocol.HTTP, Protocol.WS);
        assertThat(input.getFindings())
                .extracting(Finding::getProtocol)
                .containsExactly(Protocol.WS, Protocol.HTTP);
        assertThat(mapper.readTree(mapper.writeValueAsString(input)))
                .isEqualTo(mapper.readTree(json));
    }

    @Test
    void duplicateCapabilitiesCollapseInStableOrderAndCannotBeMutated() throws Exception {
        ScannerFindings input =
                mapper.readValue(
                        "{\"tool\":\"Mixed\",\"findings\":[],"
                                + "\"supportedProtocols\":[\"WS\",\"HTTP\",\"WS\",\"HTTP\"]}",
                        ScannerFindings.class);

        assertThat(input.getSupportedProtocols()).containsExactly(Protocol.WS, Protocol.HTTP);
        assertThatThrownBy(() -> input.getSupportedProtocols().add(Protocol.HTTP))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructorCopiesCapabilitiesRatherThanFollowingCallerMutations() {
        List<Protocol> capabilities = new ArrayList<>(Arrays.asList(Protocol.HTTP, Protocol.WS));
        ScannerFindings input =
                new ScannerFindings("Mixed", ScanType.DAST, Collections.emptyList(), capabilities);

        capabilities.clear();

        assertThat(input.getSupportedProtocols()).containsExactly(Protocol.HTTP, Protocol.WS);
    }

    @Test
    void legacyJavaConstructorsRemainCompatible() {
        Finding finding =
                new Finding("/XSS/LEVEL_1", "REFLECTED_XSS", null, null, "79", "8", "GET");
        ScannerFindings oldDast = new ScannerFindings("Legacy", Collections.singletonList(finding));
        ScannerFindings oldSast =
                new ScannerFindings("Legacy", ScanType.SAST, Collections.singletonList(finding));

        assertThat(finding.getProtocol()).isNull();
        assertThat(finding.getMethod()).isEqualTo("GET");
        assertThat(oldDast.getScanType()).isEqualTo(ScanType.DAST);
        assertThat(oldSast.getScanType()).isEqualTo(ScanType.SAST);
        assertThat(oldDast.getSupportedProtocols()).containsExactly(Protocol.HTTP);
        assertThat(oldSast.getSupportedProtocols()).containsExactly(Protocol.HTTP);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "[\"HTTP\",null]"})
    void emptyOrNullMemberCapabilityDeclarationsAreRejected(String declaration) {
        String json =
                "{\"tool\":\"Invalid\",\"findings\":[],\"supportedProtocols\":" + declaration + "}";

        assertThatThrownBy(() -> mapper.readValue(json, ScannerFindings.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void emptyOrNullMemberCapabilitiesAreAlsoRejectedByJavaConstructor() {
        assertThatThrownBy(
                        () ->
                                new ScannerFindings(
                                        "Invalid",
                                        ScanType.DAST,
                                        Collections.emptyList(),
                                        Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                new ScannerFindings(
                                        "Invalid",
                                        ScanType.DAST,
                                        Collections.emptyList(),
                                        Arrays.asList(Protocol.HTTP, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "0",
                "1",
                "\"0\"",
                "\"1\"",
                "true",
                "{}",
                "[]",
                "\"FTP\"",
                "\"\"",
                "\"ws\"",
                "\" WS \""
            })
    void malformedFindingProtocolsAreRejected(String protocol) {
        String json =
                "{\"tool\":\"Invalid\",\"findings\":[{\"url\":\"/shared\",\"protocol\":"
                        + protocol
                        + "}]}";

        assertThatThrownBy(() -> mapper.readValue(json, ScannerFindings.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "0",
                "1",
                "\"0\"",
                "\"1\"",
                "true",
                "{}",
                "[]",
                "\"FTP\"",
                "\"\"",
                "\"ws\"",
                "\" WS \""
            })
    void malformedCapabilityProtocolsAreRejected(String protocol) {
        String json =
                "{\"tool\":\"Invalid\",\"findings\":[],\"supportedProtocols\":[" + protocol + "]}";

        assertThatThrownBy(() -> mapper.readValue(json, ScannerFindings.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "[\"HTTP\",null]", "[0]"})
    void duplicateCapabilitiesCannotBypassValidationAfterCreatorConstruction(String declaration) {
        // Supplying every creator property makes Jackson construct the object before it reads
        // the duplicate property, exercising its fallback setter rather than only the creator.
        String json =
                "{\"tool\":\"Invalid\",\"scanType\":\"DAST\",\"findings\":[],"
                        + "\"supportedProtocols\":[\"HTTP\"],\"supportedProtocols\":"
                        + declaration
                        + "}";

        assertThatThrownBy(() -> mapper.readValue(json, ScannerFindings.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void duplicateNullCapabilityUsesTheSameHttpDefaultAsCreatorBinding() throws Exception {
        ScannerFindings input =
                mapper.readValue(
                        "{\"tool\":\"Legacy\",\"scanType\":\"DAST\",\"findings\":[],"
                                + "\"supportedProtocols\":[\"WS\"],\"supportedProtocols\":null}",
                        ScannerFindings.class);

        assertThat(input.getSupportedProtocols()).containsExactly(Protocol.HTTP);
        assertThatThrownBy(() -> input.getSupportedProtocols().add(Protocol.WS))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void protocolBindsOnlyFromItsNameAndSerialisesAsAString() throws Exception {
        assertThat(mapper.readValue("\"WS\"", Protocol.class)).isEqualTo(Protocol.WS);
        assertThat(mapper.writeValueAsString(Protocol.HTTP)).isEqualTo("\"HTTP\"");
        assertThatThrownBy(() -> mapper.readValue("0", Protocol.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void sastFindingSerialisationOmitsProtocol() throws Exception {
        JsonNode finding =
                mapper.valueToTree(new Finding(null, "SQL Injection", "src/Foo.java", 5, "CWE-89"));
        assertThat(finding.has("protocol")).isFalse();
        assertThat(finding.has("url")).isFalse();
        assertThat(finding.path("filePath").asText()).isEqualTo("src/Foo.java");
    }
}
