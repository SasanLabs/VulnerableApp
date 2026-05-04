package org.sasanlabs.benchmark.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Loads SAST ground truth from a CSV file with columns {@code CWE, Vulnerability Type, File, Line,
 * Number of Sources}. The default path points at {@code scanner/sast/expectedIssues.csv} in the
 * project root; override via the {@code benchmark.sast.ground-truth.path} property.
 *
 * <p>The first line is treated as a header and skipped. Blank lines and rows that fail to parse
 * (wrong column count, non-integer line/sources) are logged and skipped — one bad row should not
 * abort an entire benchmark run.
 */
@Component
public class CsvExpectedIssuesProvider implements IExpectedIssuesProvider {

    private static final Logger LOGGER = LogManager.getLogger(CsvExpectedIssuesProvider.class);

    private static final int EXPECTED_COLUMNS = 5;

    private final String csvPath;

    public CsvExpectedIssuesProvider(
            @Value("${benchmark.sast.ground-truth.path:scanner/sast/expectedIssues.csv}")
                    String csvPath) {
        this.csvPath = csvPath;
    }

    @Override
    public List<ExpectedIssue> getExpectedIssues() throws IOException {
        Path path = Paths.get(csvPath);
        List<ExpectedIssue> issues = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String raw;
            int lineNo = 0;
            while ((raw = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1) {
                    continue;
                }
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] cols = trimmed.split(",", -1);
                if (cols.length < EXPECTED_COLUMNS) {
                    LOGGER.warn(
                            "Skipping malformed SAST CSV row at line {} of {}: expected {}"
                                    + " columns, got {}",
                            lineNo,
                            csvPath,
                            EXPECTED_COLUMNS,
                            cols.length);
                    continue;
                }
                try {
                    issues.add(
                            new ExpectedIssue(
                                    cols[0].trim(),
                                    cols[1].trim(),
                                    cols[2].trim(),
                                    Integer.parseInt(cols[3].trim()),
                                    Integer.parseInt(cols[4].trim())));
                } catch (NumberFormatException nfe) {
                    LOGGER.warn(
                            "Skipping SAST CSV row at line {} of {}: non-integer line or sources"
                                    + " column ({})",
                            lineNo,
                            csvPath,
                            nfe.getMessage());
                }
            }
        }
        LOGGER.info("Loaded {} expected SAST issues from {}", issues.size(), csvPath);
        return issues;
    }
}
