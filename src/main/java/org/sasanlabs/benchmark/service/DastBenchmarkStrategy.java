package org.sasanlabs.benchmark.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Compares scanner findings against VulnerableApp's DAST ground truth (fetched from the {@code
 * /scanner} HTTP endpoint) and produces coverage / missed / unmatched metrics.
 *
 * <p>The ground-truth source is an HTTP URL rather than an in-process bean so the comparator works
 * unchanged when VulnerableApp is composed behind VulnerableApp-facade with other backing apps —
 * point the property at the facade's aggregated endpoint and coverage reflects the full attack
 * surface.
 *
 * <p>Matching is set-based and tolerant to scanner-specific vocabularies: a scanner finding matches
 * a ground-truth row when the URL agrees AND <em>any one of</em> the type-name, CWE, or WASC axes
 * agrees. Scanners that don't speak {@link VulnerabilityType} names (most of them) can therefore
 * still be graded by emitting a CWE or WASC ID alongside their finding.
 *
 * <p>Only {@code UNSECURE} ground-truth entries count toward expected — a scanner finding against a
 * {@code SECURE} URL is treated as unmatched (out of scope rather than a true false positive, since
 * hardening / SSL-style alerts on a SECURE URL aren't modelled here as vulnerabilities).
 */
@Service
public class DastBenchmarkStrategy implements BenchmarkStrategy {

    private static final Logger LOGGER = LogManager.getLogger(DastBenchmarkStrategy.class);

    private static final String CONTEXT_PATH = "/VulnerableApp";
    private static final String UNSECURE_VARIANT = "UNSECURE";
    private static final String KEY_SEPARATOR = "::";
    private static final String AXIS_TYPE = "TYPE";
    private static final String AXIS_CWE = "CWE";
    private static final String AXIS_WASC = "WASC";
    private static final String METHOD_ANY = "ANY";

    private final RestTemplate restTemplate;
    private final String groundTruthUrl;

    public DastBenchmarkStrategy(
            RestTemplate restTemplate,
            @Value("${benchmark.dast.ground-truth.url}") String groundTruthUrl) {
        this.restTemplate = restTemplate;
        this.groundTruthUrl = groundTruthUrl;
    }

    @Override
    public BenchmarkResult compare(ScannerFindings input) throws IOException {
        List<ScannerResponseBean> groundTruth = fetchGroundTruth();

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
            String url = normalizeUrl(finding.getUrl());
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

    private List<ScannerResponseBean> fetchGroundTruth() throws IOException {
        try {
            ScannerResponseBean[] response =
                    restTemplate.getForObject(groundTruthUrl, ScannerResponseBean[].class);
            return (response == null) ? Collections.emptyList() : Arrays.asList(response);
        } catch (RestClientException rce) {
            throw new IOException("Failed to fetch DAST ground truth from " + groundTruthUrl, rce);
        }
    }

    private Map<String, Set<Integer>> buildExpectedIndex(
            List<ScannerResponseBean> groundTruth, List<Finding> rowsOut) {
        Map<String, Set<Integer>> keyToRows = new LinkedHashMap<>();
        Set<String> seenRowSignatures = new HashSet<>();
        for (ScannerResponseBean entry : groundTruth) {
            if (!UNSECURE_VARIANT.equalsIgnoreCase(entry.getVariant())) {
                continue;
            }
            String url = normalizeUrl(entry.getUrl());
            String method =
                    (entry.getRequestMethod() != null)
                            ? entry.getRequestMethod().name()
                            : METHOD_ANY;
            List<VulnerabilityType> types = entry.getVulnerabilityTypes();
            if (types == null) {
                continue;
            }
            for (VulnerabilityType type : types) {
                String rowSignature = url + KEY_SEPARATOR + method + KEY_SEPARATOR + type.name();
                if (!seenRowSignatures.add(rowSignature)) {
                    continue;
                }
                int rowIndex = rowsOut.size();
                rowsOut.add(buildExpectedFinding(url, type, method));
                addAxisKeys(keyToRows, rowIndex, url, method, type);
                addAxisKeys(keyToRows, rowIndex, url, METHOD_ANY, type);
            }
        }
        return keyToRows;
    }

    private static void addAxisKeys(
            Map<String, Set<Integer>> keyToRows,
            int rowIndex,
            String url,
            String method,
            VulnerabilityType type) {
        addKey(keyToRows, typeKey(url, method, type.name()), rowIndex);
        if (type.getCweID() != null) {
            String cwe = normalizeNumericId(type.getCweID().toString());
            if (!cwe.isEmpty()) {
                addKey(keyToRows, cweKey(url, method, cwe), rowIndex);
            }
        }
        if (type.getWascID() != null) {
            String wasc = normalizeNumericId(type.getWascID().toString());
            if (!wasc.isEmpty()) {
                addKey(keyToRows, wascKey(url, method, wasc), rowIndex);
            }
        }
    }

    private static Finding buildExpectedFinding(String url, VulnerabilityType type, String method) {
        String cwe = (type.getCweID() != null) ? "CWE-" + type.getCweID() : null;
        String wasc = (type.getWascID() != null) ? type.getWascID().toString() : null;
        return new Finding(url, type.name(), null, null, cwe, wasc, method);
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
     * Reduces a CWE/WASC string to its digit run so {@code "CWE-89"}, {@code "cwe89"}, and {@code
     * "89"} all hash the same. Returns the empty string for {@code null} or all-non-digit input.
     */
    static String normalizeNumericId(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("\\D", "");
    }

    /**
     * Normalises a URL to a context-path-stripped path so absolute ground-truth URLs and relative
     * scanner-finding URLs compare equal.
     *
     * <p>Strips scheme + host + port, the {@code /VulnerableApp} context path, the query string,
     * and a trailing slash. Leaves the path case unchanged.
     */
    static String normalizeUrl(String url) {
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
        if (s.equals(CONTEXT_PATH)) {
            s = "/";
        } else if (s.startsWith(CONTEXT_PATH + "/")) {
            s = s.substring(CONTEXT_PATH.length());
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
