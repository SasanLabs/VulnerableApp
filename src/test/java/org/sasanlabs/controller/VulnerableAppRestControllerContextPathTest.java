package org.sasanlabs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.LevelResponseBean;
import org.sasanlabs.benchmark.service.IExpectedIssuesProvider;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The URLs handed to scanners must stay inside the context path the application is actually served
 * under, rather than a hardcoded {@code /VulnerableApp}.
 *
 * <p>{@code server.servlet.context-path} is configurable, and nothing in the repository overrides
 * the default, so this shows up only where that setting is overridden. These tests serve the
 * controller under three context paths, a custom one, the shipped default and none at all, and
 * assert that neither the scanner URL nor the sitemap {@code <loc>} entries point outside the
 * deployment.
 */
@ExtendWith(MockitoExtension.class)
class VulnerableAppRestControllerContextPathTest {

    /** A deployment served somewhere other than the shipped default. */
    private static final String CUSTOM_CONTEXT_PATH = "/customCtx";

    /**
     * The context path {@code application.properties} ships, and the value both endpoints used to
     * hardcode. A deployment served elsewhere must not fall back to it, and a deployment served
     * under it must still produce it.
     */
    private static final String DEFAULT_CONTEXT_PATH = "/VulnerableApp";

    @Mock private IEndPointsInformationProvider endPointsInformationProvider;

    @Mock private IExpectedIssuesProvider expectedIssuesProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new VulnerableAppRestController(
                                        endPointsInformationProvider, expectedIssuesProvider))
                        .build();
    }

    @Test
    void scannerUrlIsBuiltFromTheRequestContextPath() throws Exception {
        when(endPointsInformationProvider.getScannerRelatedEndPointInformation(anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(
                get(CUSTOM_CONTEXT_PATH + "/scanner")
                        .contextPath(CUSTOM_CONTEXT_PATH)
                        .with(servedOverHttps()));

        ArgumentCaptor<String> appUrl = ArgumentCaptor.forClass(String.class);
        verify(endPointsInformationProvider).getScannerRelatedEndPointInformation(appUrl.capture());

        assertThat(appUrl.getValue())
                .as("the scanner base URL must sit under the served context path")
                .isEqualTo("https://10.0.0.5:443" + CUSTOM_CONTEXT_PATH + "/")
                .doesNotContain(DEFAULT_CONTEXT_PATH);
    }

    @Test
    void sitemapLocEntriesAreBuiltFromTheRequestContextPath() throws Exception {
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(Collections.singletonList(sqlInjectionEndPoint()));

        String sitemap =
                mockMvc.perform(
                                get(CUSTOM_CONTEXT_PATH + "/sitemap.xml")
                                        .contextPath(CUSTOM_CONTEXT_PATH)
                                        .with(servedOverHttps()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(sitemap)
                .as("every <loc> must sit under the served context path")
                .contains("https://10.0.0.5:443" + CUSTOM_CONTEXT_PATH + "/SQLInjection/LEVEL_1")
                .doesNotContain(DEFAULT_CONTEXT_PATH);
    }

    /**
     * The shipped configuration serves the application under {@code /VulnerableApp}, so that
     * deployment's URLs have to come out exactly as they did before. This case passes on unmodified
     * master too; it is here to pin behaviour that must not change, not to demonstrate the defect.
     */
    @Test
    void defaultContextPathStillProducesTheVulnerableAppSegment() throws Exception {
        when(endPointsInformationProvider.getScannerRelatedEndPointInformation(anyString()))
                .thenReturn(Collections.emptyList());
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(Collections.singletonList(sqlInjectionEndPoint()));

        mockMvc.perform(
                get(DEFAULT_CONTEXT_PATH + "/scanner")
                        .contextPath(DEFAULT_CONTEXT_PATH)
                        .with(servedOverHttps()));

        ArgumentCaptor<String> appUrl = ArgumentCaptor.forClass(String.class);
        verify(endPointsInformationProvider).getScannerRelatedEndPointInformation(appUrl.capture());
        assertThat(appUrl.getValue())
                .isEqualTo("https://10.0.0.5:443" + DEFAULT_CONTEXT_PATH + "/");

        String sitemap =
                mockMvc.perform(
                                get(DEFAULT_CONTEXT_PATH + "/sitemap.xml")
                                        .contextPath(DEFAULT_CONTEXT_PATH)
                                        .with(servedOverHttps()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(sitemap)
                .contains("https://10.0.0.5:443" + DEFAULT_CONTEXT_PATH + "/SQLInjection/LEVEL_1");
    }

    /**
     * A root deployment has an empty context path. The URLs must then carry no path segment at all
     * rather than falling back to the old hardcoded one.
     */
    @Test
    void rootDeploymentGetsUrlsWithNoContextSegment() throws Exception {
        when(endPointsInformationProvider.getScannerRelatedEndPointInformation(anyString()))
                .thenReturn(Collections.emptyList());
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(Collections.singletonList(sqlInjectionEndPoint()));

        mockMvc.perform(get("/scanner").with(servedOverHttps()));

        ArgumentCaptor<String> appUrl = ArgumentCaptor.forClass(String.class);
        verify(endPointsInformationProvider).getScannerRelatedEndPointInformation(appUrl.capture());
        assertThat(appUrl.getValue()).isEqualTo("https://10.0.0.5:443/");

        String sitemap =
                mockMvc.perform(get("/sitemap.xml").with(servedOverHttps()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(sitemap)
                .contains("https://10.0.0.5:443/SQLInjection/LEVEL_1")
                .doesNotContain(DEFAULT_CONTEXT_PATH);
    }

    /**
     * Spring 5's request builder has no {@code scheme()}/{@code serverName()} setters, so the host
     * half of the URL is pinned on the underlying {@link
     * org.springframework.mock.web.MockHttpServletRequest} instead. Fixed values keep the
     * assertions exact.
     */
    private static RequestPostProcessor servedOverHttps() {
        return request -> {
            request.setScheme("https");
            request.setServerName("10.0.0.5");
            request.setServerPort(443);
            return request;
        };
    }

    /**
     * The controller emits one {@code <loc>} per level, so a single endpoint carrying a single
     * level is enough to pin the URL shape.
     */
    private static AllEndPointsResponseBean sqlInjectionEndPoint() {
        LevelResponseBean level = new LevelResponseBean();
        level.setLevel("LEVEL_1");
        Set<LevelResponseBean> levels = new LinkedHashSet<>();
        levels.add(level);

        AllEndPointsResponseBean endPoint = new AllEndPointsResponseBean();
        endPoint.setName("SQLInjection");
        endPoint.setLevelDescriptionSet(levels);
        return endPoint;
    }
}
