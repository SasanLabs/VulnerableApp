package org.sasanlabs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.AttackVectorResponseBean;
import org.sasanlabs.beans.ChallengeCardResponseBean;
import org.sasanlabs.beans.LevelResponseBean;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.configuration.VulnerableAppProperties;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.FrameworkConstants;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.internal.utility.annotations.AttackVector;
import org.sasanlabs.internal.utility.annotations.ChallengeCard;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRequestMapping;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRestController;
import org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketController;
import org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketMapping;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.sasanlabs.vulnerableapp.facade.schema.ChallengeCardHint;
import org.sasanlabs.vulnerableapp.facade.schema.ChallengeCardPayload;
import org.sasanlabs.vulnerableapp.facade.schema.ResourceInformation;
import org.sasanlabs.vulnerableapp.facade.schema.ResourceType;
import org.sasanlabs.vulnerableapp.facade.schema.ResourceURI;
import org.sasanlabs.vulnerableapp.facade.schema.Variant;
import org.sasanlabs.vulnerableapp.facade.schema.VulnerabilityDefinition;
import org.sasanlabs.vulnerableapp.facade.schema.VulnerabilityLevelDefinition;
import org.sasanlabs.vulnerableapp.facade.schema.VulnerabilityLevelHint;
import org.sasanlabs.vulnerableapp.facade.schema.VulnerabilityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author KSASAN preetkaran20@gmail.com
 */
@Service
public class EndPointsInformationProvider implements IEndPointsInformationProvider {

    private EnvUtils envUtils;

    private MessageBundle messageBundle;

    private VulnerableAppProperties vulnerableAppProperties;

    int port;

    public EndPointsInformationProvider(
            EnvUtils envUtils,
            MessageBundle messageBundle,
            VulnerableAppProperties vulnerableAppProperties,
            @Value("${server.port}") int port) {
        this.envUtils = envUtils;
        this.messageBundle = messageBundle;
        this.vulnerableAppProperties = vulnerableAppProperties;
        this.port = port;
    }

    @Override
    public List<AllEndPointsResponseBean> getSupportedEndPoints() throws JsonProcessingException {
        List<AllEndPointsResponseBean> allEndpoints = new ArrayList<>();
        for (Map.Entry<String, Object> entry :
                envUtils.getAllClassesAnnotatedWithVulnerableAppRestController().entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            VulnerableAppRestController restController =
                    clazz.getAnnotation(VulnerableAppRestController.class);
            if (restController != null) {
                AllEndPointsResponseBean allEndPointsResponseBean =
                        newAllEndPointsResponseBean(
                                entry.getKey(), restController.descriptionLabel());
                for (Method method : clazz.getDeclaredMethods()) {
                    VulnerableAppRequestMapping vulnLevel =
                            method.getAnnotation(VulnerableAppRequestMapping.class);
                    if (vulnLevel != null) {
                        allEndPointsResponseBean
                                .getLevelDescriptionSet()
                                .add(
                                        buildLevelResponseBean(
                                                method,
                                                vulnLevel.value(),
                                                vulnLevel.variant(),
                                                vulnLevel.htmlTemplate(),
                                                vulnLevel.requestMethod()));
                    }
                }
                allEndpoints.add(allEndPointsResponseBean);
            }
        }
        for (Map.Entry<String, Object> entry :
                envUtils.getAllClassesAnnotatedWithVulnerableAppWebSocketController().entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            VulnerableAppWebSocketController webSocketController =
                    clazz.getAnnotation(VulnerableAppWebSocketController.class);
            if (webSocketController != null) {
                AllEndPointsResponseBean allEndPointsResponseBean =
                        newAllEndPointsResponseBean(
                                entry.getKey(), webSocketController.descriptionLabel());
                for (Method method : clazz.getDeclaredMethods()) {
                    VulnerableAppWebSocketMapping vulnLevel =
                            method.getAnnotation(VulnerableAppWebSocketMapping.class);
                    if (vulnLevel != null) {
                        // A WebSocket is opened with a GET handshake request.
                        allEndPointsResponseBean
                                .getLevelDescriptionSet()
                                .add(
                                        buildLevelResponseBean(
                                                method,
                                                vulnLevel.value(),
                                                vulnLevel.variant(),
                                                vulnLevel.htmlTemplate(),
                                                RequestMethod.GET));
                    }
                }
                allEndpoints.add(allEndPointsResponseBean);
            }
        }
        return allEndpoints;
    }

