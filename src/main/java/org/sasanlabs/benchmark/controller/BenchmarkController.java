package org.sasanlabs.benchmark.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.ScannerFindings;
import org.sasanlabs.benchmark.service.BenchmarkResultWriter;
import org.sasanlabs.benchmark.service.BenchmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for benchmarking a scanner's findings against VulnerableApp's DAST ground truth.
 * Mounted alongside the existing {@code /scanner} and {@code /scanner/metadata} endpoints.
 */
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
    public ResponseEntity<?> benchmark(@RequestBody ScannerFindings input)
            throws JsonProcessingException, UnknownHostException {
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
                    "Failed to persist benchmark result for tool '{}'; returning result in"
                            + " response body only",
                    input.getTool(),
                    ioe);
        }

        return ResponseEntity.ok(result);
    }
}
