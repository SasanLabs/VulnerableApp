package org.sasanlabs.benchmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScanType;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class MergedGroundTruthTest {
    private static final String DAST_URL = "http://facade/scanner/dast";
    private static final String SAST_URL = "http://facade/scanner/sast";
    private static final String LOCAL_URL = "http://local/VulnerableApp/scanner/dast";
    private final RestTemplate rest = new RestTemplateBuilder().build();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final BenchmarkGroundTruthProvider provider =
            new BenchmarkGroundTruthProvider(
                    rest,
                    () ->
                            Collections.singletonList(
                                    new ExpectedIssue("CWE-89", "SQL", "Local.java", 10, 1)),
                    DAST_URL,
                    LOCAL_URL,
                    SAST_URL,
                    "VulnerableApp");

    @Test
    void dastReadsFacadeObjectAndScoresAllApplications() throws Exception {
        respond(
                DAST_URL,
                "{\"VulnerableApp\":"
                        + dastRows("/VulnerableApp/sql", "BLIND_SQL_INJECTION")
                        + ",\"VulnerableApp-jsp\":"
                        + dastRows("/VulnerableApp-jsp/xss", "REFLECTED_XSS")
                        + "}");
        BenchmarkResult result = dast(new Finding("/VulnerableApp-jsp/xss", "REFLECTED_XSS"));
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getCoverage()).isEqualTo(50.0);
        assertThat(result.getApplications().get("VulnerableApp-jsp").getCoverage())
                .isEqualTo(100.0);
        assertThat(result.getApplications().get("VulnerableApp").getMissed()).isEqualTo(1);
        assertThat(result.getMissedItems())
                .extracting(Finding::getApplication)
                .containsExactly("VulnerableApp");
        assertThat(result.getGroundTruthFallback()).isFalse();
        server.verify();
    }

    @Test
    void dastPreservesUnknownNativeTypesAndSeparatesSameLocationAcrossApps() throws Exception {
        respond(
                DAST_URL,
                "{\"alpha\":"
                        + dastRows("/same", "native-php-type")
                        + ",\"beta\":"
                        + dastRows("/same", "native-php-type")
                        + "}");
        BenchmarkResult result =
                dast(new Finding("/same", "native-php-type").withApplication("beta"));
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getApplications().get("alpha").getDetected()).isZero();
        assertThat(result.getApplications().get("beta").getDetected()).isEqualTo(1);
    }

    @Test
    void ambiguousDastFindingCannotCreditMultipleApplications() throws Exception {
        respond(
                DAST_URL,
                "{\"alpha\":"
                        + dastRows("/same", "BLIND_SQL_INJECTION")
                        + ",\"beta\":"
                        + dastRows("/same", "BLIND_SQL_INJECTION")
                        + "}");
        BenchmarkResult result = dast(new Finding("/same", "BLIND_SQL_INJECTION"));
        assertThat(result.getDetected()).isZero();
        assertThat(result.getMissed()).isEqualTo(2);
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getUnmatchedItems().get(0).getApplication()).isNull();
    }

    @Test
    void dastUnmatchedRouteIsAttributedByApplicationPrefix() throws Exception {
        respond(
                DAST_URL,
                "{\"VulnerableApp\":"
                        + dastRows("/VulnerableApp/sql", "BLIND_SQL_INJECTION")
                        + ",\"other\":[]}");
        BenchmarkResult result = dast(new Finding("/other/unknown?payload=test", "REFLECTED_XSS"));
        assertThat(result.getApplications().get("other").getUnmatched()).isEqualTo(1);
        assertThat(result.getUnmatchedItems().get(0).getApplication()).isEqualTo("other");
    }

    @Test
    void dastFallbackFetchesLocalArrayAndKeepsOnlyLocalScope() throws Exception {
        server.expect(requestTo(DAST_URL))
                .andRespond(withException(new IOException("connection refused")));
        respond(LOCAL_URL, dastRows("/VulnerableApp/sql", "BLIND_SQL_INJECTION"));
        BenchmarkResult result = dast(new Finding("/sql", null, null, null, "CWE-89"));
        assertThat(result.getGroundTruthFallback()).isTrue();
        assertThat(result.getApplications()).containsOnlyKeys("VulnerableApp");
        assertThat(result.getCoverage()).isEqualTo(100.0);
        server.verify();
    }

    @Test
    void unavailableFacadeAndLocalDastDoNotProduceMisleadingEmptySuccess() {
        server.expect(requestTo(DAST_URL)).andRespond(withServerError());
        server.expect(requestTo(LOCAL_URL)).andRespond(withServerError());
        assertThatThrownBy(() -> dast()).isInstanceOf(IOException.class);
        server.verify();
    }

    @Test
    void standaloneDastArrayRetainsLegacyUrlAndAnyAxisMatching() throws Exception {
        respond(DAST_URL, dastRows("http://java/VulnerableApp/sql", "BLIND_SQL_INJECTION"));
        BenchmarkResult result =
                dast(
                        new Finding(
                                "/sql?x=1",
                                "scanner-specific",
                                null,
                                null,
                                "CWE-089",
                                null,
                                "GET"));
        assertThat(result.getCoverage()).isEqualTo(100.0);
        assertThat(result.getApplications()).containsOnlyKeys("VulnerableApp");
        server.verify();
    }

    @Test
    void sastScoresPerAppAndPreservesMissedAndUnmatchedLabels() throws Exception {
        respond(
                SAST_URL,
                "{\"java\":"
                        + sastRows("Java.java", 10)
                        + ",\"php\":"
                        + sastRows("index.php", 20)
                        + "}");
        BenchmarkResult result =
                sast(
                        sastFinding("./Java.java", 10),
                        sastFinding("other.php", 30).withApplication("php"));
        assertThat(result.getTotalExpected()).isEqualTo(2);
        assertThat(result.getCoverage()).isEqualTo(50.0);
        assertThat(result.getApplications().get("java").getDetected()).isEqualTo(1);
        assertThat(result.getApplications().get("php").getMissed()).isEqualTo(1);
        assertThat(result.getApplications().get("php").getUnmatched()).isEqualTo(1);
        assertThat(result.getMissedItems().get(0).getApplication()).isEqualTo("php");
        assertThat(result.getUnmatchedItems().get(0).getApplication()).isEqualTo("php");
        server.verify();
    }

    @Test
    void sameSastLocationRequiresApplicationToAvoidCrossAppCredit() throws Exception {
        respond(
                SAST_URL,
                "{\"one\":"
                        + sastRows("index.php", 20)
                        + ",\"two\":"
                        + sastRows("index.php", 20)
                        + "}");
        BenchmarkResult result =
                sast(
                        sastFinding("index.php", 20),
                        sastFinding("index.php", 20).withApplication("two"));
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getApplications().get("one").getDetected()).isZero();
        assertThat(result.getApplications().get("two").getDetected()).isEqualTo(1);
    }

    @Test
    void duplicatesRemainSeparateAcrossApplications() throws Exception {
        respond(
                SAST_URL,
                "{\"one\":"
                        + sastRows("index.php", 20)
                        + ",\"two\":"
                        + sastRows("index.php", 20)
                        + "}");
        BenchmarkResult result =
                sast(
                        sastFinding("index.php", 20).withApplication("one"),
                        sastFinding("index.php", 20).withApplication("one"),
                        sastFinding("index.php", 20).withApplication("two"));
        assertThat(result.getDetected()).isEqualTo(2);
        assertThat(result.getCoverage()).isEqualTo(100.0);
    }

    @Test
    void unlabelledSastFindingInAFileOnlyOneAppHasStaysWithThatApp() throws Exception {
        respond(
                SAST_URL,
                "{\"one\":" + sastRows("a.php", 20) + ",\"two\":" + sastRows("b.php", 5) + "}");
        BenchmarkResult result = sast(sastFinding("a.php", 99));
        assertThat(result.getApplications().get("one").getUnmatched()).isEqualTo(1);
        assertThat(result.getUnmatchedItems().get(0).getApplication()).isEqualTo("one");
    }

    @Test
    void explicitUnknownApplicationCannotMatchKnownApp() throws Exception {
        respond(SAST_URL, "{\"one\":" + sastRows("index.php", 20) + "}");
        BenchmarkResult result = sast(sastFinding("index.php", 20).withApplication("unknown"));
        assertThat(result.getDetected()).isZero();
        assertThat(result.getApplications().get("unknown").getUnmatched()).isEqualTo(1);
    }

    @Test
    void sastFallsBackToConfiguredCsvProvider() throws Exception {
        server.expect(requestTo(SAST_URL)).andRespond(withServerError());
        BenchmarkResult result = sast(sastFinding("Local.java", 10));
        assertThat(result.getGroundTruthFallback()).isTrue();
        assertThat(result.getApplications()).containsOnlyKeys("VulnerableApp");
        assertThat(result.getDetected()).isEqualTo(1);
        server.verify();
    }

    @Test
    void standaloneSastHttpArrayIsSupported() throws Exception {
        respond(SAST_URL, sastRows("Local.java", 10));
        assertThat(sast(sastFinding("Local.java", 10)).getCoverage()).isEqualTo(100.0);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "null",
                "{}",
                "42",
                "{\"app\":[{\"filePath\":\"Java.java\",\"line\":10}]}",
                "{\"app\":[{\"filePath\":\"Java.java\",\"line\":10,\"type\":\" \",\"cwe\":\" \"}]}",
                "{\"app\":null}",
                "{\"app\":{}}",
                "{\"app\":[{}]}",
                "{\"app\":[{\"filePath\":\" \",\"line\":10,\"type\":\"SQL\"}]}",
                "{\"app\":[{\"filePath\":\"Java.java\",\"line\":0,\"type\":\"SQL\"}]}",
                "{\"app\":[{\"filePath\":\"Java.java\",\"line\":-1,\"type\":\"SQL\"}]}",
                "<html>unavailable</html>"
            })
    void malformedSastResponseFallsBackInsteadOfReportingEmptyCoverage(String body)
            throws Exception {
        respond(SAST_URL, body);
        BenchmarkResult result = sast(sastFinding("Local.java", 10));
        assertThat(result.getGroundTruthFallback()).isTrue();
        assertThat(result.getDetected()).isEqualTo(1);
        server.verify();
    }

    @Test
    void emptyApplicationsAreRetainedAndDoNotPreventOtherAppsScoring() throws Exception {
        respond(SAST_URL, "{\"empty\":[],\"java\":" + sastRows("Java.java", 10) + "}");
        BenchmarkResult result = sast(sastFinding("Java.java", 10));
        assertThat(result.getApplications().get("empty").getTotalExpected()).isZero();
        assertThat(result.getCoverage()).isEqualTo(100.0);
    }

    @Test
    void persistedSchemaRetainsPerAppMetricsAndInputApplication(@TempDir Path dir)
            throws Exception {
        respond(
                SAST_URL,
                "{\"java\":"
                        + sastRows("Java.java", 10)
                        + ",\"php\":"
                        + sastRows("index.php", 20)
                        + "}");
        ScannerFindings input =
                mapper.readValue(
                        "{\"tool\":\"Semgrep\",\"scanType\":\"SAST\",\"findings\":[{\"application\":\"java\",\"filePath\":\"Java.java\",\"line\":10,\"cwe\":\"CWE-89\"}]}",
                        ScannerFindings.class);
        BenchmarkResult result = new SastBenchmarkStrategy(provider).compare(input);
        Path target = new BenchmarkResultWriter(mapper, dir.toString()).write(result);
        JsonNode json = mapper.readTree(target.toFile());
        assertThat(json.path("schemaVersion").asInt()).isEqualTo(2);
        assertThat(json.at("/applications/java/detected").asInt()).isEqualTo(1);
        assertThat(json.at("/applications/php/missed").asInt()).isEqualTo(1);
        assertThat(json.at("/missedItems/0/application").asText()).isEqualTo("php");
        BenchmarkResult roundTrip = mapper.readValue(target.toFile(), BenchmarkResult.class);
        assertThat(roundTrip.getApplications().get("java").getCoverage()).isEqualTo(100.0);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"app\":[{}]}",
                "{\"app\":[{\"variant\":\"UNSECURE\",\"url\":\"/x\",\"vulnerabilityTypes\":[null]}]}",
                "{\"app\":[{\"variant\":\"UNSECURE\",\"url\":\"/x\",\"vulnerabilityTypes\":[]}]}"
            })
    void malformedDastRowsTriggerLocalFallback(String body) throws Exception {
        respond(DAST_URL, body);
        respond(LOCAL_URL, dastRows("/VulnerableApp/sql", "BLIND_SQL_INJECTION"));
        BenchmarkResult result = dast(new Finding("/sql", "BLIND_SQL_INJECTION"));
        assertThat(result.getGroundTruthFallback()).isTrue();
        assertThat(result.getDetected()).isEqualTo(1);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "\"BLIND_SQL_INJECTION\",\" blind_sql_injection \"",
                "\" blind_sql_injection \",\"BLIND_SQL_INJECTION\""
            })
    void dastCaseVariantsOfOneTypeAreOneRowWithItsCwe(String types) throws Exception {
        String body = dastRows("/sql", "TYPES").replace("\"TYPES\"", types);
        respond(DAST_URL, body);
        respond(DAST_URL, body);
        BenchmarkResult byType = dast(new Finding("/sql", "BLIND_SQL_INJECTION"));
        BenchmarkResult byCwe = dast(new Finding("/sql", null, null, null, "CWE-89"));
        assertThat(byType.getTotalExpected()).isEqualTo(1);
        assertThat(byType.getDetected()).isEqualTo(1);
        assertThat(byCwe.getDetected()).isEqualTo(1);
        server.verify();
    }

    @Test
    void dastCaseVariantsOfOneNativeTypeAreOneRow() throws Exception {
        respond(
                DAST_URL,
                dastRows("/sql", "TYPES")
                        .replace("\"TYPES\"", "\"native-php-type\",\"NATIVE-PHP-TYPE \""));
        BenchmarkResult result = dast(new Finding("/sql", "native-php-type"));
        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getDetected()).isEqualTo(1);
        server.verify();
    }

    @Test
    void secureDastRowWithoutTypesIsStillAccepted() throws Exception {
        respond(
                DAST_URL,
                "{\"VulnerableApp\":"
                        + dastRows("/VulnerableApp/sql", "BLIND_SQL_INJECTION")
                        + ",\"other\":"
                        + dastRows("/other/safe", "TYPES")
                                .replace("UNSECURE", "SECURE")
                                .replace("\"TYPES\"", "")
                        + "}");
        BenchmarkResult result = dast(new Finding("/VulnerableApp/sql", "BLIND_SQL_INJECTION"));
        assertThat(result.getGroundTruthFallback()).isFalse();
        assertThat(result.getDetected()).isEqualTo(1);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/shared", "/VulnerableApp/shared"})
    void secureRouteInAnotherAppKeepsUnlabelledFindingAmbiguous(String path) throws Exception {
        respond(
                DAST_URL,
                "{\"VulnerableApp\":"
                        + dastRows(path, "REFLECTED_XSS")
                        + ",\"other\":"
                        + dastRows(path, "REFLECTED_XSS").replace("UNSECURE", "SECURE")
                        + "}");
        BenchmarkResult result = dast(new Finding(path, "REFLECTED_XSS"));
        assertThat(result.getTotalExpected()).isEqualTo(1);
        assertThat(result.getDetected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getUnmatchedItems().get(0).getApplication()).isNull();
        assertThat(result.getApplications().get("other").getTotalExpected()).isZero();
        server.verify();
    }

    @Test
    void explicitApplicationStillMatchesSharedRouteWithoutCreditingSecureApp() throws Exception {
        respond(
                DAST_URL,
                "{\"VulnerableApp\":"
                        + dastRows("/shared", "REFLECTED_XSS")
                        + ",\"other\":"
                        + dastRows("/shared", "REFLECTED_XSS").replace("UNSECURE", "SECURE")
                        + "}");
        BenchmarkResult result =
                dast(
                        new Finding("/shared", "REFLECTED_XSS").withApplication("VulnerableApp"),
                        new Finding("/shared", "REFLECTED_XSS").withApplication("other"));
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getApplications().get("VulnerableApp").getCoverage()).isEqualTo(100.0);
        assertThat(result.getApplications().get("other").getDetected()).isZero();
        assertThat(result.getUnmatchedItems())
                .extracting(Finding::getApplication)
                .containsExactly("other");
        server.verify();
    }

    @Test
    void distinctFullRoutesAreNotConfusedByContextPathNormalization() throws Exception {
        respond(
                DAST_URL,
                "{\"VulnerableApp\":"
                        + dastRows("/VulnerableApp/shared", "REFLECTED_XSS")
                        + ",\"other\":"
                        + dastRows("/shared", "REFLECTED_XSS").replace("UNSECURE", "SECURE")
                        + "}");
        BenchmarkResult result =
                dast(
                        new Finding("/VulnerableApp/shared", "REFLECTED_XSS"),
                        new Finding("/shared", "REFLECTED_XSS"));
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getApplications().get("VulnerableApp").getCoverage()).isEqualTo(100.0);
        assertThat(result.getUnmatchedItems().get(0).getApplication()).isNull();
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/VulnerableApp", "/custom"})
    void unknownRouteUnderAppContextCannotMatchAnotherAppsAlias(String context) throws Exception {
        respond(
                DAST_URL,
                "{\"VulnerableApp\":"
                        + dastRows(context + "/sql1", "REFLECTED_XSS")
                        + ",\"other\":"
                        + dastRows("/sql2", "REFLECTED_XSS")
                        + "}");
        BenchmarkResult result =
                new DastBenchmarkStrategy(provider, context)
                        .compare(
                                new ScannerFindings(
                                        "ZAP",
                                        Collections.singletonList(
                                                new Finding(context + "/sql2", "REFLECTED_XSS"))));
        assertThat(result.getDetected()).isZero();
        assertThat(result.getUnmatched()).isEqualTo(1);
        assertThat(result.getApplications().get("other").getDetected()).isZero();
        server.verify();
    }

    @Test
    void blankDastUrlUsesLocalDastInsteadOfSastGroundTruth() throws Exception {
        respond(LOCAL_URL, dastRows("/VulnerableApp/sql", "BLIND_SQL_INJECTION"));
        BenchmarkGroundTruthProvider blank =
                new BenchmarkGroundTruthProvider(
                        rest,
                        () -> {
                            throw new AssertionError("DAST must not load the SAST CSV");
                        },
                        "",
                        LOCAL_URL,
                        SAST_URL,
                        "VulnerableApp");
        BenchmarkResult result =
                new DastBenchmarkStrategy(blank, "/VulnerableApp")
                        .compare(
                                new ScannerFindings(
                                        "ZAP",
                                        Collections.singletonList(
                                                new Finding("/sql", "BLIND_SQL_INJECTION"))));
        assertThat(result.getDetected()).isEqualTo(1);
        assertThat(result.getGroundTruthFallback()).isTrue();
        server.verify();
    }

    private void respond(String url, String body) {
        server.expect(requestTo(url)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private BenchmarkResult dast(Finding... findings) throws IOException {
        return new DastBenchmarkStrategy(provider, "/VulnerableApp")
                .compare(new ScannerFindings("ZAP", Arrays.asList(findings)));
    }

    private BenchmarkResult sast(Finding... findings) throws IOException {
        return new SastBenchmarkStrategy(provider)
                .compare(new ScannerFindings("Semgrep", ScanType.SAST, Arrays.asList(findings)));
    }

    private static Finding sastFinding(String file, int line) {
        return new Finding(null, "SQL", file, line, "CWE-89");
    }

    private static String dastRows(String url, String type) {
        return "[{\"url\":\""
                + url
                + "\",\"variant\":\"UNSECURE\",\"method\":\"GET\",\"vulnerabilityTypes\":[\""
                + type
                + "\"]}]";
    }

    private static String sastRows(String path, int line) {
        return "[{\"filePath\":\""
                + path
                + "\",\"line\":"
                + line
                + ",\"type\":\"SQL\",\"cwe\":\"CWE-89\",\"numberOfSources\":1}]";
    }
}
