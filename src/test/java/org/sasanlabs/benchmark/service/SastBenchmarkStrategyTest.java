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
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScanType;
import org.sasanlabs.benchmark.model.ScannerFindings;

@ExtendWith(MockitoExtension.class)
class SastBenchmarkStrategyTest {

    private static final String SQLI_FILE =
            "src/main/java/org/sasanlabs/service/vulnerability/sqlInjection/"
                    + "BlindSQLInjectionVulnerability.java";
    private static final String XSS_FILE =
            "src/main/java/org/sasanlabs/service/vulnerability/xss/reflected/"
                    + "XSSWithHtmlTagInjection.java";

    @Mock private IExpectedIssuesProvider provider;

    private SastBenchmarkStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SastBenchmarkStrategy(provider);
    }

    @Test
    void perfectMatch_yields100Coverage() throws Exception {
        when(provider.getExpectedIssues())
                .thenReturn(
                        Arrays.asList(
                                expected("CWE-89", "SQL Injection", SQLI_FILE, 56),
                                expected("CWE-79", "Reflected XSS", XSS_FILE, 45)));

        BenchmarkResult result =
                strategy.compare(
                        sastInput(
                                "Semgrep",
                                sastFinding(SQLI_FILE, 56, "CWE-89", "SQL Injection"),
                                sastFinding(XSS_FILE, 45, "CWE-79", "Reflected XSS")));

        assertThat(result.getCoverage()).isEqualTo(100.0);
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isEqualTo(2);
        assertThat(result.getMissed()).isZero();
        assertThat(result.getFalsePositives()).isZero();
    }

    @Test
    void mixedDetectedMissedAndFalsePositive() throws Exception {
        when(provider.getExpectedIssues())
                .thenReturn(
                        Arrays.asList(
                                expected("CWE-89", "SQL Injection", SQLI_FILE, 56),
                                expected("CWE-79", "Reflected XSS", XSS_FILE, 45)));

        BenchmarkResult result =
                strategy.compare(
                        sastInput(
                                "Semgrep",
                                sastFinding(SQLI_FILE, 56, "CWE-89", "SQL Injection"),
                                sastFinding("src/main/java/Imaginary.java", 1, "CWE-99", "X")));

        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getMissed()).isEqualTo(1);
        assertThat(result.getFalsePositives()).isEqualTo(1);
        assertThat(result.getFalsePositiveItems())
                .extracting(Finding::getFilePath, Finding::getLine)
                .containsExactly(tuple("src/main/java/Imaginary.java", 1));
    }

    @Test
    void cweOnlyFinding_matchesGroundTruth() throws Exception {
        when(provider.getExpectedIssues())
                .thenReturn(
                        Collections.singletonList(
                                expected("CWE-89", "SQL Injection", SQLI_FILE, 56)));

        BenchmarkResult result =
                strategy.compare(sastInput("Semgrep", sastFinding(SQLI_FILE, 56, "CWE-89", null)));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getFalsePositives()).isZero();
    }

    @Test
    void typeOnlyFindingCaseInsensitive_matchesGroundTruth() throws Exception {
        when(provider.getExpectedIssues())
                .thenReturn(
                        Collections.singletonList(
                                expected("CWE-89", "SQL Injection", SQLI_FILE, 56)));

        BenchmarkResult result =
                strategy.compare(
                        sastInput("Semgrep", sastFinding(SQLI_FILE, 56, null, "sql injection")));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getFalsePositives()).isZero();
    }

    @Test
    void filePathNormalization_handlesBackslashesAndLeadingDotSlash() throws Exception {
        when(provider.getExpectedIssues())
                .thenReturn(
                        Collections.singletonList(
                                expected("CWE-89", "SQL Injection", SQLI_FILE, 56)));

        String windowsishPath = "./" + SQLI_FILE.replace('/', '\\');

        BenchmarkResult result =
                strategy.compare(
                        sastInput(
                                "Semgrep",
                                sastFinding(windowsishPath, 56, "CWE-89", "SQL Injection")));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getFalsePositives()).isZero();
    }

    @Test
    void duplicateFindings_areCountedOnce() throws Exception {
        when(provider.getExpectedIssues())
                .thenReturn(
                        Collections.singletonList(
                                expected("CWE-89", "SQL Injection", SQLI_FILE, 56)));

        BenchmarkResult result =
                strategy.compare(
                        sastInput(
                                "Semgrep",
                                sastFinding(SQLI_FILE, 56, "CWE-89", "SQL Injection"),
                                sastFinding(SQLI_FILE, 56, "CWE-89", "SQL Injection")));

        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getFalsePositives()).isZero();
    }

    @Test
    void emptyExpected_allFindingsBecomeFalsePositives() throws Exception {
        when(provider.getExpectedIssues()).thenReturn(Collections.emptyList());

        BenchmarkResult result =
                strategy.compare(
                        sastInput(
                                "Semgrep", sastFinding(SQLI_FILE, 56, "CWE-89", "SQL Injection")));

        assertThat(result.getTotalExpected()).isZero();
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isZero();
        assertThat(result.getFalsePositives()).isEqualTo(1);
        assertThat(result.getCoverage()).isEqualTo(0.0);
    }

    @Test
    void emptyFindings_allExpectedAreMissed() throws Exception {
        when(provider.getExpectedIssues())
                .thenReturn(
                        Arrays.asList(
                                expected("CWE-89", "SQL Injection", SQLI_FILE, 56),
                                expected("CWE-79", "Reflected XSS", XSS_FILE, 45)));

        BenchmarkResult result =
                strategy.compare(
                        new ScannerFindings("Semgrep", ScanType.SAST, Collections.emptyList()));

        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isEqualTo(2);
        assertThat(result.getFalsePositives()).isZero();
        assertThat(result.getMissedItems())
                .extracting(Finding::getFilePath, Finding::getLine, Finding::getCwe)
                .containsExactlyInAnyOrder(
                        tuple(SQLI_FILE, 56, "CWE-89"), tuple(XSS_FILE, 45, "CWE-79"));
    }

    @Test
    void filePathNormalization_unitCases() {
        assertThat(SastBenchmarkStrategy.normalizeFilePath("./src/main/java/Foo.java"))
                .isEqualTo("src/main/java/Foo.java");
        assertThat(SastBenchmarkStrategy.normalizeFilePath("src\\main\\java\\Foo.java"))
                .isEqualTo("src/main/java/Foo.java");
        assertThat(SastBenchmarkStrategy.normalizeFilePath("  ./src/main/Foo.java  "))
                .isEqualTo("src/main/Foo.java");
        assertThat(SastBenchmarkStrategy.normalizeFilePath(null)).isEqualTo("");
    }

    private static ExpectedIssue expected(String cwe, String type, String file, int line) {
        return new ExpectedIssue(cwe, type, file, line, 1);
    }

    private static Finding sastFinding(String file, int line, String cwe, String type) {
        return new Finding(null, type, file, line, cwe);
    }

    private static ScannerFindings sastInput(String tool, Finding... findings) {
        List<Finding> list = Arrays.asList(findings);
        return new ScannerFindings(tool, ScanType.SAST, list);
    }
}
