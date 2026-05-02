package org.sasanlabs.benchmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.web.bind.annotation.RequestMethod;

@ExtendWith(MockitoExtension.class)
class DastBenchmarkStrategyTest {

    @Mock private IEndPointsInformationProvider provider;

    private DastBenchmarkStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DastBenchmarkStrategy(provider);
    }

    @Test
    void perfectMatch_yields100Coverage() throws Exception {
        when(provider.getScannerRelatedEndPointInformation())
                .thenReturn(
                        Arrays.asList(
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                                        VulnerabilityType.BLIND_SQL_INJECTION),
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/XSS/LEVEL_1",
                                        VulnerabilityType.REFLECTED_XSS)));

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
        assertThat(result.getFalsePositives()).isZero();
        assertThat(result.getMissedItems()).isEmpty();
        assertThat(result.getFalsePositiveItems()).isEmpty();
    }

    @Test
    void allMissed_yieldsZeroCoverageAndNoFalsePositives() throws Exception {
        when(provider.getScannerRelatedEndPointInformation())
                .thenReturn(
                        Arrays.asList(
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                                        VulnerabilityType.BLIND_SQL_INJECTION),
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/XSS/LEVEL_1",
                                        VulnerabilityType.REFLECTED_XSS)));

        BenchmarkResult result = strategy.compare(input("ZAP"));

        assertThat(result.getCoverage()).isEqualTo(0.0);
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isEqualTo(2);
        assertThat(result.getFalsePositives()).isZero();
        assertThat(result.getMissedItems())
                .extracting(Finding::getUrl, Finding::getType)
                .containsExactlyInAnyOrder(
                        tuple("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION"),
                        tuple("/XSS/LEVEL_1", "REFLECTED_XSS"));
    }

    @Test
    void mixedDetectedMissedAndFalsePositive() throws Exception {
        when(provider.getScannerRelatedEndPointInformation())
                .thenReturn(
                        Arrays.asList(
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                                        VulnerabilityType.BLIND_SQL_INJECTION),
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/XSS/LEVEL_1",
                                        VulnerabilityType.REFLECTED_XSS),
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/CommandInjection/LEVEL_1",
                                        VulnerabilityType.COMMAND_INJECTION)));

        BenchmarkResult result =
                strategy.compare(
                        input(
                                "ZAP",
                                new Finding("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION"),
                                new Finding("/PathTraversal/LEVEL_1", "PATH_TRAVERSAL")));

        assertThat(result.getTotalExpected()).isEqualTo(3);
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getMissed()).isEqualTo(2);
        assertThat(result.getFalsePositives()).isEqualTo(1);
        assertThat(result.getCoverage())
                .isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.01));
        assertThat(result.getFalsePositiveItems())
                .extracting(Finding::getUrl, Finding::getType)
                .containsExactly(tuple("/PathTraversal/LEVEL_1", "PATH_TRAVERSAL"));
    }

    @Test
    void findingAgainstSecureUrl_isCountedAsFalsePositive() throws Exception {
        when(provider.getScannerRelatedEndPointInformation())
                .thenReturn(
                        Arrays.asList(
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                                        VulnerabilityType.BLIND_SQL_INJECTION),
                                secure(
                                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_2",
                                        VulnerabilityType.BLIND_SQL_INJECTION)));

        BenchmarkResult result =
                strategy.compare(
                        input("ZAP", new Finding("/SQLInjection/LEVEL_2", "BLIND_SQL_INJECTION")));

        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isEqualTo(1);
        assertThat(result.getFalsePositives()).isEqualTo(1);
        assertThat(result.getFalsePositiveItems())
                .extracting(Finding::getUrl)
                .containsExactly("/SQLInjection/LEVEL_2");
    }

    @Test
    void emptyFindingsList_allExpectedAreMissed() throws Exception {
        when(provider.getScannerRelatedEndPointInformation())
                .thenReturn(
                        Collections.singletonList(
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                                        VulnerabilityType.BLIND_SQL_INJECTION)));

        BenchmarkResult result =
                strategy.compare(new ScannerFindings("ZAP", Collections.emptyList()));

        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isEqualTo(1);
        assertThat(result.getFalsePositives()).isZero();
        assertThat(result.getCoverage()).isEqualTo(0.0);
    }

    @Test
    void emptyExpected_yieldsZeroCoverageAndAllFindingsBecomeFalsePositives() throws Exception {
        when(provider.getScannerRelatedEndPointInformation()).thenReturn(Collections.emptyList());

        BenchmarkResult result =
                strategy.compare(
                        input("ZAP", new Finding("/SQLInjection/LEVEL_1", "BLIND_SQL_INJECTION")));

        assertThat(result.getTotalExpected()).isZero();
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isZero();
        assertThat(result.getFalsePositives()).isEqualTo(1);
        assertThat(result.getCoverage()).isEqualTo(0.0);
    }

    @Test
    void typeMatchIsCaseInsensitive() throws Exception {
        when(provider.getScannerRelatedEndPointInformation())
                .thenReturn(
                        Collections.singletonList(
                                unsecure(
                                        "http://localhost:9090/VulnerableApp/SQLInjection/LEVEL_1",
                                        VulnerabilityType.BLIND_SQL_INJECTION)));

        BenchmarkResult result =
                strategy.compare(
                        input("ZAP", new Finding("/SQLInjection/LEVEL_1", "blind_sql_injection")));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getFalsePositives()).isZero();
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
        return new ScannerResponseBean(url, "UNSECURE", RequestMethod.GET, Arrays.asList(types));
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
