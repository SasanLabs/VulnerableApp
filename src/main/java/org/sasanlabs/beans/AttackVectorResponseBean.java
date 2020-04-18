package org.sasanlabs.beans;

import org.sasanlabs.vulnerability.types.VulnerabilityType;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is the response bean for the attack vector annotation. Provides
 * the hint for the vulnerability level.
 * 
 * Note: As these beans are returned to UI so they expects that
 * all the labels/property keys are resolved to their respective values.
 * Like here {@code curlPayload} is a property key stored in {@code classpath:attackvectors/*properties}
 * and {@code description} is label which is stored in {@code i18n}
 * 
 * @author KSASAN preetkaran20@gmail.com
 */
public class AttackVectorResponseBean {

	@JsonProperty("VulnerabilityTypes")	
	private VulnerabilityType[] vulnerabilityTypes;
	
	@JsonProperty("CurlPayload")
	private String curlPayload;
	
	@JsonProperty("Description")
	private String description;

	public AttackVectorResponseBean(VulnerabilityType[] vulnerabilityTypes, String curlPayload, String description) {
		super();
		this.vulnerabilityTypes = vulnerabilityTypes;
		this.curlPayload = curlPayload;
		this.description = description;
	}

	public VulnerabilityType[] getVulnerabilityTypes() {
		return vulnerabilityTypes;
	}

	public String getCurlPayload() {
		return curlPayload;
	}
	
	public String getDescription() {
		return description;
	}
}
