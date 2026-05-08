package org.sasanlabs.benchmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DastBenchmarkStrategyTest {

    private static final String GROUND_TRUTH_URL = "http://localhost:9090/VulnerableApp/scanner";

    @Mock private RestTemplate restTemplate;

    private DastBenchmarkStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DastBenchmarkStrategy(restTemplate, GROUND_TRUTH_URL);
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
        assertThat(DastBenchmarkStrategy.normalizeNumericId(null)).isEmpty();
        assertThat(DastBenchmarkStrategy.normalizeNumericId("not-a-number")).isEmpty();
    }

    @Test
    void urlNormalization_acceptsAbsoluteRelativeAndContextStrippedForms() {
        assertThat(
                        DastBenchmarkStrategy.normalizeUrl(
                                "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1"))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(DastBenchmarkStrategy.normalizeUrl("/VulnerableApp/SQLInjection/LEVEL_1"))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(DastBenchmarkStrategy.normalizeUrl("/SQLInjection/LEVEL_1"))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(DastBenchmarkStrategy.normalizeUrl("/SQLInjection/LEVEL_1/"))
                .isEqualTo("/SQLInjection/LEVEL_1");
        assertThat(DastBenchmarkStrategy.normalizeUrl("/SQLInjection/LEVEL_1?attack=1' OR 1=1--"))
                .isEqualTo("/SQLInjection/LEVEL_1");
    }

    private static ScannerResponseBean unsecure(String url, VulnerabilityType... types) {
        return unsecure(url, RequestMethod.GET, types);
    }

    private static ScannerResponseBean unsecure(
            String url, RequestMethod method, VulnerabilityType... types) {
        return new ScannerResponseBean(url, "UNSECURE", method, Arrays.asList(types));
    }

    private static ScannerResponseBean secure(String url, VulnerabilityType... types) {
        return new ScannerResponseBean(url, "SECURE", RequestMethod.GET, Arrays.asList(types));
    }

    private static ScannerFindings input(String tool, Finding... findings) {
        List<Finding> list =
                findings.length == 0 ? Collections.emptyList() : Arrays.asList(findings);
        return new ScannerFindings(tool, list);
    }
}
