package org.sasanlabs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.sasanlabs.vulnerability.types.VulnerabilityType;

/**
 * This class is the response bean for the attack vector annotation. Provides the hint for the
 * vulnerability level.
 *
 * <p>Note: As these beans are returned to UI so they expects that all the labels/property keys are
 * resolved to their respective values. Like here {@code curlPayload} is a property key stored in
 * {@code classpath:attackvectors/*properties} and {@code description} is label which is stored in
 * {@code i18n}
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public class AttackVectorResponseBean {

    @JsonProperty("VulnerabilityTypes")
    private final List<VulnerabilityType> vulnerabilityTypes;

    @JsonProperty("CurlPayload")
    private final String curlPayload;

    @JsonProperty("Description")
    private final String description;

    @JsonProperty("Source")
    private final String source;

    @JsonProperty("Solution")
    private final String solution;

    @JsonProperty("Reference")
    private final String reference;

    public AttackVectorResponseBean(
            List<VulnerabilityType> vulnerabilityTypes, String curlPayload, String description) {
        this(
                new Builder()
                        .vulnerabilityTypes(vulnerabilityTypes)
                        .curlPayload(curlPayload)
                        .description(description));
    }

    public AttackVectorResponseBean(
            List<VulnerabilityType> vulnerabilityTypes,
            String curlPayload,
            String description,
            String source,
            String solution,
            String reference) {
        this(
                new Builder()
                        .vulnerabilityTypes(vulnerabilityTypes)
                        .curlPayload(curlPayload)
                        .description(description)
                        .source(source)
                        .solution(solution)
                        .reference(reference));
    }

    private AttackVectorResponseBean(Builder builder) {
        this.vulnerabilityTypes = builder.vulnerabilityTypes;
        this.curlPayload = builder.curlPayload;
        this.description = builder.description;
        this.source = builder.source;
        this.solution = builder.solution;
        this.reference = builder.reference;
    }

    public List<VulnerabilityType> getVulnerabilityTypes() {
        return vulnerabilityTypes;
    }

    public String getCurlPayload() {
        return curlPayload;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    public String getSolution() {
        return solution;
    }

    public String getReference() {
        return reference;
    }

    public static class Builder {

        private List<VulnerabilityType> vulnerabilityTypes;
        private String curlPayload;
        private String description;
        private String source;
        private String solution;
        private String reference;

        public Builder vulnerabilityTypes(List<VulnerabilityType> vulnerabilityTypes) {
            this.vulnerabilityTypes = vulnerabilityTypes;
            return this;
        }

        public Builder curlPayload(String curlPayload) {
            this.curlPayload = curlPayload;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder source(String source) {
            this.source = source;
            return this;
        }

        public Builder solution(String solution) {
            this.solution = solution;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public AttackVectorResponseBean build() {
            return new AttackVectorResponseBean(this);
        }
    }
}
