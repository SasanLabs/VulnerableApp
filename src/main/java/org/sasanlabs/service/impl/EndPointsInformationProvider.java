package org.sasanlabs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.AttackVectorResponseBean;
import org.sasanlabs.beans.ChallengeCardResponseBean;
import org.sasanlabs.beans.LevelResponseBean;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.configuration.VulnerableAppProperties;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.FrameworkConstants;
import org.sasanlabs.internal.utility.LevelConstants;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.internal.utility.annotations.AttackVector;
import org.sasanlabs.internal.utility.annotations.ChallengeCard;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRequestMapping;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRestController;
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

/**
 * @author KSASAN preetkaran20@gmail.com
 */
@Service
public class EndPointsInformationProvider implements IEndPointsInformationProvider {

    private static final Logger LOGGER = LogManager.getLogger(EndPointsInformationProvider.class);

    private static final Pattern CATALOG_LEVEL = Pattern.compile("LEVEL_[1-9][0-9]*");

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
        Map<String, Object> nameVsCustomVulnerableEndPoint =
                envUtils.getAllClassesAnnotatedWithVulnerableAppRestController();
        for (Map.Entry<String, Object> entry : nameVsCustomVulnerableEndPoint.entrySet()) {
            String name = entry.getKey();
            Class<?> clazz = entry.getValue().getClass();
            if (clazz.isAnnotationPresent(VulnerableAppRestController.class)) {
                Method[] methods = clazz.getDeclaredMethods();
                if (!hasValidCatalogMappings(name, methods)) {
                    continue;
                }
                VulnerableAppRestController vulnerableServiceRestEndPoint =
                        clazz.getAnnotation(VulnerableAppRestController.class);
                String description = vulnerableServiceRestEndPoint.descriptionLabel();
                AllEndPointsResponseBean allEndPointsResponseBean = new AllEndPointsResponseBean();
                allEndPointsResponseBean.setName(name);
                allEndPointsResponseBean.setDescription(messageBundle.getString(description, null));

                for (Method method : methods) {
                    VulnerableAppRequestMapping vulnLevel =
                            method.getAnnotation(VulnerableAppRequestMapping.class);
                    if (vulnLevel != null) {
                        AttackVector[] attackVectors =
                                method.getAnnotationsByType(AttackVector.class);
                        LevelResponseBean levelResponseBean = new LevelResponseBean();
                        levelResponseBean.setLevel(vulnLevel.value());
                        levelResponseBean.setVariant(vulnLevel.variant());
                        levelResponseBean.setHtmlTemplate(vulnLevel.htmlTemplate());
                        levelResponseBean.setRequestMethod(vulnLevel.requestMethod());
                        ChallengeCard[] challengeCards =
                                method.getAnnotationsByType(ChallengeCard.class);
                        for (ChallengeCard card : challengeCards) {
                            List<ChallengeCardResponseBean.HintResponseBean> hintBeans =
                                    new ArrayList<>();
                            for (ChallengeCard.Hint hint : card.hints()) {
                                hintBeans.add(
                                        new ChallengeCardResponseBean.HintResponseBean(
                                                hint.order(),
                                                messageBundle.getString(hint.text(), null)));
                            }
                            String payload = getPayload(card.payload().value());
                            ChallengeCardResponseBean.PayloadResponseBean payloadBean =
                                    new ChallengeCardResponseBean.PayloadResponseBean(
                                            messageBundle.getString(
                                                    card.payload().description(), null),
                                            payload);

                            levelResponseBean
                                    .getChallengeCards()
                                    .add(
                                            new ChallengeCardResponseBean(
                                                    messageBundle.getString(
                                                            card.challengeText(), null),
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
                                                            Arrays.asList(
                                                                    attackVector
                                                                            .vulnerabilityExposed())),
                                                    payload,
                                                    messageBundle.getString(
                                                            attackVector.description(), null)));
                        }
                        allEndPointsResponseBean.getLevelDescriptionSet().add(levelResponseBean);
                    }
                }
                allEndpoints.add(allEndPointsResponseBean);
            }
        }
        return allEndpoints;
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
        Map<String, Object> nameVsCustomVulnerableEndPoint =
                envUtils.getAllClassesAnnotatedWithVulnerableAppRestController();
        for (Map.Entry<String, Object> entry : nameVsCustomVulnerableEndPoint.entrySet()) {
            String name = entry.getKey();
            Class<?> clazz = entry.getValue().getClass();
            if (clazz.isAnnotationPresent(VulnerableAppRestController.class)) {
                Method[] methods = clazz.getDeclaredMethods();
                if (!hasValidCatalogMappings(name, methods)) {
                    continue;
                }
                VulnerableAppRestController vulnerableServiceRestEndPoint =
                        clazz.getAnnotation(VulnerableAppRestController.class);
                String description = vulnerableServiceRestEndPoint.descriptionLabel();
                VulnerabilityDefinition facadeVulnerabilityDefinition =
                        new VulnerabilityDefinition();
                facadeVulnerabilityDefinition.setName(name);
                facadeVulnerabilityDefinition.setId(name);
                facadeVulnerabilityDefinition.setDescription(
                        messageBundle.getString(description, null));
                List<VulnerabilityType> facadeVulnerabilityTypes =
                        new ArrayList<VulnerabilityType>();
                facadeVulnerabilityDefinition.setVulnerabilityTypes(facadeVulnerabilityTypes);
                for (Method method : methods) {
                    VulnerableAppRequestMapping vulnLevel =
                            method.getAnnotation(VulnerableAppRequestMapping.class);
                    if (vulnLevel != null) {
                        AttackVector[] attackVectors =
                                method.getAnnotationsByType(AttackVector.class);
                        VulnerabilityLevelDefinition facadeVulnerabilityLevelDefinition =
                                new VulnerabilityLevelDefinition();
                        facadeVulnerabilityLevelDefinition.setLevel(vulnLevel.value());
                        facadeVulnerabilityLevelDefinition.setVariant(
                                Variant.valueOf(vulnLevel.variant().name()));
                        addFacadeResourceInformation(
                                facadeVulnerabilityDefinition,
                                facadeVulnerabilityLevelDefinition,
                                vulnLevel.htmlTemplate());

                        ChallengeCard[] challengeCardAnnotations =
                                method.getAnnotationsByType(ChallengeCard.class);
                        List<org.sasanlabs.vulnerableapp.facade.schema.ChallengeCard>
                                facadeChallengeCards = new ArrayList<>();

                        for (ChallengeCard card : challengeCardAnnotations) {
                            org.sasanlabs.vulnerableapp.facade.schema.ChallengeCard
                                    facadeChallenge =
                                            new org.sasanlabs.vulnerableapp.facade.schema
                                                    .ChallengeCard();

                            // Set the Challenge Text
                            facadeChallenge.setChallengeText(
                                    messageBundle.getString(card.challengeText(), null));

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
                            org.sasanlabs.vulnerability.types.VulnerabilityType[]
                                    vulnerabilityTypes = attackVector.vulnerabilityExposed();
                            for (org.sasanlabs.vulnerability.types.VulnerabilityType
                                    vulnerabilityType : vulnerabilityTypes) {
                                facadeLevelVulnerabilityTypes.add(
                                        new VulnerabilityType("Custom", vulnerabilityType.name()));
                                if (null != vulnerabilityType.getCweID())
                                    facadeLevelVulnerabilityTypes.add(
                                            new VulnerabilityType(
                                                    "CWE",
                                                    String.valueOf(vulnerabilityType.getCweID())));
                                if (null != vulnerabilityType.getWascID())
                                    facadeLevelVulnerabilityTypes.add(
                                            new VulnerabilityType(
                                                    "WASC",
                                                    String.valueOf(vulnerabilityType.getWascID())));
                            }
                            facadeVulnerabilityLevelDefinition
                                    .getHints()
                                    .add(
                                            new VulnerabilityLevelHint(
                                                    facadeLevelVulnerabilityTypes,
                                                    buildFacadeHintDescription(attackVector)));
                        }
                        facadeVulnerabilityDefinition
                                .getLevelDescriptionSet()
                                .add(facadeVulnerabilityLevelDefinition);
                    }
                }
                vulnerabilityDefinitions.add(facadeVulnerabilityDefinition);
            }
        }
        return vulnerabilityDefinitions;
    }

    private boolean hasValidCatalogMappings(String module, Method[] methods) {
        for (Method method : methods) {
            VulnerableAppRequestMapping mapping =
                    method.getAnnotation(VulnerableAppRequestMapping.class);
            if (mapping != null && !isCatalogLevel(mapping.value())) {
                LOGGER.warn(
                        "Skipping catalog module {}: {} has invalid level mapping {}",
                        module,
                        method.getName(),
                        mapping.value());
                return false;
            }
        }
        return true;
    }

    private boolean isCatalogLevel(String level) {
        if (!CATALOG_LEVEL.matcher(level).matches()) {
            return false;
        }
        try {
            LevelConstants.getOrdinal(level);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
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
