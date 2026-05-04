package org.sasanlabs.benchmark.service;

import java.io.IOException;
import org.sasanlabs.benchmark.model.BenchmarkResult;
import org.sasanlabs.benchmark.model.ScannerFindings;

/**
 * Compares a scanner's findings against a specific kind of ground truth (e.g. DAST endpoints, SAST
 * source-level expected issues) and produces coverage / missed / false-positive metrics. Each
 * implementation owns its ground-truth source and matching rules for one scan type.
 */
public interface BenchmarkStrategy {

    BenchmarkResult compare(ScannerFindings input) throws IOException;
}
