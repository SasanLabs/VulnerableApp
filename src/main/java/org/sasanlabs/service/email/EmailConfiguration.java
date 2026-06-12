package org.sasanlabs.service.email;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vulnerableapp.email")
public class EmailConfiguration implements InitializingBean {

    private String from;

    private String baseUrl;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void afterPropertiesSet() {
        validate();
    }

    public void validate() {
        requireText(from, "vulnerableapp.email.from");
        requireText(baseUrl, "vulnerableapp.email.base-url");
    }

    private void requireText(String value, String propertyName) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalStateException(propertyName + " must be configured");
        }
    }
}
