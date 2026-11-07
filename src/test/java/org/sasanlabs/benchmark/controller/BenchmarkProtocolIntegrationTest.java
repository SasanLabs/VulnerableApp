package org.sasanlabs.benchmark.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.sasanlabs.benchmark.BenchmarkConfiguration;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.sasanlabs.benchmark.service.BenchmarkResultWriter;
import org.sasanlabs.benchmark.service.BenchmarkService;
import org.sasanlabs.benchmark.service.DastBenchmarkStrategy;
import org.sasanlabs.benchmark.service.SastBenchmarkStrategy;
import org.sasanlabs.internal.utility.Protocol;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

/** Exercises request binding, HTTP ground-truth binding, real comparison, and JSON persistence. */
class BenchmarkProtocolIntegrationTest {

    private static final String GROUND_TRUTH_URL =
            "http://benchmark.invalid/VulnerableApp/scanner/dast";

    private static final String MIXED_GROUND_TRUTH =
            "[{\"url\":\"http://benchmark.invalid/VulnerableApp/shared\","
                    + "\"variant\":\"UNSECURE\",\"method\":\"GET\",\"protocol\":\"HTTP\","
                    + "\"vulnerabilityTypes\":[\"REFLECTED_XSS\"]},"
                    + "{\"url\":\"http://benchmark.invalid/VulnerableApp/shared\","
                    + "\"variant\":\"UNSECURE\",\"method\":\"GET\",\"protocol\":\"WS\","
                    + "\"vulnerabilityTypes\":[\"REFLECTED_XSS\"]}]";

    @TempDir Path outputDir;

