package org.sasanlabs.benchmark.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.sasanlabs.benchmark.service.BenchmarkResultWriter;
import org.sasanlabs.benchmark.service.BenchmarkService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for benchmarking a scanner's findings against VulnerableApp's DAST ground truth.
 * Mounted alongside the existing {@code /scanner} and {@code /scanner/metadata} endpoints.
 */
@Profile("unsafe")
@RestController
public class BenchmarkController {

    private static final Logger LOGGER = LogManager.getLogger(BenchmarkController.class);

    private final BenchmarkService benchmarkService;
    private final BenchmarkResultWriter benchmarkResultWriter;

    public BenchmarkController(
            BenchmarkService benchmarkService, BenchmarkResultWriter benchmarkResultWriter) {
        this.benchmarkService = benchmarkService;
        this.benchmarkResultWriter = benchmarkResultWriter;
    }

    @PostMapping("/scanner/benchmark")
    public ResponseEntity<?> benchmark(@RequestBody ScannerFindings input) throws IOException {
        if (input == null || input.getTool() == null || input.getTool().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Field 'tool' is required and must be non-empty"));
        }
        if (input.getFindings() == null) {
            return ResponseEntity.badRequest()
                    .body(
                            Map.of(
                                    "error",
                                    "Field 'findings' is required (use [] for an empty list)"));
        }

        BenchmarkResult result = benchmarkService.compare(input);

        try {
            Path written = benchmarkResultWriter.write(result);
            LOGGER.info("Wrote benchmark result for tool '{}' to {}", input.getTool(), written);
        } catch (IOException ioe) {
            LOGGER.error(
                    "Failed to persist benchmark result for tool '{}'; returning 500 with result"
                            + " in body",
                    input.getTool(),
                    ioe);
            result.setPersistenceError("Failed to persist benchmark result: " + ioe.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }

        return ResponseEntity.ok(result);
    }
}
