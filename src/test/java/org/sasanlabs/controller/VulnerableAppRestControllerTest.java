package org.sasanlabs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.sasanlabs.benchmark.service.IExpectedIssuesProvider;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Covers the bare {@code /scanner} endpoint, its {@code /scanner/dast} replacement (same body, only
 * the deprecation headers differ) and the {@code /scanner/sast} ground-truth endpoint.
 *
 * <p>Driven through {@link MockMvc} rather than by calling the controller methods directly, so that
 * the request mappings and the serialised field names are covered too. Those are the contract the
 * other applications copy, and a direct method call can see neither of them.
 */
@ExtendWith(MockitoExtension.class)
class VulnerableAppRestControllerTest {

    @Mock private IEndPointsInformationProvider endPointsInformationProvider;

    @Mock private IExpectedIssuesProvider expectedIssuesProvider;

    private VulnerableAppRestController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller =
                new VulnerableAppRestController(
                        endPointsInformationProvider, expectedIssuesProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private void givenDastGroundTruth() throws Exception {
        when(endPointsInformationProvider.getScannerRelatedEndPointInformation(anyString()))
                .thenReturn(
                        Collections.singletonList(
                                new ScannerResponseBean(
                                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                                        "LEVEL_1",
                                        RequestMethod.GET,
                                        Arrays.asList(
                                                VulnerabilityType.ERROR_BASED_SQL_INJECTION))));
    }

    @Test
    void bareScanner_returnsGroundTruth_andIsMarkedDeprecated() throws Exception {
        givenDastGroundTruth();

        mockMvc.perform(
                        get(URI.create("http://localhost:9090/VulnerableApp/scanner"))
                                .contextPath("/VulnerableApp"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(
                        header().string(
                                        "Link",
                                        "<http://localhost:9090/VulnerableApp/scanner/dast>;"
                                                + " rel=\"successor-version\""))
                .andExpect(jsonPath("$[0].variant").value("LEVEL_1"));
    }

    @Test
    void deprecationLink_pointsAtTheHostThatWasActuallyCalled() throws Exception {
        givenDastGroundTruth();

        mockMvc.perform(
                        get(URI.create("https://vulnerableapp.example:8443/VulnerableApp/scanner"))
                                .contextPath("/VulnerableApp"))
                .andExpect(
                        header().string(
                                        "Link",
                                        "<https://vulnerableapp.example:8443/VulnerableApp/scanner"
                                                + "/dast>; rel=\"successor-version\""));
    }

    @Test
    void dastEndpoint_isMapped_andCarriesNoDeprecationMarker() throws Exception {
        givenDastGroundTruth();

        mockMvc.perform(get(URI.create("http://localhost:9090/scanner/dast")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Link"))
                .andExpect(jsonPath("$[0].variant").value("LEVEL_1"));
    }

    @Test
    void dastEndpoint_servesTheSameBytesAsBareScanner() throws Exception {
        givenDastGroundTruth();

        String bare =
                mockMvc.perform(get(URI.create("http://localhost:9090/scanner")))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String dast =
                mockMvc.perform(get(URI.create("http://localhost:9090/scanner/dast")))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertEquals(bare, dast);
    }

    @Test
    void bothEndpoints_buildTheApplicationUrlFromTheIncomingRequest() throws Exception {
        givenDastGroundTruth();

        mockMvc.perform(
                get(URI.create("https://10.0.0.5:443/VulnerableApp/scanner"))
                        .contextPath("/VulnerableApp"));
        mockMvc.perform(
                get(URI.create("https://10.0.0.5:443/VulnerableApp/scanner/dast"))
                        .contextPath("/VulnerableApp"));

        ArgumentCaptor<String> appUrl = ArgumentCaptor.forClass(String.class);
        verify(endPointsInformationProvider, times(2))
                .getScannerRelatedEndPointInformation(appUrl.capture());
        for (String url : appUrl.getAllValues()) {
            assertEquals("https://10.0.0.5:443/VulnerableApp/", url);
        }
    }

    @Test
    void sastEndpoint_isMapped_andReturnsEveryRowTheProviderHolds() throws Exception {
        when(expectedIssuesProvider.getExpectedIssues()).thenReturn(twoExpectedIssues());

        mockMvc.perform(get("/scanner/sast"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    /**
     * The field names are the cross-application contract settled on issue #725: {@code type} rather
     * than the Java property name {@code vulnerabilityType}, and {@code numberOfSources} included.
     * VulnerableApp-php and VulnerableApp-jsp follow whatever shape lands here, so a rename has to
     * break a test.
     */
    @Test
    void sastEndpoint_serialisesTheAgreedFieldNames() throws Exception {
        when(expectedIssuesProvider.getExpectedIssues()).thenReturn(twoExpectedIssues());

        mockMvc.perform(get("/scanner/sast"))
                .andExpect(jsonPath("$[0].cwe").value("CWE-89"))
                .andExpect(jsonPath("$[0].type").value("SQL Injection"))
                .andExpect(jsonPath("$[0].filePath").value("src/main/java/Foo.java"))
                .andExpect(jsonPath("$[0].line").value(40))
                .andExpect(jsonPath("$[0].numberOfSources").value(1))
                .andExpect(jsonPath("$[0].vulnerabilityType").doesNotExist());
    }

    @Test
    void sastEndpoint_handsBackTheProvidersListUnchanged() throws Exception {
        List<ExpectedIssue> groundTruth = twoExpectedIssues();
        when(expectedIssuesProvider.getExpectedIssues()).thenReturn(groundTruth);

        assertSame(groundTruth, controller.getSastScannerRelatedInformation());
    }

    /**
     * Called directly rather than through {@link MockMvc}: the point is that the controller lets a
     * read failure out instead of turning it into an empty list, and MockMvc would wrap it in a
     * servlet exception before it could be asserted on.
     */
    @Test
    void sastEndpoint_propagatesAGroundTruthReadFailure() throws Exception {
        when(expectedIssuesProvider.getExpectedIssues()).thenThrow(new IOException("boom"));

        assertThrows(IOException.class, () -> controller.getSastScannerRelatedInformation());
    }

    private static List<ExpectedIssue> twoExpectedIssues() {
        return Arrays.asList(
                new ExpectedIssue("CWE-89", "SQL Injection", "src/main/java/Foo.java", 40, 1),
                new ExpectedIssue("CWE-79", "Reflected XSS", "src/main/java/Bar.java", 42, 3));
    }
}