    private AllEndPointsResponseBean newAllEndPointsResponseBean(
            String name, String descriptionLabel) {
        AllEndPointsResponseBean allEndPointsResponseBean = new AllEndPointsResponseBean();
        allEndPointsResponseBean.setName(name);
        allEndPointsResponseBean.setDescription(messageBundle.getString(descriptionLabel, null));
        return allEndPointsResponseBean;
    }

    private LevelResponseBean buildLevelResponseBean(
            Method method,
            String level,
            org.sasanlabs.internal.utility.Variant variant,
            String htmlTemplate,
            RequestMethod requestMethod) {
        AttackVector[] attackVectors = method.getAnnotationsByType(AttackVector.class);
        LevelResponseBean levelResponseBean = new LevelResponseBean();
        levelResponseBean.setLevel(level);
        levelResponseBean.setVariant(variant);
        levelResponseBean.setHtmlTemplate(htmlTemplate);
        levelResponseBean.setRequestMethod(requestMethod);
        ChallengeCard[] challengeCards = method.getAnnotationsByType(ChallengeCard.class);
        for (ChallengeCard card : challengeCards) {
            List<ChallengeCardResponseBean.HintResponseBean> hintBeans = new ArrayList<>();
            for (ChallengeCard.Hint hint : card.hints()) {
                hintBeans.add(
                        new ChallengeCardResponseBean.HintResponseBean(
                                hint.order(), messageBundle.getString(hint.text(), null)));
            }
            String payload = getPayload(card.payload().value());
            ChallengeCardResponseBean.PayloadResponseBean payloadBean =
                    new ChallengeCardResponseBean.PayloadResponseBean(
                            messageBundle.getString(card.payload().description(), null), payload);

            levelResponseBean
                    .getChallengeCards()
                    .add(
                            new ChallengeCardResponseBean(
                                    messageBundle.getString(card.challengeText(), null),
                                    hintBeans,
                                    payloadBean));
        }
        for (AttackVector attackVector : attackVectors) {
            String payload = getPayload(attackVector.payload());
            levelResponseBean
                    .getAttackVectorResponseBeans()
                    .add(
                            new AttackVectorResponseBean(
                                    new ArrayList<>(
                                            Arrays.asList(attackVector.vulnerabilityExposed())),
                                    payload,
                                    messageBundle.getString(attackVector.description(), null)));
        }
        return levelResponseBean;
    }

    @Override
    public List<ScannerResponseBean> getScannerRelatedEndPointInformation(String appUrl)
            throws JsonProcessingException, UnknownHostException {
        List<AllEndPointsResponseBean> allEndPointsResponseBeans = this.getSupportedEndPoints();
        List<ScannerResponseBean> scannerResponseBeans = new ArrayList<>();
        for (AllEndPointsResponseBean allEndPointsResponseBean : allEndPointsResponseBeans) {
            for (LevelResponseBean levelResponseBean :
                    allEndPointsResponseBean.getLevelDescriptionSet()) {
                for (AttackVectorResponseBean attackVectorResponseBean :
                        levelResponseBean.getAttackVectorResponseBeans()) {
                    scannerResponseBeans.add(
                            new ScannerResponseBean(
                                    new StringBuilder()
                                            .append(appUrl)
                                            .append(allEndPointsResponseBean.getName())
                                            .append(FrameworkConstants.SLASH)
                                            .append(levelResponseBean.getLevel())
                                            .toString(),
                                    levelResponseBean.getVariant().toString(),
                                    levelResponseBean.getRequestMethod(),
                                    attackVectorResponseBean.getVulnerabilityTypes()));
                }
            }
        }
        return scannerResponseBeans;
    }

