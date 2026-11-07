package org.sasanlabs.benchmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScanType;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.sasanlabs.internal.utility.Protocol;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DastBenchmarkStrategyTest {

    private static final String CONTEXT_PATH = "/VulnerableApp";

    private static final String GROUND_TRUTH_URL = "http://localhost:9090/VulnerableApp/scanner";

    private static final String URL = "http://localhost:9090/VulnerableApp/XSS/LEVEL_1";

    @Mock private RestTemplate restTemplate;

    private DastBenchmarkStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DastBenchmarkStrategy(restTemplate, GROUND_TRUTH_URL, CONTEXT_PATH);
    }

    private void stubGroundTruth(ScannerResponseBean... beans) {
        when(restTemplate.getForObject(eq(GROUND_TRUTH_URL), any(Class.class))).thenReturn(beans);
    }

    @Test
    void perfectMatch_yields100Coverage() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        VulnerabilityType.BLIND_SQL_INJECTION),
                unsecure(
                        "http://localhost:9090/VulnerableApp/XSS/LEVEL_1",
                        VulnerabilityType.REFLECTED_XSS));

        BenchmarkResult result =
                strategy.compare(
                        input(
                                "ZAP",
                                new Finding("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION"),
                                new Finding("/XSS/LEVEL_1", "REFLECTED_XSS")));

        assertThat(result.getCoverage()).isEqualTo(100.0);
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isEqualTo(2);
        assertThat(result.getMissed()).isZero();
        assertThat(result.getUnmatched()).isZero();
        assertThat(result.getMissedItems()).isEmpty();
        assertThat(result.getUnmatchedItems()).isEmpty();
    }

    @Test
    void allMissed_yieldsZeroCoverageAndNoUnmatched() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        VulnerabilityType.BLIND_SQL_INJECTION),
                unsecure(
                        "http://localhost:9090/VulnerableApp/XSS/LEVEL_1",
                        VulnerabilityType.REFLECTED_XSS));

        BenchmarkResult result = strategy.compare(input("ZAP"));

        assertThat(result.getCoverage()).isEqualTo(0.0);
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isEqualTo(2);
        assertThat(result.getUnmatched()).isZero();
        assertThat(result.getMissedItems())
                .extracting(Finding::getUrl, Finding::getType)
                .containsExactlyInAnyOrder(
                        tuple("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION"),
                        tuple("/XSS/LEVEL_1", "REFLECTED_XSS"));
    }

    @Test
    void mixedDetectedMissedAndUnmatched() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        VulnerabilityType.BLIND_SQL_INJECTION),
                unsecure(
                        "http://localhost:9090/VulnerableApp/XSS/LEVEL_1",
                        VulnerabilityType.REFLECTED_XSS),
                unsecure(
                        "http://localhost:9090/VulnerableApp/CommandInjection/LEVEL_1",
                        VulnerabilityType.COMMAND_INJECTION));

        BenchmarkResult result =
                strategy.compare(
                        input(
                                "ZAP",
                                new Finding("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION"),
                                new Finding("/PathTraversal/LEVEL_1", "PATH_TRAVERSAL")));

        assertThat(result.getTotalExpected()).isEqualTo(3);
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getMissed()).isEqualTo(2);
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getCoverage())
                .isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.01));
        assertThat(result.getUnmatchedItems())
                .extracting(Finding::getUrl, Finding::getType)
                .containsExactly(tuple("/PathTraversal/LEVEL_1", "PATH_TRAVERSAL"));
        assertThat(result.getDetectedItems())
                .extracting(Finding::getUrl, Finding::getType, Finding::getCwe)
                .containsExactly(tuple("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION", "CWE-89"));
        assertThat(result.getMissedItems())
                .extracting(Finding::getUrl)
                .containsExactlyInAnyOrder("/XSS/LEVEL_1", "/CommandInjection/LEVEL_1");
    }

    @Test
    void findingAgainstSecureUrl_isCountedAsUnmatched() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        VulnerabilityType.BLIND_SQL_INJECTION),
                secure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_2",
                        VulnerabilityType.BLIND_SQL_INJECTION));

        BenchmarkResult result =
                strategy.compare(
                        input("ZAP", new Finding("/SQLInjection/LEVEL_2", "BLIND_SQL_INJECTION")));

        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isEqualTo(1);
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getUnmatchedItems())
                .extracting(Finding::getUrl)
                .containsExactly("/SQLInjection/LEVEL_2");
    }

    @Test
    void emptyFindingsList_allExpectedAreMissed() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        VulnerabilityType.BLIND_SQL_INJECTION));

        BenchmarkResult result =
                strategy.compare(new ScannerFindings("ZAP", Collections.emptyList()));

        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
        assertThat(result.getCoverage()).isEqualTo(0.0);
    }

    @Test
    void emptyExpected_yieldsZeroCoverageAndAllFindingsBecomeUnmatched() throws Exception {
        stubGroundTruth();

        BenchmarkResult result =
                strategy.compare(
                        input("ZAP", new Finding("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION")));

        assertThat(result.getTotalExpected()).isZero();
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getCoverage()).isEqualTo(0.0);
    }

    @Test
    void typeMatchIsCaseInsensitive() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        VulnerabilityType.BLIND_SQL_INJECTION));

        BenchmarkResult result =
                strategy.compare(
                        input("ZAP", new Finding("/SQLInjection/LEVEL_1", "blind_sql_injection")));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void cweMatch_detectsEvenWhenScannerEmitsForeignTypeName() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        VulnerabilityType.BLIND_SQL_INJECTION));

        Finding scannerFinding =
                new Finding(
                        "/SQLInjection/LEVEL_1",
                        "SQL Injection - Boolean Based",
                        null,
                        null,
                        "CWE-89");

        BenchmarkResult result = strategy.compare(input("ZAP", scannerFinding));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getMissed()).isZero();
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void cweOnlyFinding_isDetected() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/XSS/LEVEL_1",
                        VulnerabilityType.REFLECTED_XSS));

        Finding scannerFinding = new Finding("/XSS/LEVEL_1", null, null, null, "79");

        BenchmarkResult result = strategy.compare(input("ZAP", scannerFinding));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void wascOnlyFinding_isDetected() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/PathTraversal/LEVEL_1",
                        VulnerabilityType.PATH_TRAVERSAL));

        Finding scannerFinding =
                new Finding("/PathTraversal/LEVEL_1", null, null, null, null, "33");

        BenchmarkResult result = strategy.compare(input("ZAP", scannerFinding));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void cweMismatchOnDifferentUrl_isStillUnmatched() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        VulnerabilityType.BLIND_SQL_INJECTION));

        Finding scannerFinding =
                new Finding("/SomeOtherEndpoint/LEVEL_1", null, null, null, "CWE-89");

        BenchmarkResult result = strategy.compare(input("ZAP", scannerFinding));

        assertThat(result.getDetected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
    }

    @Test
    void cweAndTypeBothCorrect_countsAsOneDetection() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        VulnerabilityType.BLIND_SQL_INJECTION));

        Finding scannerFinding =
                new Finding("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION", null, null, "CWE-89");

        BenchmarkResult result = strategy.compare(input("ZAP", scannerFinding));

        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void methodStrictMatch_whenScannerEmitsMatchingMethod() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        RequestMethod.GET,
                        VulnerabilityType.BLIND_SQL_INJECTION));

        Finding scannerFinding =
                new Finding(
                        "/SQLInjection/LEVEL_1",
                        "BLIND_SQL_INJECTION",
                        null,
                        null,
                        null,
                        null,
                        "GET");

        BenchmarkResult result = strategy.compare(input("ZAP", scannerFinding));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void methodMismatch_isUnmatched() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        RequestMethod.GET,
                        VulnerabilityType.BLIND_SQL_INJECTION));

        Finding scannerFinding =
                new Finding(
                        "/SQLInjection/LEVEL_1",
                        "BLIND_SQL_INJECTION",
                        null,
                        null,
                        null,
                        null,
                        "POST");

        BenchmarkResult result = strategy.compare(input("ZAP", scannerFinding));

        assertThat(result.getDetected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getUnmatchedItems())
                .extracting(Finding::getMethod)
                .containsExactly("POST");
    }

    @Test
    void methodOmitted_matchesAnyGroundTruthMethod() throws Exception {
        stubGroundTruth(
                unsecure(
                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                        RequestMethod.POST,
                        VulnerabilityType.BLIND_SQL_INJECTION));

        BenchmarkResult result =
                strategy.compare(
                        input("ZAP", new Finding("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION")));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void groundTruthFetchFailure_isWrappedInIOException() {
        when(restTemplate.getForObject(eq(GROUND_TRUTH_URL), any(Class.class)))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("connect"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                strategy.compare(
                                        input(
                                                "ZAP",
                                                new Finding(
                                                        "/SQLInjection/LEVEL_1",
                                                        "BLIND_SQL_INJECTION"))))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining(GROUND_TRUTH_URL);
    }

    @Test
    void numericIdNormalization_acceptsCweDashAndBareDigits() {
        assertThat(DastBenchmarkStrategy.normalizeNumericId("CWE-89")).isEqualTo("89");
        assertThat(DastBenchmarkStrategy.normalizeNumericId("cwe-89")).isEqualTo("89");
        assertThat(DastBenchmarkStrategy.normalizeNumericId("89")).isEqualTo("89");
        assertThat(DastBenchmarkStrategy.normalizeNumericId(" 89 ")).isEqualTo("89");
        assertThat(DastBenchmarkStrategy.normalizeNumericId("cwe89")).isEqualTo("89");
        assertThat(DastBenchmarkStrategy.normalizeNumericId("WASC-08")).isEqualTo("8");
        assertThat(DastBenchmarkStrategy.normalizeNumericId("CWE-089")).isEqualTo("89");
        assertThat(DastBenchmarkStrategy.normalizeNumericId("00079")).isEqualTo("79");
        assertThat(DastBenchmarkStrategy.normalizeNumericId(null)).isEmpty();
        assertThat(DastBenchmarkStrategy.normalizeNumericId("not-a-number")).isEmpty();
    }

    @Test
    void urlNormalization_acceptsAbsoluteRelativeAndContextStrippedForms() {
        assertThat(
                        DastBenchmarkStrategy.normalizeUrl(
                                "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                                CONTEXT_PATH))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(
                        DastBenchmarkStrategy.normalizeUrl(
                                "/VulnerableApp/SQLInjection/LEVEL_1", CONTEXT_PATH))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(DastBenchmarkStrategy.normalizeUrl("/SQLInjection/LEVEL_1", CONTEXT_PATH))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(DastBenchmarkStrategy.normalizeUrl("/SQLInjection/LEVEL_1/", CONTEXT_PATH))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(
                        DastBenchmarkStrategy.normalizeUrl(
                                "/SQLInjection/LEVEL_1?attack=1' OR 1=1--", CONTEXT_PATH))
                .isEqualTo("/SQLInjection/LEVEL_1");
    }

    /**
     * The context path is configuration, not a constant, so the same collapsing has to work for a
     * deployment served anywhere else. Under a hardcoded {@code /VulnerableApp} the absolute and
     * context-relative forms below would key differently and stop matching each other.
     */
    @Test
    void urlNormalization_stripsWhicheverContextPathTheAppIsServedUnder() {
        assertThat(
                        DastBenchmarkStrategy.normalizeUrl(
                                "https://10.0.0.5:443/customCtx/SQLInjection/LEVEL_1",
                                "/customCtx"))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(
                        DastBenchmarkStrategy.normalizeUrl(
                                "/customCtx/SQLInjection/LEVEL_1", "/customCtx"))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(DastBenchmarkStrategy.normalizeUrl("/customCtx", "/customCtx")).isEqualTo("/");

        // A root deployment has an empty context path and must strip nothing.
        assertThat(DastBenchmarkStrategy.normalizeUrl("/SQLInjection/LEVEL_1", ""))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(DastBenchmarkStrategy.normalizeUrl("/SQLInjection/LEVEL_1", null))
                .isEqualTo("/SQLInjection/LEVEL_1");
    }

    private static ScannerResponseBean unsecure(String url, VulnerabilityType... types) {
        return unsecure(url, RequestMethod.GET, types);
    }

    private static ScannerResponseBean unsecure(
            String url, RequestMethod method, VulnerabilityType... types) {
        return new ScannerResponseBean(
                url, "UNSECURE", method, Protocol.HTTP, Arrays.asList(types));
    }

    private static ScannerResponseBean secure(String url, VulnerabilityType... types) {
        return new ScannerResponseBean(
                url, "SECURE", RequestMethod.GET, Protocol.HTTP, Arrays.asList(types));
    }

    private static ScannerFindings input(String tool, Finding... findings) {
        List<Finding> list =
                findings.length == 0 ? Collections.emptyList() : Arrays.asList(findings);
        return new ScannerFindings(tool, list);
    }

    private static ScannerResponseBean row(Protocol protocol, RequestMethod method) {
        return new ScannerResponseBean(
                URL, "UNSECURE", method, protocol, Arrays.asList(VulnerabilityType.REFLECTED_XSS));
    }

    private static ScannerFindings scan(List<Protocol> supported, Finding... findings) {
        return new ScannerFindings("Demo", ScanType.DAST, Arrays.asList(findings), supported);
    }

    private static Finding finding(Protocol protocol, String method) {
        return new Finding(
                "/XSS/LEVEL_1", "REFLECTED_XSS", null, null, null, null, method, protocol);
    }

    @Test
    void legacyHttpInputExcludesWsFromCoverageDenominator() throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET), row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        new ScannerFindings(
                                "Legacy",
                                Collections.singletonList(
                                        new Finding("/XSS/LEVEL_1", "REFLECTED_XSS"))));
        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getCoverage()).isEqualTo(100.0);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void bothProtocolsOnSameUrlRemainDistinctRows() throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET), row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        scan(
                                Arrays.asList(Protocol.HTTP, Protocol.WS),
                                finding(Protocol.HTTP, "GET"),
                                finding(Protocol.WS, "POST")));
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isEqualTo(2);
        assertThat(result.getCoverage()).isEqualTo(100.0);
        assertThat(result.getDetectedItems())
                .extracting(Finding::getProtocol)
                .containsExactly(null, Protocol.WS);
    }

    @Test
    void wsIgnoresHandshakeMethod() throws Exception {
        stubGroundTruth(row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        scan(Collections.singletonList(Protocol.WS), finding(Protocol.WS, "POST")));
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
        assertThat(result.getDetectedItems().get(0).getMethod()).isNull();
    }

    @Test
    void httpFindingMustNotMatchWsEvenOnSameUrlAndType() throws Exception {
        stubGroundTruth(row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        scan(
                                Arrays.asList(Protocol.HTTP, Protocol.WS),
                                finding(Protocol.HTTP, "GET")));
        assertThat(result.getDetected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
    }

    @Test
    void wsFindingMustNotMatchHttpEvenOnSameUrlAndType() throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        scan(
                                Arrays.asList(Protocol.HTTP, Protocol.WS),
                                finding(Protocol.WS, "GET")));
        assertThat(result.getDetected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
    }

    @Test
    void strictHttpMethodMatchingIsUnchanged() throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        scan(
                                Collections.singletonList(Protocol.HTTP),
                                finding(Protocol.HTTP, "POST")));
        assertThat(result.getDetected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
    }

    @Test
    void wsMatchesCweWithoutType() throws Exception {
        stubGroundTruth(row(Protocol.WS, RequestMethod.GET));
        Finding cweOnly =
                new Finding("/XSS/LEVEL_1", null, null, null, "CWE-79", null, null, Protocol.WS);
        BenchmarkResult result =
                strategy.compare(scan(Collections.singletonList(Protocol.WS), cweOnly));
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void wsMatchesWascWithoutType() throws Exception {
        stubGroundTruth(
                new ScannerResponseBean(
                        URL,
                        "UNSECURE",
                        RequestMethod.GET,
                        Protocol.WS,
                        Arrays.asList(VulnerabilityType.PATH_TRAVERSAL)));
        Finding wascOnly =
                new Finding("/XSS/LEVEL_1", null, null, null, null, "33", null, Protocol.WS);
        BenchmarkResult result =
                strategy.compare(scan(Collections.singletonList(Protocol.WS), wascOnly));
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void duplicateWsFindingsAreCountedOnlyOnce() throws Exception {
        stubGroundTruth(row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        scan(
                                Collections.singletonList(Protocol.WS),
                                finding(Protocol.WS, "GET"),
                                finding(Protocol.WS, "POST")));
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void duplicateWsGroundTruthRowsWithDifferentHandshakeMethodsCountOnce() throws Exception {
        stubGroundTruth(row(Protocol.WS, RequestMethod.GET), row(Protocol.WS, RequestMethod.POST));
        BenchmarkResult result =
                strategy.compare(
                        scan(Collections.singletonList(Protocol.WS), finding(Protocol.WS, null)));
        assertThat(result.getTotalExpected()).isEqualTo(1);
    }

    @Test
    void unsupportedWsFindingStaysUnmatchedAndKeepsProtocolInResult() throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET), row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        scan(Collections.singletonList(Protocol.HTTP), finding(Protocol.WS, null)));
        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getDetected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getUnmatchedItems().get(0).getProtocol()).isEqualTo(Protocol.WS);
    }

    @Test
    void legacyGroundTruthWithoutProtocolIsHttp() throws Exception {
        stubGroundTruth(row(null, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        new ScannerFindings(
                                "Legacy",
                                Collections.singletonList(
                                        new Finding("/XSS/LEVEL_1", "REFLECTED_XSS"))));
        assertThat(result.getDetected()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(Protocol.class)
    void singleFindingDetectsOnlyItsProtocolInMixedScope(Protocol protocol) throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET), row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        scan(List.of(Protocol.HTTP, Protocol.WS), finding(protocol, "GET")));
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getMissed()).isEqualTo(1);
        assertThat(result.getCoverage()).isEqualTo(50.0);
        assertThat(result.getUnmatched()).isZero();
        assertThat(result.getDetectedItems().get(0).getProtocol())
                .isEqualTo(protocol == Protocol.WS ? Protocol.WS : null);
        assertThat(result.getMissedItems().get(0).getProtocol())
                .isEqualTo(protocol == Protocol.HTTP ? Protocol.WS : null);
    }

    @ParameterizedTest
    @CsvSource({
        "HTTP,WS,TYPE",
        "WS,HTTP,TYPE",
        "HTTP,WS,CWE",
        "WS,HTTP,CWE",
        "HTTP,WS,WASC",
        "WS,HTTP,WASC"
    })
    void taxonomyAxesCannotCrossProtocols(Protocol reported, Protocol expected, String axis)
            throws Exception {
        stubGroundTruth(
                new ScannerResponseBean(
                        URL,
                        "UNSECURE",
                        RequestMethod.GET,
                        expected,
                        List.of(VulnerabilityType.PATH_TRAVERSAL)));
        Finding finding =
                new Finding(
                        "/XSS/LEVEL_1",
                        axis.equals("TYPE") ? "PATH_TRAVERSAL" : null,
                        null,
                        null,
                        axis.equals("CWE") ? "CWE-22" : null,
                        axis.equals("WASC") ? "33" : null,
                        "GET",
                        reported);
        BenchmarkResult result =
                strategy.compare(scan(List.of(Protocol.HTTP, Protocol.WS), finding));
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isEqualTo(1);
        assertThat(result.getUnmatched()).isEqualTo(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"GET", "POST", "PATCH", "DELETE", "ANY", "not-an-http-method", " "})
    void wsMatchingNeverDependsOnHandshakeMethod(String method) throws Exception {
        stubGroundTruth(row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(scan(List.of(Protocol.WS), finding(Protocol.WS, method)));
        assertThat(result.getCoverage()).isEqualTo(100.0);
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isZero();
    }

    @Test
    void emptyMixedScanStillCountsBothProtocolsAsMissed() throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET), row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result = strategy.compare(scan(List.of(Protocol.HTTP, Protocol.WS)));
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getMissed()).isEqualTo(2);
        assertThat(result.getCoverage()).isZero();
    }

    @Test
    void wsOnlyScopeExcludesHttpEvenWhenNoFindingsExist() throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET), row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result = strategy.compare(scan(List.of(Protocol.WS)));
        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getMissedItems())
                .extracting(Finding::getProtocol)
                .containsExactly(Protocol.WS);
    }

    @Test
    void nullCapabilityDefaultsToHttpIndependentlyOfFindings() throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET), row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result = strategy.compare(scan(null));
        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getMissed()).isEqualTo(1);
        assertThat(result.getMissedItems().get(0).getProtocol()).isNull();
    }

    @Test
    void wsUnmatchedDuplicatesCollapseAcrossHandshakeMethods() throws Exception {
        stubGroundTruth();
        BenchmarkResult result =
                strategy.compare(
                        scan(
                                List.of(Protocol.WS),
                                finding(Protocol.WS, "GET"),
                                finding(Protocol.WS, "POST"),
                                finding(Protocol.WS, null)));
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getUnmatchedItems().get(0).getProtocol()).isEqualTo(Protocol.WS);
        assertThat(result.getUnmatchedItems().get(0).getMethod()).isEqualTo("GET");
        assertThat(result.getCoverage()).isZero();
    }

    @Test
    void httpMethodsRemainDistinctWhileWsGroundTruthCollapses() throws Exception {
        stubGroundTruth(
                row(Protocol.HTTP, RequestMethod.GET),
                row(Protocol.HTTP, RequestMethod.POST),
                row(Protocol.WS, RequestMethod.GET),
                row(Protocol.WS, RequestMethod.POST));
        BenchmarkResult result =
                strategy.compare(
                        scan(
                                List.of(Protocol.HTTP, Protocol.WS),
                                finding(Protocol.HTTP, null),
                                finding(Protocol.WS, "PATCH")));
        assertThat(result.getTotalExpected()).isEqualTo(3);
        assertThat(result.getDetected()).isEqualTo(3);
        assertThat(result.getDetectedItems())
                .extracting(Finding::getMethod)
                .containsExactly("GET", "POST", null);
    }

    @Test
    void httpMethodNamedWsCannotCollideWithWsKeys() throws Exception {
        stubGroundTruth(row(Protocol.WS, RequestMethod.GET));
        BenchmarkResult result =
                strategy.compare(
                        scan(List.of(Protocol.HTTP, Protocol.WS), finding(Protocol.HTTP, "WS")));
        assertThat(result.getDetected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
    }

    @Test
    void wsUrlsUseExistingNormalizationAndExplicitProtocol() throws Exception {
        stubGroundTruth(row(Protocol.WS, RequestMethod.GET));
        Finding finding =
                new Finding(
                        "wss://localhost:9090/VulnerableApp/XSS/LEVEL_1/?token=test",
                        "reflected_xss",
                        null,
                        null,
                        null,
                        null,
                        "POST",
                        Protocol.WS);
        BenchmarkResult result = strategy.compare(scan(List.of(Protocol.WS), finding));
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getDetectedItems().get(0).getUrl()).isEqualTo("/XSS/LEVEL_1");
    }

    @ParameterizedTest(name = "explicitNullProtocol={0}")
    @ValueSource(booleans = {false, true})
    void wssJsonFindingWithOmittedOrNullProtocolDetectsOnlyHttpRow(boolean explicitNullProtocol)
            throws Exception {
        stubGroundTruth(row(Protocol.HTTP, RequestMethod.GET), row(Protocol.WS, RequestMethod.GET));
        // A WebSocket URL does not override the request's documented HTTP protocol default.
        String json =
                "{\"tool\":\"Protocol defaults\",\"supportedProtocols\":[\"HTTP\",\"WS\"],"
                        + "\"findings\":[{\"url\":\"wss://localhost:9090/VulnerableApp/XSS/LEVEL_1/?token=test\","
                        + "\"type\":\"REFLECTED_XSS\",\"method\":\"GET\""
                        + (explicitNullProtocol ? ",\"protocol\":null" : "")
                        + "}]}";
        ScannerFindings input = new ObjectMapper().readValue(json, ScannerFindings.class);

        BenchmarkResult result = strategy.compare(input);

        assertThat(input.getFindings().get(0).getProtocol()).isNull();
        assertThat(result)
                .extracting("totalExpected", "detected", "missed", "unmatched", "coverage")
                .containsExactly(2, 1, 1, 0, 50.0);
        assertThat(result.getDetectedItems())
                .extracting(
                        Finding::getUrl, Finding::getType, Finding::getProtocol, Finding::getMethod)
                .containsExactly(tuple("/XSS/LEVEL_1", "REFLECTED_XSS", null, "GET"));
        assertThat(result.getMissedItems())
                .extracting(
                        Finding::getUrl, Finding::getType, Finding::getProtocol, Finding::getMethod)
                .containsExactly(tuple("/XSS/LEVEL_1", "REFLECTED_XSS", Protocol.WS, null));
    }

    @Test
    void secureWsRowsNeverEnterDenominator() throws Exception {
        stubGroundTruth(
                new ScannerResponseBean(
                        URL,
                        "SECURE",
                        RequestMethod.GET,
                        Protocol.WS,
                        List.of(VulnerabilityType.REFLECTED_XSS)));
        BenchmarkResult result =
                strategy.compare(scan(List.of(Protocol.WS), finding(Protocol.WS, null)));
        assertThat(result.getTotalExpected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getCoverage()).isZero();
    }

    @Test
    void nullGroundTruthResponseRetainsEmptyGroundTruthSemantics() throws Exception {
        when(restTemplate.getForObject(eq(GROUND_TRUTH_URL), any(Class.class))).thenReturn(null);
        BenchmarkResult result =
                strategy.compare(scan(List.of(Protocol.WS), finding(Protocol.WS, null)));
        assertThat(result.getTotalExpected()).isZero();
        assertThat(result.getCoverage()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
    }

    @Test
    void legacyHttpScannerNeverDetectsWebSocketHandshake() throws Exception {
        stubGroundTruth(
                new ScannerResponseBean(
                        "http://localhost:9090/VulnerableApp/shared",
                        "UNSECURE",
                        RequestMethod.GET,
                        Protocol.WS,
                        List.of(VulnerabilityType.REFLECTED_XSS)));
        BenchmarkResult result =
                strategy.compare(
                        new ScannerFindings(
                                "Legacy",
                                List.of(
                                        new Finding(
                                                "/shared",
                                                "REFLECTED_XSS",
                                                null,
                                                null,
                                                null,
                                                null,
                                                "GET"))));
        assertThat(result)
                .extracting("totalExpected", "detected", "missed", "unmatched", "coverage")
                .containsExactly(0, 0, 0, 1, 0.0);
    }
}
