package org.sasanlabs.benchmark.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Compares scanner findings against VulnerableApp's SAST ground truth (the {@link
 * IExpectedIssuesProvider}, backed by {@code scanner/sast/expectedIssues.csv} by default) and
 * produces coverage / missed / unmatched metrics.
 *
 * <p>Each ground-truth row is indexed under up to two keys: its file with its CWE, and its file
 * with its vulnerability-type label. A scanner finding matches a row if it shares the {@code
 * filePath} and the CWE, or the type label (case-insensitive) when the finding or the row has no
 * CWE, AND its {@code line} is between the row's {@code line} and {@code line + lineCount - 1}. A
 * finding whose CWE differs from the row's does not match on its type. A finding inside the ranges
 * of two rows detects both. Scanner findings are expected to emit project-relative paths;
 * backslashes and leading {@code ./} are normalized.
 */
@Service
public class SastBenchmarkStrategy implements BenchmarkStrategy {

    private static final Logger LOGGER = LogManager.getLogger(SastBenchmarkStrategy.class);

    private static final String KEY_SEPARATOR = "::";
    private static final String CWE_TAG = "CWE";
    private static final String TYPE_TAG = "TYPE";

    private final IExpectedIssuesProvider expectedIssuesProvider;

    public SastBenchmarkStrategy(IExpectedIssuesProvider expectedIssuesProvider) {
        this.expectedIssuesProvider = expectedIssuesProvider;
    }

    /**
     * Whether {@code line} is one of the row's lines, {@code line} to {@code line + lineCount - 1},
     * compared without building that range so a large count needs no extra memory and cannot
     * overflow.
     */
    private static boolean covers(ExpectedIssue ei, int line) {
        return line >= ei.line() && (long) line - ei.line() < ei.lineCount();
    }

