package org.sasanlabs.benchmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sasanlabs.benchmark.model.ExpectedIssue;

class CsvExpectedIssuesProviderTest {

    private static final String HEADER = "CWE,Vulnerability Type,File,Line,Number of Sources\n";

    @Test
    void parsesHeaderAndAllValidRows(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("expected.csv");
        write(
                csv,
                HEADER
                        + "CWE-89,SQL Injection,src/main/java/Foo.java,56,1\n"
                        + "CWE-79,Reflected XSS,src/main/java/Bar.java,42,3\n");

        List<ExpectedIssue> issues =
                new CsvExpectedIssuesProvider(csv.toString()).getExpectedIssues();

        assertThat(issues)
                .extracting(
                        ExpectedIssue::getCwe,
                        ExpectedIssue::getVulnerabilityType,
                        ExpectedIssue::getFilePath,
                        ExpectedIssue::getLine,
                        ExpectedIssue::getNumberOfSources)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                "CWE-89", "SQL Injection", "src/main/java/Foo.java", 56, 1),
                        org.assertj.core.groups.Tuple.tuple(
                                "CWE-79", "Reflected XSS", "src/main/java/Bar.java", 42, 3));
    }

    @Test
    void trimsWhitespaceInsideCells(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("expected.csv");
        write(csv, HEADER + "CWE-22,Path Traversal,src/main/java/Baz.java,65,12 \n");

        List<ExpectedIssue> issues =
                new CsvExpectedIssuesProvider(csv.toString()).getExpectedIssues();

        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getNumberOfSources()).isEqualTo(12);
    }

    @Test
    void skipsBlankLines(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("expected.csv");
        write(
                csv,
                HEADER
                        + "\n"
                        + "CWE-89,SQL Injection,src/main/java/Foo.java,56,1\n"
                        + "   \n"
                        + "CWE-79,Reflected XSS,src/main/java/Bar.java,42,3\n"
                        + "\n");

        List<ExpectedIssue> issues =
                new CsvExpectedIssuesProvider(csv.toString()).getExpectedIssues();

        assertThat(issues).hasSize(2);
    }

    @Test
    void skipsRowsWithTooFewColumns(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("expected.csv");
        write(
                csv,
                HEADER
                        + "CWE-89,SQL Injection,src/main/java/Foo.java,56,1\n"
                        + "CWE-79,Reflected XSS,only,three\n"
                        + "CWE-77,Command Injection,src/main/java/Cmd.java,46,5\n");

        List<ExpectedIssue> issues =
                new CsvExpectedIssuesProvider(csv.toString()).getExpectedIssues();

        assertThat(issues).hasSize(2);
        assertThat(issues).extracting(ExpectedIssue::getCwe).containsExactly("CWE-89", "CWE-77");
    }

    @Test
    void skipsRowsWithNonIntegerLineOrSources(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("expected.csv");
        write(
                csv,
                HEADER
                        + "CWE-89,SQL Injection,src/main/java/Foo.java,not-a-number,1\n"
                        + "CWE-79,Reflected XSS,src/main/java/Bar.java,42,three\n"
                        + "CWE-77,Command Injection,src/main/java/Cmd.java,46,5\n");

        List<ExpectedIssue> issues =
                new CsvExpectedIssuesProvider(csv.toString()).getExpectedIssues();

        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).getCwe()).isEqualTo("CWE-77");
    }

    @Test
    void emptyFileWithOnlyHeader_returnsEmptyList(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("expected.csv");
        write(csv, HEADER);

        List<ExpectedIssue> issues =
                new CsvExpectedIssuesProvider(csv.toString()).getExpectedIssues();

        assertThat(issues).isEmpty();
    }

    @Test
    void missingFile_throwsIOException(@TempDir Path tempDir) {
        String missing = tempDir.resolve("does-not-exist.csv").toString();

        assertThatThrownBy(() -> new CsvExpectedIssuesProvider(missing).getExpectedIssues())
                .isInstanceOf(IOException.class);
    }

    private static void write(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
