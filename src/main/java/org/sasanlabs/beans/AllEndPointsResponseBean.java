package org.sasanlabs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.TreeSet;
import org.sasanlabs.vulnerability.types.VulnerabilityType;

/**
 * This is the Response Bean which contains information about all the present endpoints. This bean
 * is the Backbone behind the VulnerableApp Master-Detail UI design.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public class AllEndPointsResponseBean {

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("VulnerabilityTypes")
    private VulnerabilityType[] vulnerabilityTypes;

    @JsonProperty("Detailed Information")
    private Set<LevelResponseBean> levelDescriptionSet = new TreeSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VulnerabilityType[] getVulnerabilityTypes() {
        return vulnerabilityTypes;
    }

    public void setVulnerabilityTypes(VulnerabilityType[] vulnerabilityTypes) {
        this.vulnerabilityTypes = vulnerabilityTypes;
    }

    public Set<LevelResponseBean> getLevelDescriptionSet() {
        return levelDescriptionSet;
    }

    public void setLevelDescriptionSet(Set<LevelResponseBean> levelDescriptionSet) {
        this.levelDescriptionSet = levelDescriptionSet;
    }
}
