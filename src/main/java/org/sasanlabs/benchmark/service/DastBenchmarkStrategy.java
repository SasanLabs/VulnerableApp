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
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScanType;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Scores DAST findings by URL, method, and any matching type, CWE, or WASC axis. */
@Service
public class DastBenchmarkStrategy implements BenchmarkStrategy {

    private static final Logger LOGGER = LogManager.getLogger(DastBenchmarkStrategy.class);

    private static final String KEY_SEPARATOR = "::";
    private static final String AXIS_TYPE = "TYPE";
    private static final String AXIS_CWE = "CWE";
    private static final String AXIS_WASC = "WASC";
    private static final String METHOD_ANY = "ANY";

    private final BenchmarkGroundTruthProvider groundTruthProvider;
    private final String contextPath;

    public DastBenchmarkStrategy(
            BenchmarkGroundTruthProvider groundTruthProvider,
            @Value("${server.servlet.context-path:}") String contextPath) {
        this.groundTruthProvider = groundTruthProvider;
        this.contextPath = contextPath;
    }

    @Override
    public BenchmarkResult compare(ScannerFindings input) throws IOException {
        return ApplicationBenchmark.compare(
                input, groundTruthProvider.load(ScanType.DAST), contextPath, this::score);
    }

    private BenchmarkResult score(ScannerFindings input, List<Finding> groundTruth) {

        List<Finding> expectedRows = new ArrayList<>();
        Map<String, Set<Integer>> keyToExpected = buildExpectedIndex(groundTruth, expectedRows);

        Set<Integer> detectedIndices = new LinkedHashSet<>();
        List<Finding> unmatchedItems = new ArrayList<>();
        Set<String> seenFindingSignatures = new HashSet<>();

        List<Finding> rawFindings =
                (input.getFindings() != null) ? input.getFindings() : new ArrayList<>();
        for (Finding finding : rawFindings) {
            if (finding == null || finding.getUrl() == null) {
                continue;
            }
            String url = normalizeUrl(finding.getUrl(), contextPath);
            List<String> keys = keysForFinding(url, finding);
            if (keys.isEmpty()) {
                continue;
            }
            String signature = String.join("|", keys);
            if (!seenFindingSignatures.add(signature)) {
                continue;
            }

            Set<Integer> hits = new LinkedHashSet<>();
            for (String key : keys) {
                Set<Integer> rows = keyToExpected.get(key);
                if (rows != null) {
                    hits.addAll(rows);
                }
            }
            if (hits.isEmpty()) {
                unmatchedItems.add(
                        new Finding(
                                url,
                                finding.getType(),
                                null,
                                null,
                                finding.getCwe(),
                                finding.getWascId(),
                                finding.getMethod()));
            } else {
                detectedIndices.addAll(hits);
            }
        }

        List<Finding> missedItems = new ArrayList<>();
        for (int i = 0; i < expectedRows.size(); i++) {
            if (!detectedIndices.contains(i)) {
                missedItems.add(expectedRows.get(i));
            }
        }

        int totalExpected = expectedRows.size();
        int detected = detectedIndices.size();
        double coverage;
        if (totalExpected == 0) {
            LOGGER.warn(
                    "Ground truth is empty; coverage cannot be computed and will be reported as"
                            + " 0.0");
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
                missedItems,
                unmatchedItems);
    }

    private Map<String, Set<Integer>> buildExpectedIndex(
            List<Finding> groundTruth, List<Finding> rowsOut) {
        Map<String, Set<Integer>> keyToRows = new LinkedHashMap<>();
        Set<String> seenRowSignatures = new HashSet<>();
        for (Finding entry : groundTruth) {
            String url = normalizeUrl(entry.getUrl(), contextPath);
            String method =
                    entry.getMethod() == null
                            ? METHOD_ANY
                            : entry.getMethod().trim().toUpperCase(Locale.ROOT);
            String rowSignature = url + KEY_SEPARATOR + method + KEY_SEPARATOR + entry.getType();
            if (!seenRowSignatures.add(rowSignature)) {
                continue;
            }
            int rowIndex = rowsOut.size();
            Finding row =
                    new Finding(
                            url,
                            entry.getType(),
                            null,
                            null,
                            entry.getCwe(),
                            entry.getWascId(),
                            method);
            rowsOut.add(row);
            for (String key : keysForFinding(url, row)) {
                addKey(keyToRows, key, rowIndex);
            }
            Finding anyMethod =
                    new Finding(
                            url,
                            entry.getType(),
                            null,
                            null,
                            entry.getCwe(),
                            entry.getWascId(),
                            METHOD_ANY);
            for (String key : keysForFinding(url, anyMethod)) {
                addKey(keyToRows, key, rowIndex);
            }
        }
        return keyToRows;
    }

