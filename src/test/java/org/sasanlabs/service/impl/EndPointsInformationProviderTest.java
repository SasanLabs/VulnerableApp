package org.sasanlabs.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.LevelResponseBean;
import org.sasanlabs.configuration.VulnerableAppProperties;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.internal.utility.Variant;
import org.sasanlabs.service.vulnerability.deserialization.InsecureDeserializationVulnerability;
import org.sasanlabs.service.vulnerability.websocket.WebSocketVulnerability;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.sasanlabs.vulnerableapp.facade.schema.VulnerabilityDefinition;
import org.sasanlabs.vulnerableapp.facade.schema.VulnerabilityLevelDefinition;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.bind.annotation.RequestMethod;

class EndPointsInformationProviderTest {

    private EndPointsInformationProvider provider;

    // Same attack vector payload files as the application.
    private static Properties attackVectorProperties() throws IOException {
        Properties properties = new Properties();
        for (Resource resource :
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:/attackvectors/*.properties")) {
            properties.putAll(PropertiesLoaderUtils.loadProperties(resource));
        }
        return properties;
    }

    @BeforeEach
    void setUp() throws IOException {
        // Same message source as the application, so a missing message key fails the test.
        ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");

        EnvUtils envUtils = mock(EnvUtils.class);
        Map<String, Object> restControllers = new LinkedHashMap<>();
        restControllers.put("InsecureDeserialization", new InsecureDeserializationVulnerability());
        when(envUtils.getAllClassesAnnotatedWithVulnerableAppRestController())
                .thenReturn(restControllers);
        when(envUtils.getAllClassesAnnotatedWithVulnerableAppWebSocketController())
                .thenReturn(Map.of("WebSocketVulnerability", new WebSocketVulnerability()));

        provider =
                new EndPointsInformationProvider(
                        envUtils,
                        new MessageBundle(messageSource),
                        new VulnerableAppProperties(attackVectorProperties()),
                        9090);
    }

    private AllEndPointsResponseBean endPoint(String name) throws Exception {
        return provider.getSupportedEndPoints().stream()
                .filter(endPoint -> name.equals(endPoint.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(name + " is not listed"));
    }

    private LevelResponseBean level(AllEndPointsResponseBean endPoint, String level) {
        return endPoint.getLevelDescriptionSet().stream()
                .filter(l -> level.equals(l.getLevel()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(level + " is not listed"));
    }

    @Test
    void getSupportedEndPoints_listsRestAndWebSocketVulnerabilities() throws Exception {
        List<AllEndPointsResponseBean> endPoints = provider.getSupportedEndPoints();

        assertThat(endPoints)
                .extracting(AllEndPointsResponseBean::getName)
                .containsExactlyInAnyOrder("InsecureDeserialization", "WebSocketVulnerability");
    }

    @Test
    void getSupportedEndPoints_keepsListingEveryAttackVectorOfARestLevel() throws Exception {
        LevelResponseBean level2 = level(endPoint("InsecureDeserialization"), "LEVEL_2");

        assertThat(level2.getAttackVectorResponseBeans()).hasSize(2);
        assertThat(level2.getChallengeCards()).hasSize(1);
        assertThat(level2.getVariant()).isEqualTo(Variant.UNSECURE);
        assertThat(level2.getRequestMethod()).isEqualTo(RequestMethod.GET);
    }

    @Test
    void getSupportedEndPoints_describesAWebSocketLevelFromItsAnnotations() throws Exception {
        AllEndPointsResponseBean webSocket = endPoint("WebSocketVulnerability");
        LevelResponseBean level1 = level(webSocket, "LEVEL_1");

        assertThat(webSocket.getDescription()).contains("WebSocket");
        assertThat(level1.getHtmlTemplate()).isEqualTo("LEVEL_1/WebSocket");
        assertThat(level1.getVariant()).isEqualTo(Variant.UNSECURE);
        assertThat(level1.getRequestMethod()).isEqualTo(RequestMethod.GET);
        assertThat(level1.getAttackVectorResponseBeans()).hasSize(1);
        assertThat(level1.getAttackVectorResponseBeans().get(0).getVulnerabilityTypes())
                .containsExactly(VulnerabilityType.MISSING_AUTHENTICATION);
        assertThat(level1.getChallengeCards()).hasSize(1);
        assertThat(level1.getChallengeCards().get(0).getHints()).hasSize(2);
        assertThat(level1.getChallengeCards().get(0).getPayload().getValue())
                .isEqualTo("{\"action\":\"LIST_CUSTOMERS\"}");
    }

    @Test
    void getSupportedEndPoints_listsEveryWebSocketLevelWithItsOwnAttackVector() throws Exception {
        AllEndPointsResponseBean webSocket = endPoint("WebSocketVulnerability");

        assertThat(webSocket.getLevelDescriptionSet())
                .extracting(LevelResponseBean::getLevel)
                .containsExactlyInAnyOrder("LEVEL_1", "LEVEL_2", "LEVEL_3");
        assertThat(
                        level(webSocket, "LEVEL_2")
                                .getAttackVectorResponseBeans()
                                .get(0)
                                .getVulnerabilityTypes())
                .containsExactly(VulnerabilityType.PERSISTENT_XSS);
        assertThat(
                        level(webSocket, "LEVEL_3")
                                .getAttackVectorResponseBeans()
                                .get(0)
                                .getVulnerabilityTypes())
                .containsExactly(VulnerabilityType.CROSS_SITE_WEBSOCKET_HIJACKING);
        for (LevelResponseBean level : webSocket.getLevelDescriptionSet()) {
            assertThat(level.getChallengeCards()).hasSize(1);
        }
    }

    @Test
    void getVulnerabilityDefinitions_pointsWebSocketLevelsToTheirTemplates() throws Exception {
        VulnerabilityDefinition definition =
                provider.getVulnerabilityDefinitions().stream()
                        .filter(d -> "WebSocketVulnerability".equals(d.getName()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("WebSocketVulnerability missing"));

        assertThat(definition.getLevelDescriptionSet()).hasSize(3);
        for (VulnerabilityLevelDefinition level : definition.getLevelDescriptionSet()) {
            assertThat(level.getResourceInformation().getHtmlResource().getUri())
                    .isEqualTo(
                            "/VulnerableApp/templates/WebSocketVulnerability/"
                                    + level.getLevel()
                                    + "/WebSocket.html");
            assertThat(level.getHints()).hasSize(1);
            assertThat(level.getChallengeCards()).hasSize(1);
        }
    }

    @Test
    void getScannerRelatedEndPointInformation_includesTheWebSocketLevel() throws Exception {
        assertThat(provider.getScannerRelatedEndPointInformation("http://localhost:9090/"))
                .anySatisfy(
                        scanner ->
                                assertThat(scanner.getUrl())
                                        .isEqualTo(
                                                "http://localhost:9090/WebSocketVulnerability/LEVEL_1"));
    }

    private EndPointsInformationProvider providerWith(
            Map<String, Object> restBeans, Map<String, Object> webSocketBeans, MessageBundle bundle)
            throws IOException {
        EnvUtils envUtils = mock(EnvUtils.class);
        when(envUtils.getAllClassesAnnotatedWithVulnerableAppRestController())
                .thenReturn(restBeans);
        when(envUtils.getAllClassesAnnotatedWithVulnerableAppWebSocketController())
                .thenReturn(webSocketBeans);
        return new EndPointsInformationProvider(
                envUtils, bundle, new VulnerableAppProperties(new Properties()), 9090);
    }

    @Test
    void getSupportedEndPoints_ignoresBeansThatAreNotAnnotated() throws Exception {
        MessageBundle bundle = mock(MessageBundle.class);
        when(bundle.getString(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("text");
        EndPointsInformationProvider plain =
                providerWith(Map.of("Plain", new Object()), Map.of("Plain", new Object()), bundle);

        assertThat(plain.getSupportedEndPoints()).isEmpty();
        assertThat(plain.getVulnerabilityDefinitions()).isEmpty();
    }

    @Test
    void getSupportedEndPoints_fallsBackWhenAPayloadHasNoText() throws Exception {
        MessageBundle bundle = mock(MessageBundle.class);
        when(bundle.getString(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("");
        EndPointsInformationProvider withoutTexts =
                providerWith(
                        Map.of(),
                        Map.of("WebSocketVulnerability", new WebSocketVulnerability()),
                        bundle);

        LevelResponseBean level1 = level(withoutTexts.getSupportedEndPoints().get(0), "LEVEL_1");

        assertThat(level1.getChallengeCards().get(0).getPayload().getValue())
                .isEqualTo("Payload is not applicable.");
    }
}
