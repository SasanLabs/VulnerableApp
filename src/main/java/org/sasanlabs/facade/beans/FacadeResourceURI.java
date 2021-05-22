package org.sasanlabs.facade.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FacadeResourceURI {

    @JsonProperty("isAbsolute")
    private boolean isAbsolute;

    @JsonProperty("uri")
    private String uri;

    public FacadeResourceURI(boolean isAbsolute, String uri) {
        super();
        this.isAbsolute = isAbsolute;
        this.uri = uri;
    }

    public boolean isAbsolute() {
        return isAbsolute;
    }

    public String getUri() {
        return uri;
    }
}
