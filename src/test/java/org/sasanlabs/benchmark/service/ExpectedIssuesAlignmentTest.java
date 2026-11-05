package org.sasanlabs.benchmark.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sasanlabs.benchmark.model.ExpectedIssue;

/**
 * Guards the checked-in SAST ground truth against line drift. Every row of {@code
 * expectedIssues.csv} has to point at a line of code in an existing source file. Editing a
 * vulnerable class usually moves its rows, and a row that ends up on a blank line, a comment, an
 * annotation or a closing brace can never be matched by a scanner. An annotation includes every
 * line its parentheses span, such as the {@code description = ...} lines of a multi-line
 * {@code @AttackVector}. A row whose {@code LineCount} is above 1 also has to end on a line of code
 * inside the file.
 */
class ExpectedIssuesAlignmentTest {

    private static final List<String> ANNOTATED_METHOD =
            Arrays.asList(
                    "    @AttackVector(",
                    "            vulnerabilityExposed = VulnerabilityType.REFLECTED_XSS,",
                    "            description = \"PAYLOAD (NOT CLOSED\")",
                    "    public ResponseEntity<String> level1(",
                    "            @RequestParam Map<String, String> queryParams) {",
                    "        return new ResponseEntity<>(payload, HttpStatus.OK);",
                    "    }",
                    "",
                    "    @AttackVector(",
                    "            vulnerabilityExposed = VulnerabilityType.REFLECTED_XSS,",
                    "            description = \"PAYLOAD\")");

    @Test
    void everyRowPointsAtALineOfCode() throws IOException {
        List<ExpectedIssue> issues =
                new CsvExpectedIssuesProvider("classpath:scanner/sast/expectedIssues.csv")
                        .getExpectedIssues();
        assertThat(issues).isNotEmpty();

        List<String> misaligned = new ArrayList<>();
        for (ExpectedIssue issue : issues) {
            Path source = Paths.get(issue.getFilePath());
            if (!Files.isRegularFile(source)) {
                misaligned.add(issue.getFilePath() + ":" + issue.getLine() + " (file not found)");
                continue;
            }
            String problem = misalignment(issue, Files.readAllLines(source));
            if (problem != null) {
                misaligned.add(problem);
            }
        }

        assertThat(misaligned)
                .as("expectedIssues.csv rows that do not point at a line of code")
                .isEmpty();
    }

    @Test
    void rowInsideAMultiLineAnnotation_isReported() {
        assertThat(misalignment(row(2, 1), ANNOTATED_METHOD)).isNotNull();
        assertThat(misalignment(row(3, 1), ANNOTATED_METHOD)).isNotNull();
        assertThat(misalignment(row(3, 2), ANNOTATED_METHOD)).isNotNull();
    }

    @Test
    void rangeEndingInsideAMultiLineAnnotation_isReported() {
        assertThat(misalignment(row(6, 5), ANNOTATED_METHOD)).contains("ends on line 10");
    }

    @Test
    void codeAfterAnAnnotationWithAParenthesisInAString_isNotReported() {
        assertThat(misalignment(row(4, 1), ANNOTATED_METHOD)).isNull();
        assertThat(misalignment(row(6, 1), ANNOTATED_METHOD)).isNull();
    }

    @Test
    void parenthesisInAComment_doesNotEndTheAnnotation() {
        List<String> lines =
                Arrays.asList(
                        "    @AttackVector(",
                        "            // the payload ends with )",
                        "            description = \"PAYLOAD\")",
                        "    public void level1() {");

        assertThat(misalignment(row(3, 1), lines)).isNotNull();
        assertThat(misalignment(row(4, 1), lines)).isNull();
    }

    @Test
    void doubleSlashInAString_isNotAComment() {
        List<String> lines =
                Arrays.asList(
                        "    @AttackVector(",
                        "            description = \"http://example\")",
                        "    public void level1() {");

        assertThat(misalignment(row(3, 1), lines)).isNull();
    }

    @Test
    void lineCountTooLargeForAnInt_isReportedAsRunningPastTheFile() {
        assertThat(misalignment(row(4, Integer.MAX_VALUE), ANNOTATED_METHOD))
                .contains("runs past the file's");
    }

    /** Why {@code issue} does not point at a line of code in {@code lines}, or null if it does. */
    private static String misalignment(ExpectedIssue issue, List<String> lines) {
        String row = issue.getFilePath() + ":" + issue.getLine();
        if (issue.getLine() < 1 || issue.getLine() > lines.size()) {
            return row + " (file has " + lines.size() + " lines)";
        }
        Set<Integer> annotationLines = annotationLines(lines);
        String code = lines.get(issue.getLine() - 1).trim();
        if (!isCode(code) || annotationLines.contains(issue.getLine())) {
            return row + " -> \"" + code + "\"";
        }
        long lastLine = (long) issue.getLine() + issue.getLineCount() - 1;
        if (lastLine > lines.size()) {
            return row
                    + " (LineCount "
                    + issue.getLineCount()
                    + " runs past the file's "
                    + lines.size()
                    + " lines)";
        }
        String lastCode = lines.get((int) lastLine - 1).trim();
        if (!isCode(lastCode) || annotationLines.contains((int) lastLine)) {
            return row + " (LineCount ends on line " + lastLine + ": \"" + lastCode + "\")";
        }
        return null;
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

    /**
     * The 1-based numbers of the lines that belong to an annotation: each line that starts with
     * {@code @}, and the lines that follow it until the parentheses it opens are closed.
     */
    private static Set<Integer> annotationLines(List<String> lines) {
        Set<Integer> result = new HashSet<>();
        int depth = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (depth == 0 && !line.startsWith("@")) {
                continue;
            }
            result.add(i + 1);
            depth = Math.max(0, depth + parenthesisBalance(line));
        }
        return result;
    }

    /**
     * Opening minus closing parentheses on {@code line}, outside string and char literals and
     * before a {@code //} comment.
     */
    private static int parenthesisBalance(String line) {
        int balance = 0;
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                break;
            } else if (c == '(') {
                balance++;
            } else if (c == ')') {
                balance--;
            }
        }
        return balance;
    }

    private static ExpectedIssue row(int line, int lineCount) {
        return new ExpectedIssue("CWE-79", "Reflected XSS", "Foo.java", line, 1, lineCount);
    }
}
