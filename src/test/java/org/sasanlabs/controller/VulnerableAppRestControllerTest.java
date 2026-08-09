package org.sasanlabs.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.benchmark.service.IExpectedIssuesProvider;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Covers the bare {@code /scanner} endpoint and its {@code /scanner/dast} replacement. The two must
 * return the same body; only the deprecation headers differ.
 */
@ExtendWith(MockitoExtension.class)
class VulnerableAppRestControllerTest {

    @Mock private IEndPointsInformationProvider endPointsInformationProvider;

    @Mock private IExpectedIssuesProvider expectedIssuesProvider;

    @Mock private HttpServletRequest request;

    private VulnerableAppRestController controller;

    private List<ScannerResponseBean> groundTruth;

    @BeforeEach
    void setUp() {
        controller =
                new VulnerableAppRestController(
                        endPointsInformationProvider, expectedIssuesProvider);
        // A fresh instance rather than Collections.emptyList(), which is a shared singleton and
        // would make the identity assertions below pass even if the wrong list were returned.
        groundTruth = new ArrayList<>();
    }

    private void givenRequestFor(String scheme, String host, int port) {
        when(request.getScheme()).thenReturn(scheme);
        when(request.getServerName()).thenReturn(host);
        when(request.getServerPort()).thenReturn(port);
    }

    @Test
    void bareScannerReturnsGroundTruthAndIsMarkedDeprecated() throws Exception {
        givenRequestFor("http", "localhost", 9090);
        when(endPointsInformationProvider.getScannerRelatedEndPointInformation(anyString()))
                .thenReturn(groundTruth);

        ResponseEntity<List<ScannerResponseBean>> response =
                controller.getScannerRelatedInformation(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(groundTruth, response.getBody());
        assertEquals("true", response.getHeaders().getFirst("Deprecation"));
    }

    @Test
    void deprecationLinkPointsAtTheDastEndpointOnTheHostThatWasCalled() throws Exception {
        givenRequestFor("https", "vulnerableapp.example", 8443);
        when(endPointsInformationProvider.getScannerRelatedEndPointInformation(anyString()))
                .thenReturn(groundTruth);

        ResponseEntity<List<ScannerResponseBean>> response =
                controller.getScannerRelatedInformation(request);

        assertEquals(
                "<https://vulnerableapp.example:8443/VulnerableApp/scanner/dast>;"
                        + " rel=\"successor-version\"",
                response.getHeaders().getFirst("Link"));
    }

    @Test
    void dastEndpointReturnsTheSameBodyAsBareScanner() throws Exception {
        givenRequestFor("http", "localhost", 9090);
        when(endPointsInformationProvider.getScannerRelatedEndPointInformation(anyString()))
                .thenReturn(groundTruth);

        List<ScannerResponseBean> dast = controller.getDastScannerRelatedInformation(request);
        ResponseEntity<List<ScannerResponseBean>> bare =
                controller.getScannerRelatedInformation(request);

        assertSame(groundTruth, dast);
        assertSame(bare.getBody(), dast);
    }

    @Test
    void dastEndpointCarriesNoDeprecationMarker() throws Exception {
        givenRequestFor("http", "localhost", 9090);
        when(endPointsInformationProvider.getScannerRelatedEndPointInformation(anyString()))
                .thenReturn(groundTruth);

        // The method returns the body directly, so there is no place for a header to be attached.
        // Asserting the return type keeps that intent from being changed silently.
        Object returned = controller.getDastScannerRelatedInformation(request);

        assertTrue(returned instanceof List, "should return the body, not a ResponseEntity");
    }

    @Test
    void bothEndpointsResolveTheApplicationUrlFromTheIncomingRequest() throws Exception {
        givenRequestFor("https", "10.0.0.5", 443);
        when(endPointsInformationProvider.getScannerRelatedEndPointInformation(anyString()))
                .thenReturn(groundTruth);

        controller.getScannerRelatedInformation(request);
        controller.getDastScannerRelatedInformation(request);

        ArgumentCaptor<String> appUrl = ArgumentCaptor.forClass(String.class);
        verify(endPointsInformationProvider, org.mockito.Mockito.times(2))
                .getScannerRelatedEndPointInformation(appUrl.capture());

        for (String url : appUrl.getAllValues()) {
            assertEquals("https://10.0.0.5:443/VulnerableApp/", url);
        }
    }
}
