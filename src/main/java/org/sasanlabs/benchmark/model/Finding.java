package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.sasanlabs.internal.utility.Protocol;

/**
 * A single scanner finding. DAST findings populate {@code url} + {@code type} (and optionally
 * {@code protocol} / {@code method} / {@code cwe} / {@code wascId} for taxonomy-tolerant matching);
 * SAST findings populate {@code filePath} + {@code line} + {@code type} (and optionally {@code
 * cwe}). Null fields are omitted from the serialised output.
 *
 * <p>{@code protocol} describes DAST transport, not a SAST location. It remains nullable in this
 * shared input model so older constructors and JSON payloads keep their original field shape; the
 * DAST strategy interprets null as HTTP. The SAST strategy ignores this property and omits it from
 * result items.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Finding {

    @JsonProperty("url")
    private String url;

    @JsonProperty("type")
    private String type;

    @JsonProperty("filePath")
    private String filePath;

    @JsonProperty("line")
    private Integer line;

    @JsonProperty("cwe")
    private String cwe;

    @JsonProperty("wascId")
    private String wascId;

    @JsonProperty("method")
    private String method;

    @JsonProperty("protocol")
    @JsonDeserialize(using = BenchmarkProtocolDeserializer.class)
    private Protocol protocol;

    /**
     * Creates a finding without inferring transport from its URL or HTTP method.
     *
     * <p>The optional protocol is retained as supplied. A null value stays absent from serialised
     * JSON and means HTTP when evaluated as DAST. Older constructor signatures delegate with a null
     * protocol, preserving their Java API and legacy HTTP behaviour.
     */
    @JsonCreator
    public Finding(
            @JsonProperty("url") String url,
            @JsonProperty("type") String type,
            @JsonProperty("filePath") String filePath,
            @JsonProperty("line") Integer line,
            @JsonProperty("cwe") String cwe,
            @JsonProperty("wascId") String wascId,
            @JsonProperty("method") String method,
            @JsonProperty("protocol") @JsonDeserialize(using = BenchmarkProtocolDeserializer.class)
                    Protocol protocol) {
        this.url = url;
        this.type = type;
        this.filePath = filePath;
        this.line = line;
        this.cwe = cwe;
        this.wascId = wascId;
        this.method = method;
        this.protocol = protocol;
    }

    /** Retains the pre-protocol Java signature, leaving protocol absent from the finding. */
    public Finding(
            String url,
            String type,
            String filePath,
            Integer line,
            String cwe,
            String wascId,
            String method) {
        this(url, type, filePath, line, cwe, wascId, method, null);
    }

    public Finding(
            String url, String type, String filePath, Integer line, String cwe, String wascId) {
        this(url, type, filePath, line, cwe, wascId, null);
    }

    public Finding(String url, String type, String filePath, Integer line, String cwe) {
        this(url, type, filePath, line, cwe, null, null);
    }

    public Finding(String url, String type) {
        this(url, type, null, null, null, null, null);
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getFilePath() {
        return filePath;
    }

    public Integer getLine() {
        return line;
    }

    public String getCwe() {
        return cwe;
    }

    public String getWascId() {
        return wascId;
    }

    public String getMethod() {
        return method;
    }

    /** Null retains the legacy JSON shape; the DAST comparator interprets it as HTTP. */
    public Protocol getProtocol() {
        return protocol;
    }
}