    private void addFacadeResourceInformation(
            VulnerabilityDefinition facadeVulnerabilityDefinition,
            VulnerabilityLevelDefinition facadeVulnerabilityLevelDefinition,
            String template) {
        ResourceInformation resourceInformation = new ResourceInformation();
        facadeVulnerabilityLevelDefinition.setResourceInformation(resourceInformation);
        resourceInformation.setStaticResources(
                Arrays.asList(
                        new ResourceURI(
                                false,
                                "/VulnerableApp/templates/"
                                        + facadeVulnerabilityDefinition.getName()
                                        + "/"
                                        + template
                                        + ".css",
                                ResourceType.CSS.name()),
                        new ResourceURI(
                                false,
                                "/VulnerableApp/templates/"
                                        + facadeVulnerabilityDefinition.getName()
                                        + "/"
                                        + template
                                        + ".js",
                                ResourceType.JAVASCRIPT.name())));
        resourceInformation.setHtmlResource(
                new ResourceURI(
                        false,
                        "/VulnerableApp/templates/"
                                + facadeVulnerabilityDefinition.getName()
                                + "/"
                                + template
                                + ".html"));
    }

    @Override
    public List<VulnerabilityDefinition> getVulnerabilityDefinitions()
            throws JsonProcessingException {
        List<VulnerabilityDefinition> vulnerabilityDefinitions = new ArrayList<>();
        for (Map.Entry<String, Object> entry :
                envUtils.getAllClassesAnnotatedWithVulnerableAppRestController().entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            VulnerableAppRestController restController =
                    clazz.getAnnotation(VulnerableAppRestController.class);
            if (restController != null) {
                VulnerabilityDefinition facadeVulnerabilityDefinition =
                        newVulnerabilityDefinition(
                                entry.getKey(), restController.descriptionLabel());
                for (Method method : clazz.getDeclaredMethods()) {
                    VulnerableAppRequestMapping vulnLevel =
                            method.getAnnotation(VulnerableAppRequestMapping.class);
                    if (vulnLevel != null) {
                        facadeVulnerabilityDefinition
                                .getLevelDescriptionSet()
                                .add(
                                        buildFacadeLevelDefinition(
                                                facadeVulnerabilityDefinition,
                                                method,
                                                vulnLevel.value(),
                                                vulnLevel.variant(),
                                                vulnLevel.htmlTemplate()));
                    }
                }
                vulnerabilityDefinitions.add(facadeVulnerabilityDefinition);
            }
        }
        for (Map.Entry<String, Object> entry :
                envUtils.getAllClassesAnnotatedWithVulnerableAppWebSocketController().entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            VulnerableAppWebSocketController webSocketController =
                    clazz.getAnnotation(VulnerableAppWebSocketController.class);
            if (webSocketController != null) {
                VulnerabilityDefinition facadeVulnerabilityDefinition =
                        newVulnerabilityDefinition(
                                entry.getKey(), webSocketController.descriptionLabel());
                for (Method method : clazz.getDeclaredMethods()) {
                    VulnerableAppWebSocketMapping vulnLevel =
                            method.getAnnotation(VulnerableAppWebSocketMapping.class);
                    if (vulnLevel != null) {
                        facadeVulnerabilityDefinition
                                .getLevelDescriptionSet()
                                .add(
                                        buildFacadeLevelDefinition(
                                                facadeVulnerabilityDefinition,
                                                method,
                                                vulnLevel.value(),
                                                vulnLevel.variant(),
                                                vulnLevel.htmlTemplate()));
                    }
                }
                vulnerabilityDefinitions.add(facadeVulnerabilityDefinition);
            }
        }
        return vulnerabilityDefinitions;
    }

    private VulnerabilityDefinition newVulnerabilityDefinition(
            String name, String descriptionLabel) {
        VulnerabilityDefinition facadeVulnerabilityDefinition = new VulnerabilityDefinition();
        facadeVulnerabilityDefinition.setName(name);
        facadeVulnerabilityDefinition.setId(name);
        facadeVulnerabilityDefinition.setDescription(
                messageBundle.getString(descriptionLabel, null));
        facadeVulnerabilityDefinition.setVulnerabilityTypes(new ArrayList<VulnerabilityType>());
        return facadeVulnerabilityDefinition;
    }

