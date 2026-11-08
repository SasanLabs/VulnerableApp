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

    GroundTruth<Finding> loadDast() throws IOException {
        if (dastUrl == null || dastUrl.isBlank()) {
            return fetch(localDastUrl, ScanType.DAST, true, BenchmarkGroundTruthProvider::dastRows);
        }
        try {
            return fetch(dastUrl, ScanType.DAST, false, BenchmarkGroundTruthProvider::dastRows);
        } catch (IOException failure) {
            logFallback(ScanType.DAST, dastUrl, failure);
            if (dastUrl.equals(localDastUrl)) {
                throw failure;
            }
            return fetch(localDastUrl, ScanType.DAST, true, BenchmarkGroundTruthProvider::dastRows);
        }
    }

    /** SAST rows stay {@link ExpectedIssue}s so their {@code lineCount} reaches the matcher. */
    GroundTruth<ExpectedIssue> loadSast() throws IOException {
        if (sastUrl == null || sastUrl.isBlank()) {
            return localSast(false);
        }
        try {
            return fetch(sastUrl, ScanType.SAST, false, (entries, urls) -> sastRows(entries));
        } catch (IOException failure) {
            logFallback(ScanType.SAST, sastUrl, failure);
            return localSast(true);
        }
    }

    private static void logFallback(ScanType type, String url, IOException failure) {
        LOGGER.warn(
                "Unable to load {} ground truth from {}; using local ground truth",
                type,
                url,
                failure);
    }

    private GroundTruth<ExpectedIssue> localSast(boolean fallback) throws IOException {
        return new GroundTruth<>(
                Collections.singletonMap(applicationName, localIssues.getExpectedIssues()),
                Collections.emptyMap(),
                fallback);
    }

    private <T> GroundTruth<T> fetch(
            String url, ScanType type, boolean fallback, RowParser<T> parser) throws IOException {
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
        Map<String, List<T>> apps = new LinkedHashMap<>();
        Map<String, Set<String>> dastUrls = new LinkedHashMap<>();
        if (body.isArray()) {
            Set<String> urls = new LinkedHashSet<>();
            apps.put(applicationName, parser.parse(body, urls));
            dastUrls.put(applicationName, urls);
        } else {
            Iterator<Map.Entry<String, JsonNode>> fields = body.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> app = fields.next();
                if (app.getKey().isBlank()) {
                    throw new IOException("Ground truth application name is empty");
                }
                Set<String> urls = new LinkedHashSet<>();
                apps.put(app.getKey(), parser.parse(app.getValue(), urls));
                dastUrls.put(app.getKey(), urls);
            }
        }
        if (apps.isEmpty()) {
            throw new IOException(
                    "Facade returned no applications; local ground truth is required");
        }
        return new GroundTruth<>(apps, dastUrls, fallback);
    }

    /** Reads one application's rows; DAST rows also record every URL the application owns. */
    private interface RowParser<T> {
        List<T> parse(JsonNode entries, Set<String> dastUrls) throws IOException;
    }

    private static List<JsonNode> objects(JsonNode entries) throws IOException {
        if (!entries.isArray()) {
            throw new IOException("Ground truth for each application must be an array");
        }
        List<JsonNode> objects = new ArrayList<>();
        for (JsonNode entry : entries) {
            if (!entry.isObject()) {
                throw new IOException("Ground truth row must be an object");
            }
            objects.add(entry);
        }
        return objects;
    }

    /**
     * A missing or null {@code lineCount} is 1, as for a CSV row without one. {@code
     * numberOfSources} is not used for scoring and is 0 when it is not an integer.
     */
    private static List<ExpectedIssue> sastRows(JsonNode entries) throws IOException {
        List<ExpectedIssue> rows = new ArrayList<>();
        for (JsonNode entry : objects(entries)) {
            if (!entry.path("filePath").isTextual()
                    || entry.get("filePath").textValue().isBlank()
                    || !entry.path("line").isInt()
                    || entry.get("line").intValue() < 1) {
                throw new IOException(
                        "SAST ground truth requires a nonblank filePath and a positive line");
            }
            String label = text(entry, "type");
            String cwe = text(entry, "cwe");
            if ((label == null || label.isBlank()) && (cwe == null || cwe.isBlank())) {
                throw new IOException("SAST ground truth requires type or cwe");
            }
            int lineCount = 1;
            JsonNode count = entry.path("lineCount");
            if (!count.isMissingNode() && !count.isNull()) {
                if (!count.isInt() || count.intValue() < 1) {
                    throw new IOException(
                            "SAST ground truth lineCount must be an integer from 1 to "
                                    + Integer.MAX_VALUE);
                }
                lineCount = count.intValue();
            }
            rows.add(
                    new ExpectedIssue(
                            cwe,
                            label,
                            text(entry, "filePath"),
                            entry.get("line").intValue(),
                            entry.path("numberOfSources").isInt()
                                    ? entry.get("numberOfSources").intValue()
                                    : 0,
                            lineCount));
        }
        return rows;
    }

    private static List<Finding> dastRows(JsonNode entries, Set<String> dastUrls)
            throws IOException {
        List<Finding> rows = new ArrayList<>();
        for (JsonNode entry : objects(entries)) {
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
                throw new IOException("DAST ground truth requires a SECURE or UNSECURE variant");
            }
            if (!entry.path("url").isTextual()
                    || !entry.path("vulnerabilityTypes").isArray()
                    || entry.get("vulnerabilityTypes").isEmpty()) {
                throw new IOException(
                        "UNSECURE DAST ground truth requires a url and at least one type");
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
        return rows;
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).isTextual() ? node.get(field).textValue() : null;
    }

    static class GroundTruth<T> {
        final Map<String, List<T>> applications;
        final Map<String, Set<String>> dastUrls;
        final boolean fallback;

        GroundTruth(
                Map<String, List<T>> applications,
                Map<String, Set<String>> dastUrls,
                boolean fallback) {
            this.applications = applications;
            this.dastUrls = dastUrls;
            this.fallback = fallback;
        }
    }
}
