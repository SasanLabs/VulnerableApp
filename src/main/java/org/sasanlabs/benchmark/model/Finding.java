package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single scanner finding. DAST findings populate {@code url} + {@code type}; SAST findings
 * populate {@code filePath} + {@code line} + {@code type} (and optionally {@code cwe}). Unused
 * fields are omitted from the serialised output.
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

    @JsonCreator
    public Finding(
            @JsonProperty("url") String url,
            @JsonProperty("type") String type,
            @JsonProperty("filePath") String filePath,
            @JsonProperty("line") Integer line,
            @JsonProperty("cwe") String cwe) {
        this.url = url;
        this.type = type;
        this.filePath = filePath;
        this.line = line;
        this.cwe = cwe;
    }

    public Finding(String url, String type) {
        this(url, type, null, null, null);
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
}
