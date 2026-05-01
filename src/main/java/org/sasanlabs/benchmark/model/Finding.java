package org.sasanlabs.benchmark.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single scanner finding — a URL that the scanner believes carries a vulnerability of a given
 * type. URLs may be relative (path-only) or absolute; the comparator normalises both sides before
 * matching.
 */
public class Finding {

    @JsonProperty("url")
    private String url;

    @JsonProperty("type")
    private String type;

    public Finding(@JsonProperty("url") String url, @JsonProperty("type") String type) {
        this.url = url;
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }
}
