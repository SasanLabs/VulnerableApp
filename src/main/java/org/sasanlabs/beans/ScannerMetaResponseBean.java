package org.sasanlabs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.sasanlabs.internal.utility.annotations.RequestParameterLocation;
import org.sasanlabs.vulnerability.types.VulnerabilityType;

/**
 * This class represents the meta data about the data provided by scanner endpoint. This is useful
 * for scanners to map there vulnerability type names with VulnerableApp's vulnerability type names
 * and same goes with the request parameter locations etc. This is mainly used for mapping
 * conventions across different applications
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public class ScannerMetaResponseBean {

    @JsonProperty("availableVulnerabilities")
    private List<VulnerabilityType> availableVulnerabilityTypes;

    @JsonProperty("availableLocations")
    private List<RequestParameterLocation> availableLocations;

    public ScannerMetaResponseBean(
            List<VulnerabilityType> availableVulnerabilityTypes,
            List<RequestParameterLocation> availableLocations) {
        super();
        this.availableVulnerabilityTypes = availableVulnerabilityTypes;
        this.availableLocations = availableLocations;
    }

    public List<VulnerabilityType> getAvailableVulnerabilityTypes() {
        return availableVulnerabilityTypes;
    }

    public List<RequestParameterLocation> getAvailableLocations() {
        return availableLocations;
    }
}
