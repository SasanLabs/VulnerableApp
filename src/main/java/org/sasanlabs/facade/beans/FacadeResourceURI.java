package org.sasanlabs.facade.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FacadeResourceURI {

    @JsonProperty("isAbsolute")
    private boolean isAbsolute;

    @JsonProperty("uri")
    private String uri;

    public boolean isAbsolute() {
        return isAbsolute;
    }

    public void setAbsolute(boolean isAbsolute) {
        this.isAbsolute = isAbsolute;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
