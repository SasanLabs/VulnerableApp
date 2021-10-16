package org.sasanlabs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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

    @JsonProperty("variant")
    private String variant;

    @JsonProperty("method")
    private RequestMethod requestMethod;

    @JsonProperty("vulnerabilityTypes")
    private List<VulnerabilityType> vulnerabilityTypes;

    public ScannerResponseBean(
            String url,
            String variant,
            RequestMethod requestMethod,
            List<VulnerabilityType> vulnerabilityTypes) {
        super();
        this.url = url;
        this.variant = variant;
        this.requestMethod = requestMethod;
        this.vulnerabilityTypes = vulnerabilityTypes;
    }

    public String getUrl() {
        return url;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public List<VulnerabilityType> getVulnerabilityTypes() {
        return vulnerabilityTypes;
    }
}
