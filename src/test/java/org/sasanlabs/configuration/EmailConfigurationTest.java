package org.sasanlabs.configuration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmailConfigurationTest {

    @Test
    void shouldFallBackToDefaultsWhenNotConfigured() {
        EmailConfiguration emailConfiguration = new EmailConfiguration();

        assertEquals(EmailConfiguration.DEFAULT_FROM, emailConfiguration.getFrom());
        assertEquals(EmailConfiguration.DEFAULT_BASE_URL, emailConfiguration.getBaseUrl());
    }

    @Test
    void shouldReturnConfiguredValuesWhenSet() {
        EmailConfiguration emailConfiguration = new EmailConfiguration();
        emailConfiguration.setFrom("custom@vulnerableapp.local");
        emailConfiguration.setBaseUrl("http://custom-host");

        assertEquals("custom@vulnerableapp.local", emailConfiguration.getFrom());
        assertEquals("http://custom-host", emailConfiguration.getBaseUrl());
    }

    @Test
    void shouldNotThrowOnValidate() {
        EmailConfiguration emailConfiguration = new EmailConfiguration();

        assertDoesNotThrow(emailConfiguration::validate);
    }
}