    private VulnerabilityLevelDefinition buildFacadeLevelDefinition(
            VulnerabilityDefinition facadeVulnerabilityDefinition,
            Method method,
            String level,
            org.sasanlabs.internal.utility.Variant variant,
            String htmlTemplate) {
        AttackVector[] attackVectors = method.getAnnotationsByType(AttackVector.class);
        VulnerabilityLevelDefinition facadeVulnerabilityLevelDefinition =
                new VulnerabilityLevelDefinition();
        facadeVulnerabilityLevelDefinition.setLevel(level);
        facadeVulnerabilityLevelDefinition.setVariant(Variant.valueOf(variant.name()));
        addFacadeResourceInformation(
                facadeVulnerabilityDefinition, facadeVulnerabilityLevelDefinition, htmlTemplate);

        ChallengeCard[] challengeCardAnnotations = method.getAnnotationsByType(ChallengeCard.class);
        List<org.sasanlabs.vulnerableapp.facade.schema.ChallengeCard> facadeChallengeCards =
                new ArrayList<>();

        for (ChallengeCard card : challengeCardAnnotations) {
            org.sasanlabs.vulnerableapp.facade.schema.ChallengeCard facadeChallenge =
                    new org.sasanlabs.vulnerableapp.facade.schema.ChallengeCard();

            // Set the Challenge Text
            facadeChallenge.setChallengeText(messageBundle.getString(card.challengeText(), null));

            // Map Hints
            List<ChallengeCardHint> facadeHints = new ArrayList<>();
            for (ChallengeCard.Hint hint : card.hints()) {
                ChallengeCardHint hintObj = new ChallengeCardHint();
                hintObj.setOrder(hint.order());
                hintObj.setText(messageBundle.getString(hint.text(), null));
                facadeHints.add(hintObj);
            }
            facadeChallenge.setHints(facadeHints);

            // Map Payload
            ChallengeCardPayload facadePayload = new ChallengeCardPayload();
            facadePayload.setDescription(
                    messageBundle.getString(card.payload().description(), null));
            String payload = getPayload(card.payload().value());
            facadePayload.setValue(payload);
            facadeChallenge.setPayload(facadePayload);

            facadeChallengeCards.add(facadeChallenge);
        }
        // Set the populated list into the facade level definition
        facadeVulnerabilityLevelDefinition.setChallengeCards(facadeChallengeCards);

        for (AttackVector attackVector : attackVectors) {
            List<VulnerabilityType> facadeLevelVulnerabilityTypes =
                    new ArrayList<VulnerabilityType>();
            org.sasanlabs.vulnerability.types.VulnerabilityType[] vulnerabilityTypes =
                    attackVector.vulnerabilityExposed();
            for (org.sasanlabs.vulnerability.types.VulnerabilityType vulnerabilityType :
                    vulnerabilityTypes) {
                facadeLevelVulnerabilityTypes.add(
                        new VulnerabilityType("Custom", vulnerabilityType.name()));
                if (null != vulnerabilityType.getCweID())
                    facadeLevelVulnerabilityTypes.add(
                            new VulnerabilityType(
                                    "CWE", String.valueOf(vulnerabilityType.getCweID())));
                if (null != vulnerabilityType.getWascID())
                    facadeLevelVulnerabilityTypes.add(
                            new VulnerabilityType(
                                    "WASC", String.valueOf(vulnerabilityType.getWascID())));
            }
            facadeVulnerabilityLevelDefinition
                    .getHints()
                    .add(
                            new VulnerabilityLevelHint(
                                    facadeLevelVulnerabilityTypes,
                                    buildFacadeHintDescription(attackVector)));
        }
        return facadeVulnerabilityLevelDefinition;
    }

    private String getPayload(String payloadKey) {
        String payload = vulnerableAppProperties.getAttackVectorProperty(payloadKey);
        if (StringUtils.isBlank(payload)) {
            payload = messageBundle.getString(payloadKey, null);
        }
        if (StringUtils.isBlank(payload)) {
            payload = "Payload is not applicable.";
        }
        return payload;
    }

    private String buildFacadeHintDescription(AttackVector attackVector) {
        String description = messageBundle.getString(attackVector.description(), null);
        String payloadText = getPayload(attackVector.payload());
        return "<b>Description about the attack:</b> "
                + description
                + "<br/><b>Payload:</b> "
                + payloadText;
    }
}
