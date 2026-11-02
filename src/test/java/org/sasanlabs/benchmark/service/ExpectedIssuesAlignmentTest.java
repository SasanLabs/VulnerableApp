package org.sasanlabs.benchmark.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sasanlabs.benchmark.model.ExpectedIssue;

/**
 * Guards the checked-in SAST ground truth against line drift. Every row of {@code
 * expectedIssues.csv} has to point at a line of code in an existing source file. Editing a
 * vulnerable class usually moves its rows, and a row that ends up on a blank line, a comment, an
 * annotation or a closing brace can never be matched by a scanner. A row whose {@code LineCount} is
 * above 1 also has to end on a line of code inside the file.
 */
class ExpectedIssuesAlignmentTest {

    @Test
    void everyRowPointsAtALineOfCode() throws IOException {
        List<ExpectedIssue> issues =
                new CsvExpectedIssuesProvider("classpath:scanner/sast/expectedIssues.csv")
                        .getExpectedIssues();
        assertThat(issues).isNotEmpty();

        List<String> misaligned = new ArrayList<>();
        for (ExpectedIssue issue : issues) {
            String row = issue.getFilePath() + ":" + issue.getLine();
            Path source = Paths.get(issue.getFilePath());
            if (!Files.isRegularFile(source)) {
                misaligned.add(row + " (file not found)");
                continue;
            }
            List<String> lines = Files.readAllLines(source);
            if (issue.getLine() < 1 || issue.getLine() > lines.size()) {
                misaligned.add(row + " (file has " + lines.size() + " lines)");
                continue;
            }
            String code = lines.get(issue.getLine() - 1).trim();
            if (!isCode(code)) {
                misaligned.add(row + " -> \"" + code + "\"");
                continue;
            }
            int lastLine = issue.getLine() + issue.getLineCount() - 1;
            if (lastLine > lines.size()) {
                misaligned.add(
                        row
                                + " (LineCount "
                                + issue.getLineCount()
                                + " runs past the file's "
                                + lines.size()
                                + " lines)");
                continue;
            }
            String lastCode = lines.get(lastLine - 1).trim();
            if (!isCode(lastCode)) {
                misaligned.add(
                        row + " (LineCount ends on line " + lastLine + ": \"" + lastCode + "\")");
            }
        }

        assertThat(misaligned)
                .as("expectedIssues.csv rows that do not point at a line of code")
                .isEmpty();
    }

    private static boolean isCode(String line) {
        if (line.startsWith("//")
                || line.startsWith("/*")
                || line.startsWith("*")
                || line.startsWith("@")) {
            return false;
        }
        return line.chars().anyMatch(Character::isLetterOrDigit);
    }
}
