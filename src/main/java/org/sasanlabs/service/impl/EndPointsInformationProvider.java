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

    private static final String NOT_APPLICABLE = "NOT_APPLICABLE";

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
                VulnerableAppRestController vulnerableServiceRestEndPoint =
                        clazz.getAnnotation(VulnerableAppRestController.class);
                String description = vulnerableServiceRestEndPoint.descriptionLabel();
                AllEndPointsResponseBean allEndPointsResponseBean = new AllEndPointsResponseBean();
                allEndPointsResponseBean.setName(name);
                allEndPointsResponseBean.setDescription(messageBundle.getString(description, null));

                Method[] methods = clazz.getDeclaredMethods();
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
                                                            attackVector.description(), null),
                                                    getMetaInformation(attackVector.source()),
                                                    getMetaInformation(attackVector.solution()),
                                                    getMetaInformation(attackVector.reference())));
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
                Method[] methods = clazz.getDeclaredMethods();
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

    /**
     * Resolves an optional metadata label (source/solution/reference) to its localized value.
     * Returns {@code null} when the key is absent, blank or {@link #NOT_APPLICABLE} so that
     * consumers which do not yet know about these fields are not shown meaningless text.
     */
    private String getMetaInformation(String key) {
        if (StringUtils.isBlank(key) || NOT_APPLICABLE.equals(key)) {
            return null;
        }
        String value = messageBundle.getString(key, null);
        return (StringUtils.isBlank(value) || NOT_APPLICABLE.equals(value)) ? null : value;
    }

    private String buildFacadeHintDescription(AttackVector attackVector) {
        String description = messageBundle.getString(attackVector.description(), null);
        String payloadText = getPayload(attackVector.payload());
        StringBuilder hint = new StringBuilder();
        hint.append("<b>Description about the attack:</b> ").append(description);
        String source = getMetaInformation(attackVector.source());
        if (StringUtils.isNotBlank(source)) {
            hint.append("<br/><b>Source:</b> ").append(source);
        }
        hint.append("<br/><b>Payload:</b> ").append(payloadText);
        String solution = getMetaInformation(attackVector.solution());
        if (StringUtils.isNotBlank(solution)) {
            hint.append("<br/><b>Solution:</b> ").append(solution);
        }
        String reference = getMetaInformation(attackVector.reference());
        if (StringUtils.isNotBlank(reference)) {
            hint.append("<br/><b>Reference:</b> ").append(reference);
        }
        return hint.toString();
    }
}
