package org.sasanlabs.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.sasanlabs.internal.utility.LevelEnum;
import org.sasanlabs.internal.utility.annotations.RequestParameterLocation;
import org.springframework.http.HttpMethod;

/** @author KSASAN preetkaran20@gmail.com */
public class LevelResponseBean implements Comparable<LevelResponseBean> {

    @JsonProperty("Level")
    private LevelEnum levelEnum;

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
    private HttpMethod httpMethod;

    @JsonProperty("AttackVectors")
    private List<AttackVectorResponseBean> attackVectorResponseBeans = new ArrayList<>();

    public LevelEnum getLevelEnum() {
        return levelEnum;
    }

    public void setLevelEnum(LevelEnum levelEnum) {
        this.levelEnum = levelEnum;
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

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
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
        return this.levelEnum.ordinal() - levelResponseBean.levelEnum.ordinal();
    }
}
