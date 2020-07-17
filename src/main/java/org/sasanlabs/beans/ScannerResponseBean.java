package org.sasanlabs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.sasanlabs.internal.utility.annotations.RequestParameterLocation;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.http.HttpMethod;

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

    @JsonProperty("location")
    private RequestParameterLocation requestParameterLocation;

    @JsonProperty("parameter")
    private String parameterName;

    @JsonProperty("sampleValues")
    private String[] sampleValues;

    @JsonProperty("method")
    private HttpMethod httpMethod;

    @JsonProperty("vulnerabilityTypes")
    private List<VulnerabilityType> vulnerabilityTypes;

    public ScannerResponseBean(
            String url,
            RequestParameterLocation requestParameterLocation,
            String parameterName,
            String[] sampleValues,
            HttpMethod httpMethod,
            List<VulnerabilityType> vulnerabilityTypes) {
        super();
        this.url = url;
        this.requestParameterLocation = requestParameterLocation;
        this.parameterName = parameterName;
        this.sampleValues = sampleValues;
        this.httpMethod = httpMethod;
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

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public List<VulnerabilityType> getVulnerabilityTypes() {
        return vulnerabilityTypes;
    }
}
