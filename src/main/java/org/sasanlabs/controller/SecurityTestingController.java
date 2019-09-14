package org.sasanlabs.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.JSONSerializationUtils;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.internal.utility.VulnerableServiceRestEndPoint;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.vulnerability.xss.IGetInjectionPayload;
import org.sasanlabs.service.vulnerability.xss.UrlParamBean;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author KSASAN preetkaran20@gmail.com
 */
@RestController
public class SecurityTestingController {

	private EnvUtils envUtils;

	private IEndPointResolver<IGetInjectionPayload> endPointResolver;

	private MessageBundle messageBundle;

	@Autowired
	public SecurityTestingController(EnvUtils envUtils, IEndPointResolver<IGetInjectionPayload> endPointResolver,
			MessageBundle messageBundle) {
		this.envUtils = envUtils;
		this.endPointResolver = endPointResolver;
		this.messageBundle = messageBundle;
	}

	@RequestMapping("/vulnerable/{endPoint}")
	public String vulnerable(@RequestParam Map<String, String> allParams, @PathVariable("endPoint") String endPoint)
			throws IOException {
		UrlParamBean urlParamBean = new UrlParamBean();
		if (allParams != null) {
			urlParamBean.setParamKeyValueMap(allParams);
		}
		String generalPayload = "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1> %s </body></html>";
		IGetInjectionPayload payload = endPointResolver.resolve(urlParamBean, endPoint);
		if (payload.inclusionInBodyTag()) {
			return String.format(generalPayload, payload.getVulnerablePayload());
		} else {
			return payload.getVulnerablePayload();
		}
	}

	@RequestMapping("/allEndPoint")
	public String allEndPoints() throws JsonProcessingException {
		List<AllEndPointsResponseBean> allEndpoints = new ArrayList<>();
		Map<String, IGetInjectionPayload> nameVsIGetInjectionPayloadMap = envUtils
				.getAllClassesExtendingIGetInjectionPayload();
		for (Map.Entry<String, IGetInjectionPayload> entry : nameVsIGetInjectionPayloadMap.entrySet()) {
			String name = entry.getKey();
			Class<? extends IGetInjectionPayload> clazz = entry.getValue().getClass();
			if (clazz.isAnnotationPresent(VulnerableServiceRestEndPoint.class)) {
				VulnerableServiceRestEndPoint vulnerableServiceRestEndPoint = clazz
						.getAnnotation(VulnerableServiceRestEndPoint.class);
				String description = vulnerableServiceRestEndPoint.description();
				VulnerabilityType vulnerabilityType = vulnerableServiceRestEndPoint.type();
				AllEndPointsResponseBean allEndPointsResponseBean = new AllEndPointsResponseBean();
				allEndPointsResponseBean.setName(name);
				allEndPointsResponseBean.setDescription(messageBundle.getString(description,null));
				allEndPointsResponseBean.setVulnerabilityType(vulnerabilityType);
				allEndpoints.add(allEndPointsResponseBean);
			}
		}
		return JSONSerializationUtils.serialize(allEndpoints);
	}

}
