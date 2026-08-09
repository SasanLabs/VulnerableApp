package org.sasanlabs.benchmark.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Loads SAST ground truth from a CSV file with header columns {@code CWE}, {@code Vulnerability
 * Type}, {@code File}, {@code Line}, {@code Number of Sources}. The default path points at {@code
 * scanner/sast/expectedIssues.csv} on the classpath; a filesystem path (relative to the working
 * directory, or absolute) is still accepted, and any path may be overridden via the {@code
 * benchmark.sast.ground-truth.path} property.
 *
 * <p>Rows that fail to parse (missing columns, non-integer line / sources) are logged and skipped —
 * one bad row should not abort an entire benchmark run.
 *
 * <p>The CSV is parsed once and cached, so repeated reads (the benchmark flow, and the {@code
 * /scanner/sast} endpoint) do not hit disk or re-parse on every call. The cache is warmed at
 * startup so a missing or malformed file fails fast instead of on first use.
 */
@Component
public class CsvExpectedIssuesProvider implements IExpectedIssuesProvider {

    private static final Logger LOGGER = LogManager.getLogger(CsvExpectedIssuesProvider.class);

    private static final String CLASSPATH_PREFIX = "classpath:";

    private static final String COL_CWE = "CWE";
    private static final String COL_TYPE = "Vulnerability Type";
    private static final String COL_FILE = "File";
    private static final String COL_LINE = "Line";
    private static final String COL_SOURCES = "Number of Sources";

    private static final CSVFormat FORMAT =
            CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();

    private final String csvPath;

    private volatile List<ExpectedIssue> cached;

    public CsvExpectedIssuesProvider(
            @Value("${benchmark.sast.ground-truth.path:classpath:scanner/sast/expectedIssues.csv}")
                    String csvPath) {
        this.csvPath = csvPath;
    }

    @PostConstruct
    void cacheAtStartup() throws IOException {
        getExpectedIssues();
    }

    @Override
    public List<ExpectedIssue> getExpectedIssues() throws IOException {
        List<ExpectedIssue> local = cached;
        if (local == null) {
            synchronized (this) {
                local = cached;
                if (local == null) {
                    local = Collections.unmodifiableList(parse());
                    cached = local;
                }
            }
        }
        return local;
    }

    private List<ExpectedIssue> parse() throws IOException {
        if (csvPath.startsWith(CLASSPATH_PREFIX)) {
            return parseFromResource(csvPath.substring(CLASSPATH_PREFIX.length()));
        }
        return parseFromPath(Paths.get(csvPath));
    }

    private List<ExpectedIssue> parseFromResource(String location) throws IOException {
        try (InputStream in = new ClassPathResource(location).getInputStream();
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                CSVParser parser = FORMAT.parse(reader)) {
            return parseAll(parser);
        }
    }

    private List<ExpectedIssue> parseFromPath(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                CSVParser parser = FORMAT.parse(reader)) {
            return parseAll(parser);
        }
    }

    private List<ExpectedIssue> parseAll(CSVParser parser) {
        List<ExpectedIssue> issues = new ArrayList<>();
        for (CSVRecord row : parser) {
            ExpectedIssue issue = parseRow(row);
            if (issue != null) {
                issues.add(issue);
            }
        }
        LOGGER.info("Loaded {} expected SAST issues from {}", issues.size(), csvPath);
        return issues;
    }

    private ExpectedIssue parseRow(CSVRecord row) {
        if (!row.isSet(COL_CWE)
                || !row.isSet(COL_TYPE)
                || !row.isSet(COL_FILE)
                || !row.isSet(COL_LINE)
                || !row.isSet(COL_SOURCES)) {
            LOGGER.warn(
                    "Skipping malformed SAST CSV row at line {} of {}: missing required columns",
                    row.getRecordNumber(),
                    csvPath);
            return null;
        }
        try {
            return new ExpectedIssue(
                    row.get(COL_CWE),
                    row.get(COL_TYPE),
                    row.get(COL_FILE),
                    Integer.parseInt(row.get(COL_LINE)),
                    Integer.parseInt(row.get(COL_SOURCES)));
        } catch (NumberFormatException nfe) {
            LOGGER.warn(
                    "Skipping SAST CSV row at line {} of {}: non-integer line or sources column"
                            + " ({})",
                    row.getRecordNumber(),
                    csvPath,
                    nfe.getMessage());
            return null;
        }
    }
}
