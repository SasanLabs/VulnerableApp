package org.sasanlabs.service.impl;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sasanlabs.benchmark.service.IExpectedIssuesProvider;
import org.sasanlabs.configuration.VulnerableAppProperties;
import org.sasanlabs.controller.VulnerableAppRestController;
import org.sasanlabs.controller.exception.ControllerExceptionHandler;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.LevelConstants;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.internal.utility.Variant;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRequestMapping;
import org.sasanlabs.service.vulnerability.csrf.CSRFSessionService;
import org.sasanlabs.service.vulnerability.csrf.CSRFVulnerability;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;

class CatalogMappingIntegrationTest {

    private MockMvc mockMvc;
    private Map<String, Object> controllers;

    @BeforeEach
    void setUp() {
        EnvUtils envUtils = mock(EnvUtils.class);
        controllers = new LinkedHashMap<>();
        controllers.put("InvalidFixture", new InvalidFixture());
        controllers.put("HealthyFixture", new HealthyFixture());
        controllers.put("OverflowFixture", new OverflowFixture());
        when(envUtils.getAllClassesAnnotatedWithVulnerableAppRestController())
                .thenReturn(controllers);

        MessageBundle messageBundle = mock(MessageBundle.class);
        EndPointsInformationProvider provider =
                new EndPointsInformationProvider(
                        envUtils, messageBundle, mock(VulnerableAppProperties.class), 9090);
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new VulnerableAppRestController(
                                        provider, mock(IExpectedIssuesProvider.class)))
                        .setControllerAdvice(new ControllerExceptionHandler(messageBundle))
                        .build();
    }

    @Test
    void legacyCatalogKeepsHealthyModulesWhenAnotherModuleHasAnInvalidMapping() throws Exception {
        mockMvc.perform(get("/allEndPointJson"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].Name").value(contains("HealthyFixture")))
                .andExpect(
                        jsonPath("$[0]['Detailed Information'][*].Level")
                                .value(contains("LEVEL_2", "LEVEL_10")));
    }

    @Test
    void facadeCatalogKeepsHealthyModulesWhenAnotherModuleHasAnInvalidMapping() throws Exception {
        mockMvc.perform(get("/VulnerabilityDefinitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains("HealthyFixture")));
    }

    @Test
    void catalogsListOnlyTheSecureCsrfLevel() throws Exception {
        controllers.put("CSRFVulnerability", new CSRFVulnerability(mock(CSRFSessionService.class)));

        mockMvc.perform(get("/allEndPointJson"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[*].Name")
                                .value(contains("HealthyFixture", "CSRFVulnerability")))
                .andExpect(
                        jsonPath("$[1]['Detailed Information'][*].Level")
                                .value(contains("LEVEL_5")))
                .andExpect(
                        jsonPath("$[1]['Detailed Information'][*].Variant")
                                .value(contains("SECURE")));

        mockMvc.perform(get("/VulnerabilityDefinitions"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[*].id").value(contains("HealthyFixture", "CSRFVulnerability")))
                .andExpect(jsonPath("$[1].levels[*].levelIdentifier").value(contains("LEVEL_5")))
                .andExpect(jsonPath("$[1].levels[*].variant").value(contains("SECURE")));
    }

    @org.sasanlabs.internal.utility.annotations.VulnerableAppRestController(
            value = "InvalidFixture",
            descriptionLabel = "fixture.invalid")
    private static class InvalidFixture {

        @VulnerableAppRequestMapping(value = LevelConstants.LEVEL_1, variant = Variant.SECURE)
        public void level() {}

        @VulnerableAppRequestMapping(value = "LEVEL_1/details", variant = Variant.SECURE)
        public void details() {}
    }

    @org.sasanlabs.internal.utility.annotations.VulnerableAppRestController(
            value = "OverflowFixture",
            descriptionLabel = "fixture.overflow")
    private static class OverflowFixture {

        @VulnerableAppRequestMapping(value = "LEVEL_2147483648", variant = Variant.SECURE)
        public void level() {}
    }

    @org.sasanlabs.internal.utility.annotations.VulnerableAppRestController(
            value = "HealthyFixture",
            descriptionLabel = "fixture.healthy")
    private static class HealthyFixture {

        @VulnerableAppRequestMapping(value = LevelConstants.LEVEL_10, variant = Variant.SECURE)
        public void levelTen() {}

        @VulnerableAppRequestMapping(value = LevelConstants.LEVEL_2, variant = Variant.SECURE)
        public void levelTwo() {}

        @GetMapping("details")
        public void details() {}
    }
}
