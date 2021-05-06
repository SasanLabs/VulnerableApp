package org.sasanlabs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.sasanlabs.internal.utility.LevelConstants;
import org.sasanlabs.internal.utility.Variant;
import org.sasanlabs.internal.utility.annotations.RequestParameterLocation;
import org.springframework.web.bind.annotation.RequestMethod;

/** @author KSASAN preetkaran20@gmail.com */
public class LevelResponseBean implements Comparable<LevelResponseBean> {

    @JsonProperty("Level")
    private String level;

    @JsonProperty("Variant")
    private Variant variant;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("HtmlTemplate")
    private String htmlTemplate;

    @JsonProperty("Location")
    private RequestParameterLocation requestParameterLocation;

    @JsonProperty("Parameter")
    private String parameterName;

    @JsonProperty("SampleValues")
    private String[] sampleValues;

    @JsonProperty("HttpMethod")
    private RequestMethod requestMethod;

    @JsonProperty("AttackVectors")
    private List<AttackVectorResponseBean> attackVectorResponseBeans = new ArrayList<>();

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Variant getVariant() {
        return variant;
    }

    public void setVariant(Variant variant) {
        this.variant = variant;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlTemplate() {
        return htmlTemplate;
    }

    public void setHtmlTemplate(String htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
    }

    public RequestParameterLocation getRequestParameterLocation() {
        return requestParameterLocation;
    }

    public void setRequestParameterLocation(RequestParameterLocation requestParameterLocation) {
        this.requestParameterLocation = requestParameterLocation;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String[] getSampleValues() {
        return sampleValues;
    }

    public void setSampleValues(String[] sampleValues) {
        this.sampleValues = sampleValues;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(RequestMethod httpMethod) {
        this.requestMethod = httpMethod;
    }

    public List<AttackVectorResponseBean> getAttackVectorResponseBeans() {
        return attackVectorResponseBeans;
    }

    public void setAttackVectorResponseBeans(
            List<AttackVectorResponseBean> attackVectorResponseBeans) {
        this.attackVectorResponseBeans = attackVectorResponseBeans;
    }

    @Override
    public int compareTo(LevelResponseBean levelResponseBean) {
        return LevelConstants.getOrdinal(this.level)
                - LevelConstants.getOrdinal(levelResponseBean.getLevel());
    }
}
