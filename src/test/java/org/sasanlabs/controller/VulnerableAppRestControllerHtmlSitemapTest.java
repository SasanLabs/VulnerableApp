package org.sasanlabs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.LevelResponseBean;
import org.sasanlabs.benchmark.service.IExpectedIssuesProvider;
import org.sasanlabs.internal.utility.Protocol;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Covers {@code /sitemap.html}, the HTML sitemap served for crawlers that follow {@code <a href>}
 * links but do not parse {@code /sitemap.xml}.
 *
 * <p>Driven through {@link MockMvc} so that the request mapping and the response content type, both
 * of which decide whether a crawler treats the page as HTML at all, are covered too.
 */
@ExtendWith(MockitoExtension.class)
class VulnerableAppRestControllerHtmlSitemapTest {

    private static final String CONTEXT_PATH = "/VulnerableApp";

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
    void sitemapIsServedAsUtf8Html() throws Exception {
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get(CONTEXT_PATH + "/sitemap.html").contextPath(CONTEXT_PATH))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html;charset=UTF-8"));
    }

    @Test
    void everyHttpLevelIsLinkedAndGroupedUnderItsVulnerability() throws Exception {
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(
                        Arrays.asList(
                                endPoint(
                                        "SQLInjection",
                                        level("LEVEL_1", Protocol.HTTP),
                                        level("LEVEL_2", Protocol.HTTP)),
                                endPoint(
                                        "XSSWithHtmlTagInjection",
                                        level("LEVEL_1", Protocol.HTTP))));

        String sitemap = sitemapServedUnder(CONTEXT_PATH);

        assertThat(sitemap)
                .startsWith("<!DOCTYPE html>")
                .containsSubsequence(
                        "<h2>SQLInjection</h2>",
                        "<a href=\"/VulnerableApp/SQLInjection/LEVEL_1\">SQLInjection/LEVEL_1</a>",
                        "<a href=\"/VulnerableApp/SQLInjection/LEVEL_2\">SQLInjection/LEVEL_2</a>",
                        "<h2>XSSWithHtmlTagInjection</h2>",
                        "<a href=\"/VulnerableApp/XSSWithHtmlTagInjection/LEVEL_1\">"
                                + "XSSWithHtmlTagInjection/LEVEL_1</a>")
                .endsWith("</html>\n");
    }

    /**
     * The XML sitemap needs absolute URLs, the HTML one does not: a root-relative link is resolved
     * by the crawler against the host it actually fetched the page from, so the scheme, host and
     * port the application sees behind a proxy must not leak into the links.
     */
    @Test
    void linksAreRootRelativeAndCarryNoHost() throws Exception {
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(
                        Collections.singletonList(
                                endPoint("SQLInjection", level("LEVEL_1", Protocol.HTTP))));

        String sitemap =
                mockMvc.perform(
                                get(CONTEXT_PATH + "/sitemap.html")
                                        .contextPath(CONTEXT_PATH)
                                        .with(
                                                request -> {
                                                    request.setServerName("internal-host");
                                                    request.setServerPort(9090);
                                                    return request;
                                                }))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(sitemap)
                .contains("href=\"/VulnerableApp/SQLInjection/LEVEL_1\"")
                .doesNotContain("internal-host")
                .doesNotContain("9090");
    }

    @Test
    void linksFollowACustomContextPath() throws Exception {
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(
                        Collections.singletonList(
                                endPoint("SQLInjection", level("LEVEL_1", Protocol.HTTP))));

        assertThat(sitemapServedUnder("/customCtx"))
                .contains("href=\"/customCtx/SQLInjection/LEVEL_1\"")
                .doesNotContain(CONTEXT_PATH);
    }

    @Test
    void rootDeploymentGetsLinksWithNoContextSegment() throws Exception {
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(
                        Collections.singletonList(
                                endPoint("SQLInjection", level("LEVEL_1", Protocol.HTTP))));

        String sitemap =
                mockMvc.perform(get("/sitemap.html"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(sitemap).contains("href=\"/SQLInjection/LEVEL_1\"").doesNotContain(CONTEXT_PATH);
    }

    /**
     * A crawler following a link would only send a plain GET to a WebSocket handshake URL, so
     * WebSocket levels are not linked, and a vulnerability left with no HTTP level gets no empty
     * heading.
     */
    @Test
    void webSocketLevelsAreNotLinked() throws Exception {
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(
                        Arrays.asList(
                                endPoint(
                                        "SQLInjection",
                                        level("LEVEL_1", Protocol.HTTP),
                                        level("LEVEL_2", Protocol.WS)),
                                endPoint("WebSocketVulnerability", level("LEVEL_1", Protocol.WS))));

        assertThat(sitemapServedUnder(CONTEXT_PATH))
                .contains("href=\"/VulnerableApp/SQLInjection/LEVEL_1\"")
                .doesNotContain("SQLInjection/LEVEL_2")
                .doesNotContain("WebSocketVulnerability");
    }

    @Test
    void namesAreHtmlEscaped() throws Exception {
        when(endPointsInformationProvider.getSupportedEndPoints())
                .thenReturn(
                        Collections.singletonList(
                                endPoint("<b>\"Vuln\"", level("LEVEL_1", Protocol.HTTP))));

        assertThat(sitemapServedUnder(CONTEXT_PATH))
                .contains("<h2>&lt;b&gt;&quot;Vuln&quot;</h2>")
                .contains(
                        "<a href=\"/VulnerableApp/&lt;b&gt;&quot;Vuln&quot;/LEVEL_1\">"
                                + "&lt;b&gt;&quot;Vuln&quot;/LEVEL_1</a>")
                .doesNotContain("<b>");
    }

    @Test
    void sitemapOnlyAnswersGet() throws Exception {
        mockMvc.perform(post(CONTEXT_PATH + "/sitemap.html").contextPath(CONTEXT_PATH))
                .andExpect(status().isMethodNotAllowed());
    }

    private String sitemapServedUnder(String contextPath) throws Exception {
        return mockMvc.perform(get(contextPath + "/sitemap.html").contextPath(contextPath))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private static LevelResponseBean level(String name, Protocol protocol) {
        LevelResponseBean level = new LevelResponseBean();
        level.setLevel(name);
        level.setProtocol(protocol);
        return level;
    }

    private static AllEndPointsResponseBean endPoint(String name, LevelResponseBean... levels) {
        Set<LevelResponseBean> levelSet = new LinkedHashSet<>(Arrays.asList(levels));
        AllEndPointsResponseBean endPoint = new AllEndPointsResponseBean();
        endPoint.setName(name);
        endPoint.setLevelDescriptionSet(levelSet);
        return endPoint;
    }
}
