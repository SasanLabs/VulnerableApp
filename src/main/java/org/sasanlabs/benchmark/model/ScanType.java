package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * The kind of scanner whose findings are being benchmarked. Each value selects a different
 * ground-truth source and matching strategy in the benchmark service.
 */
public enum ScanType {
    DAST,
    SAST;

    @JsonCreator
    public static ScanType fromString(String value) {
        if (value == null) {
            return null;
        }
        return ScanType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
