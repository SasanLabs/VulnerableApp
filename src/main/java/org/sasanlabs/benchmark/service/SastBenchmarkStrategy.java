package org.sasanlabs.benchmark.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.springframework.stereotype.Service;

/**
 * Compares scanner findings against VulnerableApp's SAST ground truth (the {@link
 * IExpectedIssuesProvider}, backed by {@code scanner/sast/expectedIssues.csv} by default) and
 * produces coverage / missed / false-positive metrics.
 *
 * <p>Each ground-truth row contributes up to two match keys: one keyed on CWE and one keyed on the
 * vulnerability-type label. A scanner finding matches if it shares the {@code (filePath, line)}
 * pair AND either the CWE or the type label (case-insensitive). Scanner findings are expected to
 * emit project-relative paths; backslashes and leading {@code ./} are normalised.
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

    @Override
    public BenchmarkResult compare(ScannerFindings input) throws IOException {
        List<ExpectedIssue> expected = expectedIssuesProvider.getExpectedIssues();

        Map<String, ExpectedIssue> expectedByKey = new LinkedHashMap<>();
        for (ExpectedIssue ei : expected) {
            for (String key : keysFor(ei)) {
                expectedByKey.putIfAbsent(key, ei);
            }
        }

        List<Finding> rawFindings =
                (input.getFindings() != null) ? input.getFindings() : new ArrayList<>();

        Map<String, Finding> foundByKey = new LinkedHashMap<>();
        List<Finding> uniqueFindings = new ArrayList<>();
        Set<String> primaryKeysSeen = new HashSet<>();
        for (Finding f : rawFindings) {
            if (f == null || f.getFilePath() == null || f.getLine() == null) {
                continue;
            }
            List<String> keys = keysFor(f);
            if (keys.isEmpty()) {
                continue;
            }
            String primary = keys.get(0);
            if (!primaryKeysSeen.add(primary)) {
                continue;
            }
            uniqueFindings.add(f);
            for (String key : keys) {
                foundByKey.putIfAbsent(key, f);
            }
        }

        Set<ExpectedIssue> detectedSet = new LinkedHashSet<>();
        List<Finding> missedItems = new ArrayList<>();
        for (ExpectedIssue ei : expected) {
            boolean hit = false;
            for (String key : keysFor(ei)) {
                if (foundByKey.containsKey(key)) {
                    hit = true;
                    break;
                }
            }
            if (hit) {
                detectedSet.add(ei);
            } else {
                missedItems.add(toFinding(ei));
            }
        }

        List<Finding> falsePositiveItems = new ArrayList<>();
        for (Finding f : uniqueFindings) {
            boolean hit = false;
            for (String key : keysFor(f)) {
                if (expectedByKey.containsKey(key)) {
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                falsePositiveItems.add(f);
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
                falsePositiveItems.size(),
                missedItems,
                falsePositiveItems);
    }

    private static List<String> keysFor(ExpectedIssue ei) {
        return buildKeys(ei.getFilePath(), ei.getLine(), ei.getCwe(), ei.getVulnerabilityType());
    }

    private static List<String> keysFor(Finding f) {
        return buildKeys(f.getFilePath(), f.getLine(), f.getCwe(), f.getType());
    }

    private static List<String> buildKeys(String filePath, Integer line, String cwe, String type) {
        if (filePath == null || line == null) {
            return new ArrayList<>();
        }
        String path = normalizeFilePath(filePath);
        String prefix = path + KEY_SEPARATOR + line + KEY_SEPARATOR;
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
                ei.getVulnerabilityType(),
                normalizeFilePath(ei.getFilePath()),
                ei.getLine(),
                ei.getCwe());
    }
}