    private static List<String> keysForFinding(String url, Finding finding) {
        String method =
                (finding.getMethod() != null && !finding.getMethod().trim().isEmpty())
                        ? finding.getMethod().trim().toUpperCase(Locale.ROOT)
                        : METHOD_ANY;
        List<String> keys = new ArrayList<>(3);
        if (finding.getType() != null && !finding.getType().trim().isEmpty()) {
            keys.add(typeKey(url, method, finding.getType()));
        }
        String cwe = normalizeNumericId(finding.getCwe());
        if (!cwe.isEmpty()) {
            keys.add(cweKey(url, method, cwe));
        }
        String wasc = normalizeNumericId(finding.getWascId());
        if (!wasc.isEmpty()) {
            keys.add(wascKey(url, method, wasc));
        }
        return keys;
    }

    private static void addKey(Map<String, Set<Integer>> map, String key, int rowIndex) {
        map.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(rowIndex);
    }

    private static String typeKey(String url, String method, String typeName) {
        return AXIS_TYPE
                + KEY_SEPARATOR
                + method
                + KEY_SEPARATOR
                + url
                + KEY_SEPARATOR
                + typeName.trim().toUpperCase(Locale.ROOT);
    }

    private static String cweKey(String url, String method, String cweDigits) {
        return AXIS_CWE + KEY_SEPARATOR + method + KEY_SEPARATOR + url + KEY_SEPARATOR + cweDigits;
    }

    private static String wascKey(String url, String method, String wascDigits) {
        return AXIS_WASC
                + KEY_SEPARATOR
                + method
                + KEY_SEPARATOR
                + url
                + KEY_SEPARATOR
                + wascDigits;
    }

    /**
     * Reduces a CWE/WASC string to its canonical digit form so {@code "CWE-89"}, {@code "cwe89"},
     * {@code "89"}, and zero-padded variants like {@code "CWE-089"} all hash the same. Returns the
     * empty string for {@code null} or all-non-digit input.
     */
    static String normalizeNumericId(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "";
        }
        try {
            return Long.toString(Long.parseLong(digits));
        } catch (NumberFormatException nfe) {
            return digits;
        }
    }

    /**
     * Normalises a URL to a context-path-stripped path so absolute ground-truth URLs and relative
     * scanner-finding URLs compare equal.
     *
     * <p>Strips scheme + host + port, the context path the application is served under, the query
     * string, and a trailing slash. Leaves the path case unchanged. An empty or {@code null} {@code
     * contextPath} strips no prefix, leaving a root deployment's paths unchanged.
     */
    static String normalizeUrl(String url, String contextPath) {
        if (url == null) {
            return "";
        }
        String s = url.trim();
        int schemeIdx = s.indexOf("://");
        if (schemeIdx >= 0) {
            int pathIdx = s.indexOf('/', schemeIdx + 3);
            s = (pathIdx >= 0) ? s.substring(pathIdx) : "/";
        }
        int queryIdx = s.indexOf('?');
        if (queryIdx >= 0) {
            s = s.substring(0, queryIdx);
        }
        // for URLs like
        // /VulnerableApp/BlindSQLInjectionVulnerability/LEVEL_1;(SELECT%20*%20FROM%20(SELECT(SLEEP(5)))PpKC)%23"
        int zapSQLPayloadIdx = s.indexOf(';');
        if (zapSQLPayloadIdx >= 0) {
            s = s.substring(0, zapSQLPayloadIdx);
        }

        if (contextPath != null && !contextPath.isEmpty()) {
            if (s.equals(contextPath)) {
                s = "/";
            } else if (s.startsWith(contextPath + "/")) {
                s = s.substring(contextPath.length());
            }
        }
        if (s.length() > 1 && s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.isEmpty()) {
            return "/";
        }
        if (!s.startsWith("/")) {
            s = "/" + s;
        }
        return s;
    }
}
