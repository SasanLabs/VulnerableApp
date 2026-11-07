package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.LinkedHashSet;
import java.util.List;
import org.sasanlabs.internal.utility.Protocol;

/**
 * Tool-agnostic input payload for the benchmark endpoint. {@code scanType} selects which benchmark
 * strategy runs — defaults to {@link ScanType#DAST} when omitted, preserving the original DAST-only
 * request shape. For DAST, {@code supportedProtocols} scopes the ground truth; omitted or null
 * defaults to HTTP. SAST does not use this field.
 */
public class ScannerFindings {

    @JsonProperty("tool")
    private String tool;

    @JsonProperty("scanType")
    private ScanType scanType;

    @JsonProperty("findings")
    private List<Finding> findings;

    @JsonProperty("supportedProtocols")
    private List<Protocol> supportedProtocols;

    /**
     * Creates a request whose declared DAST scope is independent of its reported findings.
     *
     * <p>A missing or null scan type selects DAST, and a missing or null protocol list selects
     * HTTP. Existing constructor signatures delegate with that same default scope. SAST ignores the
     * scope when scoring, but supplied protocol values still undergo input validation.
     *
     * @throws IllegalArgumentException if the protocol list is empty or contains a null member
     */
    @JsonCreator
    public ScannerFindings(
            @JsonProperty("tool") String tool,
            @JsonProperty("scanType") ScanType scanType,
            @JsonProperty("findings") List<Finding> findings,
            @JsonProperty("supportedProtocols")
                    @JsonDeserialize(contentUsing = BenchmarkProtocolDeserializer.class)
                    List<Protocol> supportedProtocols) {
        this.tool = tool;
        this.scanType = (scanType != null) ? scanType : ScanType.DAST;
        this.findings = findings;
        setSupportedProtocols(supportedProtocols);
    }

    /**
     * Validates and snapshots the declared scope, preserving the first occurrence of each protocol.
     *
     * <p>Null restores the legacy HTTP default; an explicit empty scope is invalid. The immutable
     * defensive copy prevents caller mutations from changing the denominator after construction.
     * Both the creator and Jackson's fallback property assignment use this method, so a repeated
     * JSON property cannot bypass scope validation.
     *
     * @throws IllegalArgumentException if the list is empty or contains a null member
     */
    @JsonProperty("supportedProtocols")
    @JsonDeserialize(contentUsing = BenchmarkProtocolDeserializer.class)
    private void setSupportedProtocols(List<Protocol> supportedProtocols) {
        if (supportedProtocols != null
                && (supportedProtocols.isEmpty()
                        || supportedProtocols.stream().anyMatch(protocol -> protocol == null))) {
            throw new IllegalArgumentException(
                    "supportedProtocols must contain at least one of HTTP or WS and no nulls");
        }
        // Scope is declared independently of findings, including scans that detect nothing.
        this.supportedProtocols =
                supportedProtocols == null
                        ? List.of(Protocol.HTTP)
                        : List.copyOf(new LinkedHashSet<>(supportedProtocols));
    }

    /** Retains the pre-scope Java signature and defaults its DAST evaluation scope to HTTP. */
    public ScannerFindings(String tool, ScanType scanType, List<Finding> findings) {
        this(tool, scanType, findings, null);
    }

    public ScannerFindings(String tool, List<Finding> findings) {
        this(tool, null, findings);
    }

    public String getTool() {
        return tool;
    }

    public ScanType getScanType() {
        return scanType;
    }

    public List<Finding> getFindings() {
        return findings;
    }

    /** Returns the immutable, de-duplicated scope; it is never inferred from findings. */
    public List<Protocol> getSupportedProtocols() {
        return supportedProtocols;
    }
}
