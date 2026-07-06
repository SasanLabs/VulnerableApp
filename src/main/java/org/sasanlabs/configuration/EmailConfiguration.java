package org.sasanlabs.configuration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vulnerableapp.email")
public class EmailConfiguration {

    public static final String DEFAULT_FROM = "no-reply@vulnerableapp.local";
    public static final String DEFAULT_BASE_URL = "http://localhost";

    private String from;

    private String baseUrl;

    public String getFrom() {
        return StringUtils.defaultIfBlank(from, DEFAULT_FROM);
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBaseUrl() {
        return StringUtils.defaultIfBlank(baseUrl, DEFAULT_BASE_URL);
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void validate() {
        // Missing values fall back to defaults so email support remains optional.
    }
}
