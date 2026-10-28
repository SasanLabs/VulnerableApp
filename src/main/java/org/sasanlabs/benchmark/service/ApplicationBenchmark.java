package org.sasanlabs.benchmark.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScanType;
import org.sasanlabs.benchmark.model.ScannerFindings;

/**
 * Partitions findings before scoring so identical locations in separate apps cannot cross-match.
 */
final class ApplicationBenchmark {
    private ApplicationBenchmark() {}

    static BenchmarkResult compare(
            ScannerFindings input,
            BenchmarkGroundTruthProvider.GroundTruth truth,
            String contextPath,
            BiFunction<ScannerFindings, List<Finding>, BenchmarkResult> scorer) {
        Map<String, List<Finding>> grouped = new LinkedHashMap<>();
        truth.applications.keySet().forEach(app -> grouped.put(app, new ArrayList<>()));
        Map<String, Set<String>> owners = new LinkedHashMap<>();
        for (Map.Entry<String, List<Finding>> app : truth.applications.entrySet()) {
            for (Finding row : app.getValue()) {
                String key = location(row, input.getScanType(), contextPath);
                owners.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(app.getKey());
            }
        }
        Map<String, Set<String>> fullUrlOwners = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> app : truth.dastUrls.entrySet()) {
            for (String url : app.getValue()) {
                String fullPath = DastBenchmarkStrategy.normalizeUrl(url, "");
                fullUrlOwners
                        .computeIfAbsent(fullPath, ignored -> new LinkedHashSet<>())
                        .add(app.getKey());
                String key = DastBenchmarkStrategy.normalizeUrl(url, contextPath);
                owners.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(app.getKey());
            }
        }
        List<Finding> unattributed = new ArrayList<>();
        if (input.getFindings() != null) {
            for (Finding finding : input.getFindings()) {
                if (finding == null) {
                    continue;
                }
                String app =
                        resolveApplication(
                                finding,
                                input.getScanType(),
                                truth.applications,
                                contextPath,
                                owners,
                                fullUrlOwners);
                if (app == null) {
                    unattributed.add(finding.withApplication(null));
                } else {
                    grouped.computeIfAbsent(app, key -> new ArrayList<>())
                            .add(finding.withApplication(app));
                }
            }
        }
        Map<String, BenchmarkResult> results = new LinkedHashMap<>();
        List<Finding> missed = new ArrayList<>();
        List<Finding> unmatched = new ArrayList<>();
        int total = 0;
        int detected = 0;
        for (Map.Entry<String, List<Finding>> app : grouped.entrySet()) {
            BenchmarkResult result =
                    scorer.apply(
                            new ScannerFindings(
                                    input.getTool(), input.getScanType(), app.getValue()),
                            truth.applications.getOrDefault(app.getKey(), Collections.emptyList()));
            result.getMissedItems().replaceAll(f -> f.withApplication(app.getKey()));
            result.getUnmatchedItems().replaceAll(f -> f.withApplication(app.getKey()));
            results.put(app.getKey(), result);
            total += result.getTotalExpected();
            detected += result.getDetected();
            missed.addAll(result.getMissedItems());
            unmatched.addAll(result.getUnmatchedItems());
        }
        if (!unattributed.isEmpty()) {
            BenchmarkResult unknown =
                    scorer.apply(
                            new ScannerFindings(input.getTool(), input.getScanType(), unattributed),
                            Collections.emptyList());
            unmatched.addAll(unknown.getUnmatchedItems());
        }
        BenchmarkResult result =
                new BenchmarkResult(
                        input.getTool(),
                        total == 0 ? 0.0 : detected * 100.0 / total,
                        total,
                        detected,
                        missed.size(),
                        unmatched.size(),
                        missed,
                        unmatched);
        result.setApplications(results);
        result.setGroundTruthFallback(truth.fallback);
        return result;
    }

    private static String resolveApplication(
            Finding finding,
            ScanType type,
            Map<String, List<Finding>> apps,
            String contextPath,
            Map<String, Set<String>> owners,
            Map<String, Set<String>> fullUrlOwners) {
        if (finding.getApplication() != null && !finding.getApplication().isBlank()) {
            return finding.getApplication();
        }
        if (apps.size() == 1) {
            return apps.keySet().iterator().next();
        }
        String prefixOwner = null;
        if (type == ScanType.DAST && finding.getUrl() != null) {
            String path = DastBenchmarkStrategy.normalizeUrl(finding.getUrl(), "");
            for (String app : apps.keySet()) {
                if ((path.equals("/" + app) || path.startsWith("/" + app + "/"))
                        && (prefixOwner == null || app.length() > prefixOwner.length())) {
                    prefixOwner = app;
                }
            }
            Set<String> fullOwners = fullUrlOwners.get(path);
            boolean explicitContext =
                    contextPath != null
                            && !contextPath.isEmpty()
                            && (path.equals(contextPath) || path.startsWith(contextPath + "/"));
            if (fullOwners != null && (prefixOwner != null || explicitContext)) {
                return fullOwners.size() == 1 ? fullOwners.iterator().next() : null;
            }
            if (prefixOwner != null || explicitContext) {
                return prefixOwner;
            }
        }
        Set<String> candidates = owners.get(location(finding, type, contextPath));
        if (candidates != null) {
            return candidates.size() == 1 ? candidates.iterator().next() : null;
        }
        return prefixOwner;
    }

    private static String location(Finding finding, ScanType type, String contextPath) {
        return type == ScanType.SAST
                ? SastBenchmarkStrategy.normalizeFilePath(finding.getFilePath())
                : DastBenchmarkStrategy.normalizeUrl(finding.getUrl(), contextPath);
    }
}
