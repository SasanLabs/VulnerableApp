package org.sasanlabs.facade.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** @author preetkaran20@gmail.com KSASAN */
public class FacadeResourceInformation {

    @JsonProperty("htmlResource")
    private FacadeResourceURI htmlResource;

    @JsonProperty("staticResources")
    private List<FacadeResourceURI> staticResources;

    public FacadeResourceURI getHtmlResource() {
        return htmlResource;
    }

    public void setHtmlResource(FacadeResourceURI htmlResource) {
        this.htmlResource = htmlResource;
    }

    public List<FacadeResourceURI> getStaticResources() {
        return staticResources;
    }

    public void setStaticResources(List<FacadeResourceURI> staticResources) {
        this.staticResources = staticResources;
    }
}
