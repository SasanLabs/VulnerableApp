package org.sasanlabs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.AttackVectorResponseBean;
import org.sasanlabs.beans.LevelResponseBean;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.configuration.VulnerableAppProperties;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.FrameworkConstants;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.internal.utility.annotations.AttackVector;
import org.sasanlabs.internal.utility.annotations.VulnerabilityLevel;
import org.sasanlabs.internal.utility.annotations.VulnerableServiceRestEndPoint;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.sasanlabs.service.vulnerability.ICustomVulnerableEndPoint;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** @author KSASAN preetkaran20@gmail.com */
@Service
public class EndPointsInformationProvider implements IEndPointsInformationProvider {

    private EnvUtils envUtils;

    private MessageBundle messageBundle;

    private VulnerableAppProperties vulnerableAppProperties;

    int port;

    @Autowired
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
        Map<String, ICustomVulnerableEndPoint> nameVsCustomVulnerableEndPoint =
                envUtils.getAllClassesExtendingIGetInjectionPayload();
        for (Map.Entry<String, ICustomVulnerableEndPoint> entry :
                nameVsCustomVulnerableEndPoint.entrySet()) {
            String name = entry.getKey();
            Class<? extends ICustomVulnerableEndPoint> clazz = entry.getValue().getClass();
            if (clazz.isAnnotationPresent(VulnerableServiceRestEndPoint.class)) {
                VulnerableServiceRestEndPoint vulnerableServiceRestEndPoint =
                        clazz.getAnnotation(VulnerableServiceRestEndPoint.class);
                String description = vulnerableServiceRestEndPoint.descriptionLabel();
                VulnerabilityType[] vulnerabilityTypes = vulnerableServiceRestEndPoint.type();
                AllEndPointsResponseBean allEndPointsResponseBean = new AllEndPointsResponseBean();
                allEndPointsResponseBean.setName(name);
                allEndPointsResponseBean.setDescription(messageBundle.getString(description, null));
                allEndPointsResponseBean.setVulnerabilityTypes(vulnerabilityTypes);

                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    VulnerabilityLevel vulnLevel = method.getAnnotation(VulnerabilityLevel.class);
                    if (vulnLevel != null) {
                        AttackVector[] attackVectors =
                                method.getAnnotationsByType(AttackVector.class);
                        LevelResponseBean levelResponseBean = new LevelResponseBean();
                        levelResponseBean.setLevelEnum(vulnLevel.value());
                        levelResponseBean.setDescription(
                                messageBundle.getString(vulnLevel.descriptionLabel(), null));
                        levelResponseBean.setHtmlTemplate(vulnLevel.htmlTemplate());

                        levelResponseBean.setRequestParameterLocation(
                                vulnLevel.requestParameterLocation());
                        levelResponseBean.setParameterName(vulnLevel.parameterName());
                        levelResponseBean.setSampleValues(vulnLevel.sampleValues());
                        levelResponseBean.setHttpMethod(vulnLevel.httpMethod());
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
                                                            attackVector.curlPayload()),
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
        String ipAddress = InetAddress.getLocalHost().getHostAddress();
        allEndPointsResponseBeans.stream()
                .forEach(
                        allEndPointsResponseBean -> {
                            allEndPointsResponseBean
                                    .getLevelDescriptionSet()
                                    .forEach(
                                            levelResponseBean -> {
                                                levelResponseBean.getAttackVectorResponseBeans()
                                                        .stream()
                                                        .map(
                                                                attackVectorResponseBean ->
                                                                        scannerResponseBeans.add(
                                                                                new ScannerResponseBean(
                                                                                        new StringBuilder()
                                                                                                .append(
                                                                                                        FrameworkConstants
                                                                                                                .HTTP)
                                                                                                .append(
                                                                                                        ipAddress)
                                                                                                .append(
                                                                                                        FrameworkConstants
                                                                                                                .COLON)
                                                                                                .append(
                                                                                                        port)
                                                                                                .append(
                                                                                                        FrameworkConstants
                                                                                                                .SLASH)
                                                                                                .append(
                                                                                                        FrameworkConstants
                                                                                                                .VULNERABLE)
                                                                                                .append(
                                                                                                        FrameworkConstants
                                                                                                                .SLASH)
                                                                                                .append(
                                                                                                        allEndPointsResponseBean
                                                                                                                .getName())
                                                                                                .append(
                                                                                                        FrameworkConstants
                                                                                                                .SLASH)
                                                                                                .append(
                                                                                                        levelResponseBean
                                                                                                                .getLevelEnum()
                                                                                                                .name())
                                                                                                .toString(),
                                                                                        levelResponseBean
                                                                                                .getRequestParameterLocation(),
                                                                                        levelResponseBean
                                                                                                .getParameterName(),
                                                                                        levelResponseBean
                                                                                                .getSampleValues(),
                                                                                        levelResponseBean
                                                                                                .getHttpMethod(),
                                                                                        attackVectorResponseBean
                                                                                                .getVulnerabilityTypes())))
                                                        .collect(Collectors.toList());
                                            });
                        });
        return scannerResponseBeans;
    }
}
