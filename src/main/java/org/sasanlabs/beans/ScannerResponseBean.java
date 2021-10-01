package org.sasanlabs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.sasanlabs.internal.utility.annotations.RequestParameterLocation;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * This class represents the response which is used by the scanners for executing attacks on the
 * VulnerableApp applications. This response bean is self sufficient for scanners to know about all
 * the exposed vulnerabilities by the application.
 *
 * <p><a href="https://github.com/SasanLabs/VulnerableApp/issues/130">For more information on how
 * scanners can use this response.</a>
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public class ScannerResponseBean {

    @JsonProperty("url")
    private String url;

    @JsonProperty("level")
    private String level;

    @JsonProperty("variant")
    private String variant;

    @JsonProperty("location")
    private RequestParameterLocation requestParameterLocation;

    @JsonProperty("parameter")
    private String parameterName;

    @JsonProperty("sampleValues")
    private String[] sampleValues;

    @JsonProperty("method")
    private RequestMethod requestMethod;

    @JsonProperty("vulnerabilityTypes")
    private List<VulnerabilityType> vulnerabilityTypes;

    public ScannerResponseBean(
            String url,
            String level,
            String variant,
            RequestParameterLocation requestParameterLocation,
            String parameterName,
            String[] sampleValues,
            RequestMethod requestMethod,
            List<VulnerabilityType> vulnerabilityTypes) {
        super();
        this.url = url;
        this.level = level;
        this.variant = variant;
        this.requestParameterLocation = requestParameterLocation;
        this.parameterName = parameterName;
        this.sampleValues = sampleValues;
        this.requestMethod = requestMethod;
        this.vulnerabilityTypes = vulnerabilityTypes;
    }

    public String getUrl() {
        return url;
    }

    public RequestParameterLocation getRequestParameterLocation() {
        return requestParameterLocation;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String[] getSampleValues() {
        return sampleValues;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public List<VulnerabilityType> getVulnerabilityTypes() {
        return vulnerabilityTypes;
    }
}