    private ObjectMapper mapper;
    private RestTemplate restTemplate;
    private MockRestServiceServer groundTruthServer;
    private BenchmarkService benchmarkService;
    private BenchmarkResultWriter writer;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Use the application's Boot customizers, including its parameter-name module.
        new ApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                JacksonAutoConfiguration.class,
                                HttpMessageConvertersAutoConfiguration.class,
                                RestTemplateAutoConfiguration.class))
                .withUserConfiguration(BenchmarkConfiguration.class)
                .run(
                        context -> {
                            mapper = context.getBean(ObjectMapper.class);
                            restTemplate = context.getBean(RestTemplate.class);
                        });
        groundTruthServer = MockRestServiceServer.bindTo(restTemplate).build();
        DastBenchmarkStrategy dast =
                new DastBenchmarkStrategy(restTemplate, GROUND_TRUTH_URL, "/VulnerableApp");
        SastBenchmarkStrategy sast =
                new SastBenchmarkStrategy(
                        () ->
                                Collections.singletonList(
                                        new ExpectedIssue(
                                                "CWE-89",
                                                "SQL Injection",
                                                "src/Foo.java",
                                                10,
                                                1,
                                                3)));
        benchmarkService = spy(new BenchmarkService(dast, sast));
        writer = spy(new BenchmarkResultWriter(mapper, outputDir.toString()));
        mockMvc =
                MockMvcBuilders.standaloneSetup(new BenchmarkController(benchmarkService, writer))
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                        .build();
    }

    @Test
    void mixedJsonMatchesBothProtocolsAndPersistsExactlyTheReturnedReport() throws Exception {
        expectGroundTruth(MIXED_GROUND_TRUTH);
        String request =
                "{\"tool\":\"ProtocolTest\",\"supportedProtocols\":[\"HTTP\",\"WS\"],"
                        + "\"findings\":[{\"url\":\"/shared\",\"type\":\"REFLECTED_XSS\",\"method\":\"GET\"},"
                        + "{\"url\":\"wss://scanner.invalid/VulnerableApp/shared?attack=1\","
                        + "\"cwe\":\"CWE-79\",\"protocol\":\"WS\",\"method\":\"POST\"},"
                        + "{\"url\":\"/unrelated\",\"type\":\"REFLECTED_XSS\","
                        + "\"protocol\":\"WS\",\"method\":\"DELETE\"}]}";

        MvcResult response =
                mockMvc.perform(
                                post("/scanner/benchmark")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(request))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalExpected").value(2))
                        .andExpect(jsonPath("$.detected").value(2))
                        .andExpect(jsonPath("$.missed").value(0))
                        .andExpect(jsonPath("$.coverage").value(100.0))
                        .andExpect(jsonPath("$.unmatched").value(1))
                        .andExpect(jsonPath("$.detectedItems[0].method").value("GET"))
                        .andExpect(jsonPath("$.detectedItems[0].protocol").doesNotExist())
                        .andExpect(jsonPath("$.detectedItems[1].protocol").value("WS"))
                        .andExpect(jsonPath("$.detectedItems[1].method").doesNotExist())
                        .andExpect(jsonPath("$.unmatchedItems[0].protocol").value("WS"))
                        .andExpect(jsonPath("$.unmatchedItems[0].method").value("DELETE"))
                        .andReturn();

        assertPersistedEqualsResponse(response);
        groundTruthServer.verify();
    }

    @Test
    void legacyRequestKeepsHttpOnlyCoverageAndTheOriginalReportShape() throws Exception {
        expectGroundTruth(MIXED_GROUND_TRUTH);

        MvcResult response =
                mockMvc.perform(
                                post("/scanner/benchmark")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"tool\":\"ProtocolTest\",\"findings\":["
                                                        + "{\"url\":\"/shared\",\"type\":\"REFLECTED_XSS\"}]}"))
                        .andExpect(status().isOk())
                        .andReturn();

        String expected =
                "{\"tool\":\"ProtocolTest\",\"coverage\":100.0,\"totalExpected\":1,"
                        + "\"detected\":1,\"missed\":0,\"unmatched\":0,\"detectedItems\":["
                        + "{\"url\":\"/shared\",\"type\":\"REFLECTED_XSS\",\"cwe\":\"CWE-79\","
                        + "\"wascId\":\"8\",\"method\":\"GET\"}],\"missedItems\":[],\"unmatchedItems\":[]}";
        assertThat(mapper.readTree(response.getResponse().getContentAsString()))
                .isEqualTo(mapper.readTree(expected));
        assertPersistedEqualsResponse(response);
        groundTruthServer.verify();
    }

    @Test
    void groundTruthWithoutProtocolRemainsHttpCompatible() throws Exception {
        expectGroundTruth(
                "[{\"url\":\"http://benchmark.invalid/VulnerableApp/shared\","
                        + "\"variant\":\"UNSECURE\",\"method\":\"POST\","
                        + "\"vulnerabilityTypes\":[\"REFLECTED_XSS\"]}]");

        mockMvc.perform(
                        post("/scanner/benchmark")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"tool\":\"ProtocolTest\",\"findings\":["
                                                + "{\"url\":\"/shared\",\"wascId\":\"WASC-08\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detected").value(1))
                .andExpect(jsonPath("$.detectedItems[0].method").value("POST"))
                .andExpect(jsonPath("$.detectedItems[0].protocol").doesNotExist());

        groundTruthServer.verify();
    }

    @Test
    void dastProtocolMetadataDoesNotChangeActualSastComparisonOrPersistedOutput() throws Exception {
        String finding =
                "{\"filePath\":\"./src/Foo.java\",\"line\":11,\"cwe\":\"CWE-89\","
                        + "\"type\":\"SQL Injection\"}";
        String legacyRequest =
                "{\"tool\":\"ProtocolTest\",\"scanType\":\"SAST\",\"findings\":[" + finding + "]}";
        String metadataRequest =
                "{\"tool\":\"ProtocolTest\",\"scanType\":\"SAST\",\"supportedProtocols\":[\"WS\"],"
                        + "\"findings\":[{\"filePath\":\"./src/Foo.java\",\"line\":11,"
                        + "\"cwe\":\"CWE-89\",\"type\":\"SQL Injection\","
                        + "\"protocol\":\"WS\",\"method\":\"POST\"}]}";

        MvcResult legacyResponse =
                mockMvc.perform(
                                post("/scanner/benchmark")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(legacyRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.coverage").value(100.0))
                        .andExpect(jsonPath("$.totalExpected").value(1))
                        .andExpect(jsonPath("$.detectedItems[0].line").value(10))
                        .andReturn();
        MvcResult metadataResponse =
                mockMvc.perform(
                                post("/scanner/benchmark")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(metadataRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.detectedItems[0].protocol").doesNotExist())
                        .andExpect(jsonPath("$.detectedItems[0].method").doesNotExist())
                        .andReturn();

        assertThat(mapper.readTree(metadataResponse.getResponse().getContentAsString()))
                .isEqualTo(mapper.readTree(legacyResponse.getResponse().getContentAsString()));
        assertPersistedEqualsResponse(metadataResponse);
        // No DAST ground-truth request was registered or is allowed during these SAST requests.
        groundTruthServer.verify();
    }

    @ParameterizedTest
    @EnumSource(Protocol.class)
    void sastUnmatchedReportOmitsProtocolAndPreservesEveryLegacyField(Protocol protocol)
            throws Exception {
        String finding =
                "{\"url\":\"http://scanner.invalid/legacy?keep=1\",\"filePath\":\"./src/Other.java\","
                        + "\"line\":77,\"cwe\":\"CWE-89\",\"type\":\"SQL Injection\","
                        + "\"wascId\":\"19\",\"method\":\"POST\"}";
        String legacyRequest =
                "{\"tool\":\"ProtocolTest\",\"scanType\":\"SAST\",\"findings\":[" + finding + "]}";
        ObjectNode metadataRequest = (ObjectNode) mapper.readTree(legacyRequest);
        metadataRequest.putArray("supportedProtocols").add("WS");
        ((ObjectNode) metadataRequest.path("findings").get(0)).put("protocol", protocol.name());

        MvcResult legacyResponse =
                mockMvc.perform(
                                post("/scanner/benchmark")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(legacyRequest))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.coverage").value(0.0))
                        .andExpect(jsonPath("$.detected").value(0))
                        .andExpect(jsonPath("$.missed").value(1))
                        .andExpect(jsonPath("$.unmatched").value(1))
                        .andReturn();
        assertPersistedEqualsResponse(legacyResponse);
        assertThat(
                        mapper.readTree(legacyResponse.getResponse().getContentAsString())
                                .path("unmatchedItems")
                                .get(0))
                .isEqualTo(mapper.readTree(finding));

        MvcResult metadataResponse =
                mockMvc.perform(
                                post("/scanner/benchmark")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(mapper.writeValueAsString(metadataRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.unmatchedItems[0].protocol").doesNotExist())
                        .andExpect(
                                jsonPath("$.unmatchedItems[0].url")
                                        .value("http://scanner.invalid/legacy?keep=1"))
                        .andExpect(jsonPath("$.unmatchedItems[0].method").value("POST"))
                        .andExpect(jsonPath("$.unmatchedItems[0].wascId").value("19"))
                        .andReturn();
        assertThat(mapper.readTree(metadataResponse.getResponse().getContentAsString()))
                .isEqualTo(mapper.readTree(legacyResponse.getResponse().getContentAsString()));
        assertPersistedEqualsResponse(metadataResponse);

        ArgumentCaptor<ScannerFindings> inputs = ArgumentCaptor.forClass(ScannerFindings.class);
        verify(benchmarkService, times(2)).compare(inputs.capture());
        assertThat(inputs.getAllValues().get(1).getFindings().get(0).getProtocol())
                .isEqualTo(protocol);
        groundTruthServer.verify();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "[]",
                "[null]",
                "[\"HTTP\",null]",
                "[\"FTP\"]",
                "[0]",
                "[1]",
                "[\"0\"]",
                "[true]",
                "[{}]",
                "[[]]",
                "[\"ws\"]",
                "[\"\"]",
                "\"HTTP\""
            })
    void invalidCapabilityJsonReturns400BeforeComparisonOrPersistence(String capabilities)
            throws Exception {
        assertRejectedBeforeComparison(
                "{\"tool\":\"ProtocolTest\",\"findings\":[],\"supportedProtocols\":"
                        + capabilities
                        + "}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "\"0\"", "true", "{}", "[]", "\"FTP\"", "\"ws\"", "\"\""})
    void invalidFindingProtocolJsonReturns400BeforeComparisonOrPersistence(String protocol)
            throws Exception {
        assertRejectedBeforeComparison(
                "{\"tool\":\"ProtocolTest\",\"findings\":[{\"url\":\"/shared\","
                        + "\"type\":\"REFLECTED_XSS\",\"protocol\":"
                        + protocol
                        + "}]}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[null]", "[0]"})
    void duplicatePostCreatorProtocolPropertiesAreRejectedBeforeComparison(String invalidValue)
            throws Exception {
        assertRejectedBeforeComparison(
                "{\"tool\":\"ProtocolTest\",\"scanType\":\"DAST\",\"findings\":[],"
                        + "\"supportedProtocols\":[\"HTTP\"],\"supportedProtocols\":"
                        + invalidValue
                        + "}");
        assertRejectedBeforeComparison(
                "{\"tool\":\"ProtocolTest\",\"findings\":[{\"url\":\"/shared\","
                        + "\"type\":\"REFLECTED_XSS\",\"filePath\":null,\"line\":null,"
                        + "\"cwe\":null,\"wascId\":null,\"method\":\"GET\","
                        + "\"protocol\":\"HTTP\",\"protocol\":"
                        + invalidValue
                        + "}]}");
    }

    private void expectGroundTruth(String json) {
        groundTruthServer
                .expect(requestTo(GROUND_TRUTH_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    private void assertRejectedBeforeComparison(String json) throws Exception {
        mockMvc.perform(
                        post("/scanner/benchmark")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(benchmarkService, writer);
        try (Stream<Path> outputs = Files.list(outputDir)) {
            assertThat(outputs).isEmpty();
        }
        groundTruthServer.verify();
    }

    private void assertPersistedEqualsResponse(MvcResult response) throws Exception {
        JsonNode persisted =
                mapper.readTree(outputDir.resolve("protocoltest-results.json").toFile());
        assertThat(persisted)
                .isEqualTo(mapper.readTree(response.getResponse().getContentAsString()));
        try (Stream<Path> outputs = Files.list(outputDir)) {
            assertThat(outputs).containsExactly(outputDir.resolve("protocoltest-results.json"));
        }
    }
}