    private static boolean isLocated(Finding f) {
        return f != null && f.getFilePath() != null && f.getLine() != null;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * The key a finding is matched and de-duplicated by: its file with its CWE, or with its type
     * when it has no CWE. {@code null} when it has neither.
     */
    private static String identityKey(Finding f) {
        if (hasText(f.getCwe())) {
            return buildKeys(f.getFilePath(), f.getCwe(), null).get(0);
        }
        if (hasText(f.getType())) {
            return typeKey(f.getFilePath(), f.getType());
        }
        return null;
    }

    /**
     * The rows a finding can match: the rows under its identity key and, for a finding with a CWE,
     * the rows with its type that have no CWE to compare.
     */
    private static List<ExpectedIssue> candidateRows(
            Finding f, Map<String, List<ExpectedIssue>> expectedByKey) {
        List<ExpectedIssue> rows =
                new ArrayList<>(
                        expectedByKey.getOrDefault(identityKey(f), Collections.emptyList()));
        if (hasText(f.getCwe()) && hasText(f.getType())) {
            for (ExpectedIssue ei :
                    expectedByKey.getOrDefault(
                            typeKey(f.getFilePath(), f.getType()), Collections.emptyList())) {
                if (!hasText(ei.cwe())) {
                    rows.add(ei);
                }
            }
        }
        return rows;
    }

    private static String typeKey(String filePath, String type) {
        return buildKeys(filePath, null, type).get(0);
    }

    /** The file's CWE and type keys, without a line. */
    private static List<String> buildKeys(String filePath, String cwe, String type) {
        if (filePath == null) {
            return new ArrayList<>();
        }
        String path = normalizeFilePath(filePath);
        String prefix = path + KEY_SEPARATOR;
        List<String> keys = new ArrayList<>(2);
        if (cwe != null && !cwe.trim().isEmpty()) {
            keys.add(prefix + CWE_TAG + KEY_SEPARATOR + cwe.trim().toUpperCase(Locale.ROOT));
        }
        if (type != null && !type.trim().isEmpty()) {
            keys.add(prefix + TYPE_TAG + KEY_SEPARATOR + type.trim().toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    /**
     * Normalises a SAST file path to a project-relative form: backslashes → forward slashes,
     * leading {@code ./} stripped, surrounding whitespace trimmed. Scanner authors are expected to
     * emit project-root-relative paths; absolute paths or unexpected prefixes will not match.
     */
    static String normalizeFilePath(String path) {
        if (path == null) {
            return "";
        }
        String s = path.trim().replace('\\', '/');
        while (s.startsWith("./")) {
            s = s.substring(2);
        }
        return s;
    }

    private static Finding toFinding(ExpectedIssue ei) {
        return new Finding(
                null,
                ei.vulnerabilityType(),
                normalizeFilePath(ei.filePath()),
                ei.line(),
                ei.cwe());
    }

    @Override
    public BenchmarkResult compare(ScannerFindings input) throws IOException {
        List<ExpectedIssue> expected = expectedIssuesProvider.getExpectedIssues();

        Map<String, List<ExpectedIssue>> expectedByKey = new HashMap<>();
        for (ExpectedIssue ei : expected) {
            for (String key : buildKeys(ei.filePath(), ei.cwe(), ei.vulnerabilityType())) {
                expectedByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(ei);
            }
        }

        List<Finding> rawFindings =
                (input.getFindings() != null) ? input.getFindings() : new ArrayList<>();

        // A finding without a CWE repeats a finding with one that has the same file, line and type,
        // whichever of the two comes first.
        Set<String> typesOfFindingsWithACwe = new HashSet<>();
        for (Finding f : rawFindings) {
            if (isLocated(f) && hasText(f.getCwe()) && hasText(f.getType())) {
                typesOfFindingsWithACwe.add(
                        f.getLine() + KEY_SEPARATOR + typeKey(f.getFilePath(), f.getType()));
            }
        }

        // Every finding is matched, so the result does not depend on their order. Findings with
        // the same file, line and CWE, or the same type when they have no CWE, form one group,
        // which is listed once as unmatched when none of its findings matches a row.
        Map<String, Finding> firstFindingOfGroup = new LinkedHashMap<>();
        Set<String> matchedGroups = new HashSet<>();
        Set<ExpectedIssue> detectedSet = new HashSet<>();
        for (Finding f : rawFindings) {
            if (!isLocated(f)) {
                continue;
            }
            String key = identityKey(f);
            if (key == null) {
                continue;
            }
            String lineKey = f.getLine() + KEY_SEPARATOR + key;
            if (!hasText(f.getCwe()) && typesOfFindingsWithACwe.contains(lineKey)) {
                continue;
            }
            firstFindingOfGroup.putIfAbsent(lineKey, f);
            for (ExpectedIssue ei : candidateRows(f, expectedByKey)) {
                if (covers(ei, f.getLine())) {
                    detectedSet.add(ei);
                    matchedGroups.add(lineKey);
                }
            }
        }

        List<Finding> unmatchedItems = new ArrayList<>();
        for (Map.Entry<String, Finding> group : firstFindingOfGroup.entrySet()) {
            if (!matchedGroups.contains(group.getKey())) {
                unmatchedItems.add(group.getValue());
            }
        }

        List<Finding> detectedItems = new ArrayList<>();
        List<Finding> missedItems = new ArrayList<>();
        Set<ExpectedIssue> listedAsDetected = new HashSet<>();
        for (ExpectedIssue ei : expected) {
            if (!detectedSet.contains(ei)) {
                missedItems.add(toFinding(ei));
            } else if (listedAsDetected.add(ei)) {
                // Listed once, as detected counts it once
                detectedItems.add(toFinding(ei));
            }
        }

        int totalExpected = expected.size();
        int detected = detectedSet.size();
        double coverage;
        if (totalExpected == 0) {
            LOGGER.warn(
                    "SAST ground truth is empty; coverage cannot be computed and will be reported"
                            + " as 0.0");
            coverage = 0.0;
        } else {
            coverage = detected * 100.0 / totalExpected;
        }

        return new BenchmarkResult(
                input.getTool(),
                coverage,
                totalExpected,
                detected,
                missedItems.size(),
                unmatchedItems.size(),
                detectedItems,
                missedItems,
                unmatchedItems);
    }
}
