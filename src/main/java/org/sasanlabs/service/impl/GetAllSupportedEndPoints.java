package org.sasanlabs.service.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.LevelResponseBean;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.JSONSerializationUtils;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.internal.utility.VulnerabilityLevel;
import org.sasanlabs.internal.utility.VulnerableServiceRestEndPoint;
import org.sasanlabs.service.IGetAllSupportedEndPoints;
import org.sasanlabs.service.vulnerability.xss.IGetInjectionPayload;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
@Service
public class GetAllSupportedEndPoints implements IGetAllSupportedEndPoints {

	private EnvUtils envUtils;

	private MessageBundle messageBundle;

	@Autowired
	public GetAllSupportedEndPoints(EnvUtils envUtils, MessageBundle messageBundle) {
		this.envUtils = envUtils;
		this.messageBundle = messageBundle;
	}

	@Override
	public String getSupportedEndPoints() throws JsonProcessingException {
		List<AllEndPointsResponseBean> allEndpoints = new ArrayList<>();
		Map<String, IGetInjectionPayload> nameVsIGetInjectionPayloadMap = envUtils
				.getAllClassesExtendingIGetInjectionPayload();
		for (Map.Entry<String, IGetInjectionPayload> entry : nameVsIGetInjectionPayloadMap.entrySet()) {
			String name = entry.getKey();
			Class<? extends IGetInjectionPayload> clazz = entry.getValue().getClass();
			if (clazz.isAnnotationPresent(VulnerableServiceRestEndPoint.class)) {
				VulnerableServiceRestEndPoint vulnerableServiceRestEndPoint = clazz
						.getAnnotation(VulnerableServiceRestEndPoint.class);
				String description = vulnerableServiceRestEndPoint.descriptionLabel();
				VulnerabilityType vulnerabilityType = vulnerableServiceRestEndPoint.type();
				AllEndPointsResponseBean allEndPointsResponseBean = new AllEndPointsResponseBean();
				allEndPointsResponseBean.setName(name);
				allEndPointsResponseBean.setDescription(messageBundle.getString(description, null));
				allEndPointsResponseBean.setVulnerabilityType(vulnerabilityType);

				Method[] methods = clazz.getDeclaredMethods();
				for (Method method : methods) {
					VulnerabilityLevel[] vulnLevel = method.getAnnotationsByType(VulnerabilityLevel.class);
					for (VulnerabilityLevel level : vulnLevel) {
						LevelResponseBean levelResponseBean = new LevelResponseBean();
						levelResponseBean.setLevelEnum(level.value());
						levelResponseBean.setDescription(messageBundle.getString(level.descriptionLabel(), null));
						allEndPointsResponseBean.getLevelDescriptionSet().add(levelResponseBean);
					}
				}
				allEndpoints.add(allEndPointsResponseBean);
			}
		}
		return JSONSerializationUtils.serializeWithPrettyPrintJSON(allEndpoints);
	}

}
