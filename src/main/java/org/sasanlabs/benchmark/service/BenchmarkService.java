package org.sasanlabs.benchmark.service;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
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
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.stereotype.Service;

/**
 * Compares a scanner's findings against VulnerableApp's DAST ground truth (the {@code /scanner}
 * endpoint's data) and produces coverage / missed / false-positive metrics.
 *
 * <p>Matching is set-based on a key of {@code normalizedUrl + "::" + uppercaseType}. URLs are
 * normalised to a context-path-stripped path; types are matched case-insensitively against {@link
 * VulnerabilityType} names. Only {@code UNSECURE} ground-truth entries count toward expected — a
 * scanner finding against a {@code SECURE} URL is treated as a false positive.
 */
@Service
public class BenchmarkService {

    private static final Logger LOGGER = LogManager.getLogger(BenchmarkService.class);

    private static final String CONTEXT_PATH = "/VulnerableApp";
    private static final String UNSECURE_VARIANT = "UNSECURE";
    private static final String KEY_SEPARATOR = "::";

    private final IEndPointsInformationProvider endPointsInformationProvider;

    public BenchmarkService(IEndPointsInformationProvider endPointsInformationProvider) {
        this.endPointsInformationProvider = endPointsInformationProvider;
    }

    public BenchmarkResult compare(ScannerFindings input)
            throws com.fasterxml.jackson.core.JsonProcessingException, UnknownHostException {
        List<ScannerResponseBean> groundTruth =
                endPointsInformationProvider.getScannerRelatedEndPointInformation();

        Map<String, Finding> expectedByKey = buildExpectedIndex(groundTruth);
        Map<String, Finding> foundByKey = buildFoundIndex(input.getFindings());

        Set<String> detectedKeys = new LinkedHashSet<>(expectedByKey.keySet());
        detectedKeys.retainAll(foundByKey.keySet());

        List<Finding> missedItems = new ArrayList<>();
        for (Map.Entry<String, Finding> entry : expectedByKey.entrySet()) {
            if (!foundByKey.containsKey(entry.getKey())) {
                missedItems.add(entry.getValue());
            }
        }

        List<Finding> falsePositiveItems = new ArrayList<>();
        for (Map.Entry<String, Finding> entry : foundByKey.entrySet()) {
            if (!expectedByKey.containsKey(entry.getKey())) {
                falsePositiveItems.add(entry.getValue());
            }
        }

        int totalExpected = expectedByKey.size();
        int detected = detectedKeys.size();
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
                falsePositiveItems.size(),
                missedItems,
                falsePositiveItems);
    }

    private Map<String, Finding> buildExpectedIndex(List<ScannerResponseBean> groundTruth) {
        Map<String, Finding> index = new LinkedHashMap<>();
        for (ScannerResponseBean entry : groundTruth) {
            if (!UNSECURE_VARIANT.equalsIgnoreCase(entry.getVariant())) {
                continue;
            }
            String normalizedUrl = normalizeUrl(entry.getUrl());
            List<VulnerabilityType> types = entry.getVulnerabilityTypes();
            if (types == null) {
                continue;
            }
            for (VulnerabilityType type : types) {
                String key = normalizedUrl + KEY_SEPARATOR + type.name();
                index.putIfAbsent(key, new Finding(normalizedUrl, type.name()));
            }
        }
        return index;
    }

    private Map<String, Finding> buildFoundIndex(List<Finding> findings) {
        if (findings == null || findings.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Finding> index = new LinkedHashMap<>();
        for (Finding finding : findings) {
            if (finding == null || finding.getUrl() == null || finding.getType() == null) {
                continue;
            }
            String normalizedUrl = normalizeUrl(finding.getUrl());
            String normalizedType = finding.getType().trim().toUpperCase(Locale.ROOT);
            String key = normalizedUrl + KEY_SEPARATOR + normalizedType;
            index.putIfAbsent(key, new Finding(normalizedUrl, normalizedType));
        }
        return index;
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
