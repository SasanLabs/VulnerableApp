package org.sasanlabs.beans;

import org.sasanlabs.vulnerability.types.VulnerabilityType;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public class AllEndPointsResponseBean {

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

	public VulnerabilityType getVulnerabilityType() {
		return vulnerabilityType;
	}

	public void setVulnerabilityType(VulnerabilityType vulnerabilityType) {
		this.vulnerabilityType = vulnerabilityType;
	}

	private String name;

	private String description;

	private VulnerabilityType vulnerabilityType;
}
