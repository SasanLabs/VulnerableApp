package org.sasanlabs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.AttackVectorResponseBean;
import org.sasanlabs.beans.LevelResponseBean;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.configuration.VulnerableAppProperties;
import org.sasanlabs.facade.beans.FacadeResourceInformation;
import org.sasanlabs.facade.beans.FacadeResourceType;
import org.sasanlabs.facade.beans.FacadeResourceURI;
import org.sasanlabs.facade.beans.FacadeVulnerabilityDefinition;
import org.sasanlabs.facade.beans.FacadeVulnerabilityLevelDefinition;
import org.sasanlabs.facade.beans.FacadeVulnerabilityLevelHint;
import org.sasanlabs.facade.beans.FacadeVulnerabilityType;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.FrameworkConstants;
import org.sasanlabs.internal.utility.GenericUtils;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.internal.utility.annotations.AttackVector;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRequestMapping;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRestController;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** @author KSASAN preetkaran20@gmail.com */
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
        Map<String, Object> nameVsCustomVulnerableEndPoint =
                envUtils.getAllClassesAnnotatedWithVulnerableAppRestController();
        for (Map.Entry<String, Object> entry : nameVsCustomVulnerableEndPoint.entrySet()) {
            String name = entry.getKey();
            Class<?> clazz = entry.getValue().getClass();
            if (clazz.isAnnotationPresent(VulnerableAppRestController.class)) {
                VulnerableAppRestController vulnerableServiceRestEndPoint =
                        clazz.getAnnotation(VulnerableAppRestController.class);
                String description = vulnerableServiceRestEndPoint.descriptionLabel();
                VulnerabilityType[] vulnerabilityTypes = vulnerableServiceRestEndPoint.type();
                AllEndPointsResponseBean allEndPointsResponseBean = new AllEndPointsResponseBean();
                allEndPointsResponseBean.setName(name);
                allEndPointsResponseBean.setDescription(messageBundle.getString(description, null));
                allEndPointsResponseBean.setVulnerabilityTypes(vulnerabilityTypes);

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
                        levelResponseBean.setDescription(
                                messageBundle.getString(vulnLevel.descriptionLabel(), null));
                        levelResponseBean.setHtmlTemplate(vulnLevel.htmlTemplate());

                        levelResponseBean.setRequestParameterLocation(
                                vulnLevel.requestParameterLocation());
                        levelResponseBean.setParameterName(vulnLevel.parameterName());
                        levelResponseBean.setSampleValues(vulnLevel.sampleValues());
                        levelResponseBean.setRequestMethod(vulnLevel.requestMethod());
                        for (AttackVector attackVector : attackVectors) {
                            levelResponseBean
                                    .getAttackVectorResponseBeans()
                                    .add(
                                            new AttackVectorResponseBean(
                                                    new ArrayList<>(
                                                            Arrays.asList(
                                                                    attackVector
                                                                            .vulnerabilityExposed())),
                                                    vulnerableAppProperties.getAttackVectorProperty(
                                                            attackVector.payload()),
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
    public List<ScannerResponseBean> getScannerRelatedEndPointInformation()
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
                                            .append(FrameworkConstants.HTTP)
                                            .append(GenericUtils.LOCALHOST)
                                            .append(FrameworkConstants.COLON)
                                            .append(port)
                                            .append(FrameworkConstants.SLASH)
                                            .append(allEndPointsResponseBean.getName())
                                            .append(FrameworkConstants.SLASH)
                                            .append(levelResponseBean.getLevel())
                                            .toString(),
                                    levelResponseBean.getLevel(),
                                    levelResponseBean.getVariant().toString(),
                                    levelResponseBean.getRequestParameterLocation(),
                                    levelResponseBean.getParameterName(),
                                    levelResponseBean.getSampleValues(),
                                    levelResponseBean.getRequestMethod(),
                                    attackVectorResponseBean.getVulnerabilityTypes()));
                }
            }
        }
        return scannerResponseBeans;
    }

    private void addFacadeResourceInformation(
            FacadeVulnerabilityDefinition facadeVulnerabilityDefinition,
            FacadeVulnerabilityLevelDefinition facadeVulnerabilityLevelDefinition,
            String template) {
        FacadeResourceInformation resourceInformation = new FacadeResourceInformation();
        facadeVulnerabilityLevelDefinition.setResourceInformation(resourceInformation);
        resourceInformation.setStaticResources(
                Arrays.asList(
                        new FacadeResourceURI(
                                false,
                                "/VulnerableApp/templates/"
                                        + facadeVulnerabilityDefinition.getName()
                                        + "/"
                                        + template
                                        + ".css",
                                FacadeResourceType.CSS.name()),
                        new FacadeResourceURI(
                                false,
                                "/VulnerableApp/templates/"
                                        + facadeVulnerabilityDefinition.getName()
                                        + "/"
                                        + template
                                        + ".js",
                                FacadeResourceType.JAVASCRIPT.name())));
        resourceInformation.setHtmlResource(
                new FacadeResourceURI(
                        false,
                        "/VulnerableApp/templates/"
                                + facadeVulnerabilityDefinition.getName()
                                + "/"
                                + template
                                + ".html"));
    }

    @Override
    public List<FacadeVulnerabilityDefinition> getVulnerabilityDefinitions()
            throws JsonProcessingException {
        List<FacadeVulnerabilityDefinition> vulnerabilityDefinitions = new ArrayList<>();
        Map<String, Object> nameVsCustomVulnerableEndPoint =
                envUtils.getAllClassesAnnotatedWithVulnerableAppRestController();
        for (Map.Entry<String, Object> entry : nameVsCustomVulnerableEndPoint.entrySet()) {
            String name = entry.getKey();
            Class<?> clazz = entry.getValue().getClass();
            if (clazz.isAnnotationPresent(VulnerableAppRestController.class)) {
                VulnerableAppRestController vulnerableServiceRestEndPoint =
                        clazz.getAnnotation(VulnerableAppRestController.class);
                String description = vulnerableServiceRestEndPoint.descriptionLabel();
                VulnerabilityType[] vulnerabilityTypes = vulnerableServiceRestEndPoint.type();
                FacadeVulnerabilityDefinition facadeVulnerabilityDefinition =
                        new FacadeVulnerabilityDefinition();
                facadeVulnerabilityDefinition.setName(name);
                facadeVulnerabilityDefinition.setId(name);
                facadeVulnerabilityDefinition.setDescription(
                        messageBundle.getString(description, null));
                List<FacadeVulnerabilityType> facadeVulnerabilityTypes =
                        new ArrayList<FacadeVulnerabilityType>();
                for (VulnerabilityType vulnerabilityType : vulnerabilityTypes) {
                    facadeVulnerabilityTypes.add(
                            new FacadeVulnerabilityType("Custom", vulnerabilityType.name()));
                }
                facadeVulnerabilityDefinition.setVulnerabilityTypes(facadeVulnerabilityTypes);
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    VulnerableAppRequestMapping vulnLevel =
                            method.getAnnotation(VulnerableAppRequestMapping.class);
                    if (vulnLevel != null) {
                        AttackVector[] attackVectors =
                                method.getAnnotationsByType(AttackVector.class);
                        FacadeVulnerabilityLevelDefinition facadeVulnerabilityLevelDefinition =
                                new FacadeVulnerabilityLevelDefinition();
                        facadeVulnerabilityLevelDefinition.setLevel(vulnLevel.value());
                        facadeVulnerabilityLevelDefinition.setVariant(vulnLevel.variant());
                        facadeVulnerabilityLevelDefinition.setDescription(
                                messageBundle.getString(vulnLevel.descriptionLabel(), null));
                        addFacadeResourceInformation(
                                facadeVulnerabilityDefinition,
                                facadeVulnerabilityLevelDefinition,
                                vulnLevel.htmlTemplate());
                        for (AttackVector attackVector : attackVectors) {
                            List<FacadeVulnerabilityType> facadeLevelVulnerabilityTypes =
                                    new ArrayList<FacadeVulnerabilityType>();
                            for (VulnerabilityType vulnerabilityType : vulnerabilityTypes) {
                                facadeLevelVulnerabilityTypes.add(
                                        new FacadeVulnerabilityType(
                                                "Custom", vulnerabilityType.name()));
                            }
                            facadeVulnerabilityLevelDefinition
                                    .getHints()
                                    .add(
                                            new FacadeVulnerabilityLevelHint(
                                                    facadeLevelVulnerabilityTypes,
                                                    attackVector.payload()));
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
}
