package org.sasanlabs.beans;

import java.util.Set;
import java.util.TreeSet;

import org.sasanlabs.vulnerability.types.VulnerabilityType;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public class AllEndPointsResponseBean {

	private String name;

	private String description;

	private VulnerabilityType[] vulnerabilityTypes;

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
