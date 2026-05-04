package org.sasanlabs.benchmark.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.Finding;

class BenchmarkResultWriterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void write_persistsResultToToolNamedFileInDefaultDir(@TempDir Path tempDir) throws Exception {
        BenchmarkResultWriter writer = new BenchmarkResultWriter(tempDir.toString());

        Path target = writer.write(sampleResult("ZAP"));

        assertThat(target).isEqualTo(tempDir.resolve("zap-results.json"));
        assertThat(Files.exists(target)).isTrue();

        BenchmarkResult roundTripped = MAPPER.readValue(target.toFile(), BenchmarkResult.class);
        assertThat(roundTripped.getTool()).isEqualTo("ZAP");
        assertThat(roundTripped.getCoverage()).isEqualTo(50.0);
        assertThat(roundTripped.getTotalExpected()).isEqualTo(2);
        assertThat(roundTripped.getDetected()).isEqualTo(1);
        assertThat(roundTripped.getMissedItems())
                .extracting(Finding::getUrl)
                .containsExactly("/SQLInjection/LEVEL_2");
    }

    @Test
    void write_overwritesExistingFileForSameTool(@TempDir Path tempDir) throws Exception {
        BenchmarkResultWriter writer = new BenchmarkResultWriter(tempDir.toString());
        writer.write(sampleResult("ZAP"));

        BenchmarkResult second =
                new BenchmarkResult(
                        "ZAP", 100.0, 1, 1, 0, 0, Collections.emptyList(), Collections.emptyList());
        Path target = writer.write(second);

        try (java.util.stream.Stream<Path> entries = Files.list(tempDir)) {
            assertThat(entries).hasSize(1);
        }
        BenchmarkResult roundTripped = MAPPER.readValue(target.toFile(), BenchmarkResult.class);
        assertThat(roundTripped.getCoverage()).isEqualTo(100.0);
        assertThat(roundTripped.getMissed()).isZero();
    }

    @Test
    void write_createsBenchmarksDirectoryIfMissing(@TempDir Path tempDir) throws Exception {
        Path nested = tempDir.resolve("does/not/exist/yet");
        BenchmarkResultWriter writer = new BenchmarkResultWriter(nested.toString());

        Path target = writer.write(sampleResult("ZAP"));

        assertThat(Files.isDirectory(nested)).isTrue();
        assertThat(target).isEqualTo(nested.resolve("zap-results.json"));
    }

    @Test
    void write_withCustomDir_overridesDefault(@TempDir Path tempDir) throws Exception {
        Path defaultDir = tempDir.resolve("default");
        Path overrideDir = tempDir.resolve("override");
        BenchmarkResultWriter writer = new BenchmarkResultWriter(defaultDir.toString());

        Path target = writer.write(sampleResult("ZAP"), overrideDir.toString());

        assertThat(target).isEqualTo(overrideDir.resolve("zap-results.json"));
        assertThat(Files.exists(target)).isTrue();
        assertThat(Files.exists(defaultDir)).isFalse();
    }

    @Test
    void write_withWhitespaceAndUppercaseTool_sanitisesFileName(@TempDir Path tempDir)
            throws Exception {
        BenchmarkResultWriter writer = new BenchmarkResultWriter(tempDir.toString());

        Path target = writer.write(sampleResult("  Burp Suite 2.14  "));

        assertThat(target.getFileName().toString()).isEqualTo("burpsuite214-results.json");
    }

    @Test
    void sanitizeToolName_handlesNullEmptyAndAllSymbols() {
        assertThat(BenchmarkResultWriter.sanitizeToolName(null)).isEqualTo("unknown");
        assertThat(BenchmarkResultWriter.sanitizeToolName("")).isEqualTo("unknown");
        assertThat(BenchmarkResultWriter.sanitizeToolName("   ")).isEqualTo("unknown");
        assertThat(BenchmarkResultWriter.sanitizeToolName("***!!!")).isEqualTo("unknown");
    }

    @Test
    void sanitizeToolName_lowercasesAndKeepsAlphanumericUnderscoreAndHyphen() {
        assertThat(BenchmarkResultWriter.sanitizeToolName("ZAP")).isEqualTo("zap");
        assertThat(BenchmarkResultWriter.sanitizeToolName("OWASP-ZAP_v2"))
                .isEqualTo("owasp-zap_v2");
        assertThat(BenchmarkResultWriter.sanitizeToolName("../etc/passwd")).isEqualTo("etcpasswd");
    }

    private static BenchmarkResult sampleResult(String tool) {
        return new BenchmarkResult(
                tool,
                50.0,
                2,
                1,
                1,
                0,
                Arrays.asList(new Finding("/SQLInjection/LEVEL_2", "BLIND_SQL_INJECTION")),
                Collections.emptyList());
    }
}
