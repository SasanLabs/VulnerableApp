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
import org.sasanlabs.internal.utility.Protocol;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Compares scanner findings against VulnerableApp's DAST ground truth (fetched from the {@code
 * /scanner/dast} HTTP endpoint) and produces coverage / missed / unmatched metrics.
 *
 * <p>The ground-truth source is an HTTP URL rather than an in-process bean so the comparator works
 * unchanged when VulnerableApp is composed behind VulnerableApp-facade with other backing apps —
 * point the property at the facade's aggregated endpoint and coverage reflects the full attack
 * surface.
 *
 * <p>Matching is set-based and tolerant to scanner-specific vocabularies: a scanner finding matches
 * a ground-truth row when URL and protocol agree, the HTTP method check passes (ignored for
 * WebSockets), AND <em>any one of</em> the type-name, CWE, or WASC axes agrees. Scanners that don't
 * speak {@link VulnerabilityType} names (most of them) can therefore still be graded by emitting a
 * CWE or WASC ID alongside their finding.
 *
 * <p>Only {@code UNSECURE} ground-truth entries count toward expected — a scanner finding against a
 * {@code SECURE} URL is treated as unmatched (out of scope rather than a true false positive, since
 * hardening / SSL-style alerts on a SECURE URL aren't modelled here as vulnerabilities).
 */
@Service
public class DastBenchmarkStrategy implements BenchmarkStrategy {

    private static final Logger LOGGER = LogManager.getLogger(DastBenchmarkStrategy.class);

    private static final String UNSECURE_VARIANT = "UNSECURE";
    private static final String KEY_SEPARATOR = "::";
    private static final String AXIS_TYPE = "TYPE";
    private static final String AXIS_CWE = "CWE";
    private static final String AXIS_WASC = "WASC";
    private static final String METHOD_ANY = "ANY";

    private final RestTemplate restTemplate;
    private final String groundTruthUrl;

    /**
     * The context path this instance is served under, used to reduce scanner findings and
     * ground-truth entries to a common key. Read from configuration rather than hardcoded: {@code
     * benchmark.dast.ground-truth.url} already follows {@code server.servlet.context-path}, so a
     * fixed value here would disagree with the very URLs it is comparing. Empty for a root
     * deployment, which strips nothing.
     */
    private final String contextPath;

    /**
     * Configures the ground-truth client and the URL normalisation shared by both protocols.
     *
     * @param restTemplate client used to fetch scanner ground truth
     * @param groundTruthUrl HTTP source of ground truth, including an aggregated facade endpoint
     * @param contextPath application context prefix to strip; null or empty strips no prefix
     */
    public DastBenchmarkStrategy(
            RestTemplate restTemplate,
            @Value("${benchmark.dast.ground-truth.url}") String groundTruthUrl,
            @Value("${server.servlet.context-path:}") String contextPath) {
        this.restTemplate = restTemplate;
        this.groundTruthUrl = groundTruthUrl;
        this.contextPath = contextPath;
    }

    /**
     * Grades only UNSECURE rows in the request's declared protocol scope.
     *
     * <p>Scope comes from the request, including when no findings are reported. Matching requires
     * URL and protocol agreement plus a taxonomy axis; HTTP retains its opt-in method check and WS
     * ignores the handshake method. Null protocols retain legacy HTTP semantics. Findings outside
     * scope remain unmatched and cannot earn credit.
     *
     * <p>Equivalent match-key signatures are processed once and each expected row is credited at
     * most once. Null findings and findings without a URL or usable taxonomy axis are ignored. An
     * empty eligible ground truth produces zero coverage rather than division by zero.
     *
     * @throws IOException if the ground-truth HTTP request fails
     */
    @Override
    public BenchmarkResult compare(ScannerFindings input) throws IOException {
        List<ScannerResponseBean> groundTruth = fetchGroundTruth();

        List<Finding> expectedRows = new ArrayList<>();
        Map<String, Set<Integer>> keyToExpected =
                buildExpectedIndex(groundTruth, expectedRows, input.getSupportedProtocols());

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
                                finding.getMethod(),
                                finding.getProtocol()));
            } else {
                detectedIndices.addAll(hits);
            }
        }

        List<Finding> detectedItems = new ArrayList<>();
        List<Finding> missedItems = new ArrayList<>();
        for (int i = 0; i < expectedRows.size(); i++) {
            if (detectedIndices.contains(i)) {
                detectedItems.add(expectedRows.get(i));
            } else {
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
                detectedItems,
                missedItems,
                unmatchedItems);
    }

    /** Fetches ground truth, treating an empty response body as no expected rows. */
    private List<ScannerResponseBean> fetchGroundTruth() throws IOException {
        try {
            ScannerResponseBean[] response =
                    restTemplate.getForObject(groundTruthUrl, ScannerResponseBean[].class);
            return (response == null) ? Collections.emptyList() : Arrays.asList(response);
        } catch (RestClientException rce) {
            throw new IOException("Failed to fetch DAST ground truth from " + groundTruthUrl, rce);
        }
    }

    /**
     * Adds eligible, unique expected rows and indexes each available type/CWE/WASC matching axis.
     *
     * <p>HTTP row identity includes protocol, URL, method and type; WS identity omits method. HTTP
     * rows also receive wildcard-method keys for findings that omit their method. Scope is applied
     * before rows enter either the index or the coverage denominator.
     */
    private Map<String, Set<Integer>> buildExpectedIndex(
            List<ScannerResponseBean> groundTruth,
            List<Finding> rowsOut,
            List<Protocol> supportedProtocols) {
        Map<String, Set<Integer>> keyToRows = new LinkedHashMap<>();
        Set<String> seenRowSignatures = new HashSet<>();
        for (ScannerResponseBean entry : groundTruth) {
            if (!UNSECURE_VARIANT.equalsIgnoreCase(entry.getVariant())) {
                continue;
            }
            Protocol protocol = Protocol.orDefault(entry.getProtocol());
            if (!supportedProtocols.contains(protocol)) {
                continue;
            }
            String url = normalizeUrl(entry.getUrl(), contextPath);
            String method =
                    (entry.getRequestMethod() != null)
                            ? entry.getRequestMethod().name()
                            : METHOD_ANY;
            List<VulnerabilityType> types = entry.getVulnerabilityTypes();
            if (types == null) {
                continue;
            }
            // WebSocket's GET handshake is not an ordinary HTTP method to benchmark.
            String applicableMethod = protocol.getMethodIfApplicable(method);
            for (VulnerabilityType type : types) {
                String rowSignature =
                        protocol
                                + KEY_SEPARATOR
                                + url
                                + KEY_SEPARATOR
                                + (applicableMethod != null ? applicableMethod + KEY_SEPARATOR : "")
                                + type.name();
                if (!seenRowSignatures.add(rowSignature)) {
                    continue;
                }
                int rowIndex = rowsOut.size();
                rowsOut.add(buildExpectedFinding(url, type, method, protocol));
                addAxisKeys(keyToRows, rowIndex, url, protocol, method, type);
                if (applicableMethod != null) {
                    addAxisKeys(keyToRows, rowIndex, url, protocol, METHOD_ANY, type);
                }
            }
        }
        return keyToRows;
    }

    /**
     * Registers every available taxonomy axis in the same protocol/method namespace. Multiple keys
     * may reference one row; the comparison still credits that row only once.
     */
    private static void addAxisKeys(
            Map<String, Set<Integer>> keyToRows,
            int rowIndex,
            String url,
            Protocol protocol,
            String method,
            VulnerabilityType type) {
        addKey(keyToRows, typeKey(url, protocol, method, type.name()), rowIndex);
        if (type.getCweID() != null) {
            String cwe = normalizeNumericId(type.getCweID().toString());
            if (!cwe.isEmpty()) {
                addKey(keyToRows, cweKey(url, protocol, method, cwe), rowIndex);
            }
        }
        if (type.getWascID() != null) {
            String wasc = normalizeNumericId(type.getWascID().toString());
            if (!wasc.isEmpty()) {
                addKey(keyToRows, wascKey(url, protocol, method, wasc), rowIndex);
            }
        }
    }

    /**
     * Keeps the legacy HTTP report shape, while WS expected items identify protocol, not method.
     */
    private static Finding buildExpectedFinding(
            String url, VulnerabilityType type, String method, Protocol protocol) {
        String cwe = (type.getCweID() != null) ? "CWE-" + type.getCweID() : null;
        String wasc = (type.getWascID() != null) ? type.getWascID().toString() : null;
        // Keep the legacy HTTP output shape. WS results must identify their transport.
        return new Finding(
                url,
                type.name(),
                null,
                null,
                cwe,
                wasc,
                protocol.getMethodIfApplicable(method),
                protocol == Protocol.WS ? Protocol.WS : null);
    }

    /**
     * Builds the finding's usable taxonomy keys in stable type/CWE/WASC order for matching and
     * duplicate signatures. A missing or blank HTTP method uses the wildcard; the WS key prefix
     * excludes method entirely. An empty list means no taxonomy axis can be evaluated.
     */
    private static List<String> keysForFinding(String url, Finding finding) {
        Protocol protocol = Protocol.orDefault(finding.getProtocol());
        String method =
                (finding.getMethod() != null && !finding.getMethod().trim().isEmpty())
                        ? finding.getMethod().trim().toUpperCase(Locale.ROOT)
                        : METHOD_ANY;
        List<String> keys = new ArrayList<>(3);
        if (finding.getType() != null && !finding.getType().trim().isEmpty()) {
            keys.add(typeKey(url, protocol, method, finding.getType()));
        }
        String cwe = normalizeNumericId(finding.getCwe());
        if (!cwe.isEmpty()) {
            keys.add(cweKey(url, protocol, method, cwe));
        }
        String wasc = normalizeNumericId(finding.getWascId());
        if (!wasc.isEmpty()) {
            keys.add(wascKey(url, protocol, method, wasc));
        }
        return keys;
    }

    private static void addKey(Map<String, Set<Integer>> map, String key, int rowIndex) {
        map.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(rowIndex);
    }

    /** Separates protocol namespaces and includes method only for ordinary HTTP findings. */
    private static String keyPrefix(String axis, String url, Protocol protocol, String method) {
        // WS keys intentionally omit the HTTP handshake method.
        String applicableMethod = protocol.getMethodIfApplicable(method);
        return axis
                + KEY_SEPARATOR
                + protocol
                + KEY_SEPARATOR
                + url
                + KEY_SEPARATOR
                + (applicableMethod != null ? applicableMethod + KEY_SEPARATOR : "");
    }

    /** Builds the matching key for the vulnerability-type axis. */
    private static String typeKey(String url, Protocol protocol, String method, String typeName) {
        return keyPrefix(AXIS_TYPE, url, protocol, method)
                + typeName.trim().toUpperCase(Locale.ROOT);
    }

    /** Builds the matching key for the CWE axis. */
    private static String cweKey(String url, Protocol protocol, String method, String cweDigits) {
        return keyPrefix(AXIS_CWE, url, protocol, method) + cweDigits;
    }

    /** Builds the matching key for the WASC axis. */
    private static String wascKey(String url, Protocol protocol, String method, String wascDigits) {
        return keyPrefix(AXIS_WASC, url, protocol, method) + wascDigits;
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
     *
     * <p>This reduction also handles {@code ws://} and {@code wss://} URLs, but never infers a
     * finding's protocol from its scheme. Transport remains a separate matching dimension.
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
