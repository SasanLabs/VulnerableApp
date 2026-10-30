package org.sasanlabs.benchmark.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.sasanlabs.benchmark.model.Finding;
import org.sasanlabs.benchmark.model.ScanType;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Reads standalone or facade ground truth without changing the scanner-facing endpoints. */
@Service
public class BenchmarkGroundTruthProvider {
    private static final Logger LOGGER = LogManager.getLogger(BenchmarkGroundTruthProvider.class);
    private final RestTemplate restTemplate;
    private final IExpectedIssuesProvider localIssues;
    private final String dastUrl;
    private final String localDastUrl;
    private final String sastUrl;
    private final String applicationName;

    public BenchmarkGroundTruthProvider(
            RestTemplate restTemplate,
            IExpectedIssuesProvider localIssues,
            @Value("${benchmark.dast.ground-truth.url}") String dastUrl,
            @Value(
                            "${benchmark.dast.ground-truth.fallback-url:http://localhost:${server.port:9090}${server.servlet.context-path:/VulnerableApp}/scanner/dast}")
                    String localDastUrl,
            @Value("${benchmark.sast.ground-truth.url:}") String sastUrl,
            @Value("${benchmark.application-name:VulnerableApp}") String applicationName) {
        this.restTemplate = restTemplate;
        this.localIssues = localIssues;
        this.dastUrl = dastUrl;
        this.localDastUrl = localDastUrl;
        this.sastUrl = sastUrl;
        this.applicationName = applicationName;
    }

    GroundTruth load(ScanType type) throws IOException {
        String url = type == ScanType.DAST ? dastUrl : sastUrl;
        if (url == null || url.isBlank()) {
            return type == ScanType.SAST ? localSast(false) : fetch(localDastUrl, type, true);
        }
        try {
            return fetch(url, type, false);
        } catch (IOException failure) {
            LOGGER.warn(
                    "Unable to load {} ground truth from {}; using local ground truth",
                    type,
                    url,
                    failure);
            if (type == ScanType.SAST) {
                return localSast(true);
            }
            if (url.equals(localDastUrl)) {
                throw failure;
            }
            return fetch(localDastUrl, type, true);
        }
    }

    private GroundTruth localSast(boolean fallback) throws IOException {
        List<Finding> rows = new ArrayList<>();
        for (ExpectedIssue issue : localIssues.getExpectedIssues()) {
            rows.add(
                    new Finding(
                            null,
                            issue.getVulnerabilityType(),
                            issue.getFilePath(),
                            issue.getLine(),
                            issue.getCwe()));
        }
        return new GroundTruth(
                Collections.singletonMap(applicationName, rows), Collections.emptyMap(), fallback);
    }

    private GroundTruth fetch(String url, ScanType type, boolean fallback) throws IOException {
        final JsonNode body;
        try {
            body = restTemplate.getForObject(url, JsonNode.class);
        } catch (RestClientException failure) {
            throw new IOException("Failed to fetch " + type + " ground truth from " + url, failure);
        }
        if (body == null || (!body.isArray() && !body.isObject())) {
            throw new IOException(
                    "Expected an array or application-keyed ground truth object from " + url);
        }
        Map<String, List<Finding>> apps = new LinkedHashMap<>();
        Map<String, Set<String>> dastUrls = new LinkedHashMap<>();
        if (body.isArray()) {
            Set<String> urls = new LinkedHashSet<>();
            apps.put(applicationName, parseRows(body, type, urls));
            dastUrls.put(applicationName, urls);
        } else {
            Iterator<Map.Entry<String, JsonNode>> fields = body.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> app = fields.next();
                if (app.getKey().isBlank()) {
                    throw new IOException("Ground truth application name is empty");
                }
                Set<String> urls = new LinkedHashSet<>();
                apps.put(app.getKey(), parseRows(app.getValue(), type, urls));
                dastUrls.put(app.getKey(), urls);
            }
        }
        if (apps.isEmpty()) {
            throw new IOException(
                    "Facade returned no applications; local ground truth is required");
        }
        return new GroundTruth(apps, dastUrls, fallback);
    }

    private List<Finding> parseRows(JsonNode entries, ScanType type, Set<String> dastUrls)
            throws IOException {
        if (!entries.isArray()) {
            throw new IOException("Ground truth for each application must be an array");
        }
        List<Finding> rows = new ArrayList<>();
        for (JsonNode entry : entries) {
            if (!entry.isObject()) {
                throw new IOException("Ground truth row must be an object");
            }
            if (type == ScanType.SAST) {
                if (!entry.path("filePath").isTextual() || !entry.path("line").isInt()) {
                    throw new IOException("SAST ground truth requires filePath and line");
                }
                String label = text(entry, "type");
                String cwe = text(entry, "cwe");
                if ((label == null || label.isBlank()) && (cwe == null || cwe.isBlank())) {
                    throw new IOException("SAST ground truth requires type or cwe");
                }
                rows.add(
                        new Finding(
                                null,
                                label,
                                text(entry, "filePath"),
                                entry.get("line").intValue(),
                                cwe));
            } else {
                if (!entry.path("url").isTextual() || entry.get("url").textValue().isBlank()) {
                    throw new IOException("DAST ground truth requires a nonempty url");
                }
                // Secure routes still identify their app, but do not contribute expected findings.
                dastUrls.add(entry.get("url").textValue());
                String variant = text(entry, "variant");
                if ("SECURE".equalsIgnoreCase(variant)) {
                    continue;
                }
                if (!"UNSECURE".equalsIgnoreCase(variant)) {
                    throw new IOException(
                            "DAST ground truth requires a SECURE or UNSECURE variant");
                }
                if (!entry.path("url").isTextual() || !entry.path("vulnerabilityTypes").isArray()) {
                    throw new IOException("DAST ground truth requires url and vulnerabilityTypes");
                }
                for (JsonNode label : entry.get("vulnerabilityTypes")) {
                    if (!label.isTextual() || label.asText().isBlank()) {
                        throw new IOException("DAST vulnerability type must be a nonempty string");
                    }
                    String name = label.asText().trim();
                    String cwe = null;
                    String wasc = null;
                    try {
                        VulnerabilityType known =
                                VulnerabilityType.valueOf(name.toUpperCase(Locale.ROOT));
                        name = known.name();
                        cwe = known.getCweID() == null ? null : "CWE-" + known.getCweID();
                        wasc = known.getWascID() == null ? null : known.getWascID().toString();
                    } catch (IllegalArgumentException unknownType) {
                        // Other applications may expose native type names absent from the Java
                        // enum.
                    }
                    rows.add(
                            new Finding(
                                    text(entry, "url"),
                                    name,
                                    null,
                                    null,
                                    cwe,
                                    wasc,
                                    text(entry, "method")));
                }
            }
        }
        return rows;
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).isTextual() ? node.get(field).textValue() : null;
    }

    static class GroundTruth {
        final Map<String, List<Finding>> applications;
        final Map<String, Set<String>> dastUrls;
        final boolean fallback;

        GroundTruth(
                Map<String, List<Finding>> applications,
                Map<String, Set<String>> dastUrls,
                boolean fallback) {
            this.applications = applications;
            this.dastUrls = dastUrls;
            this.fallback = fallback;
        }
    }
}
