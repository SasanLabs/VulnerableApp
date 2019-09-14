package org.sasanlabs.controller;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.LevelResponseBean;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.JSONSerializationUtils;
import org.sasanlabs.internal.utility.LevelEnum;
import org.sasanlabs.internal.utility.MessageBundle;
import org.sasanlabs.internal.utility.VulnerabilityLevel;
import org.sasanlabs.internal.utility.VulnerableServiceRestEndPoint;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.IGetAllSupportedEndPoints;
import org.sasanlabs.service.vulnerability.xss.IGetInjectionPayload;
import org.sasanlabs.service.vulnerability.xss.UrlParamBean;
import org.sasanlabs.service.vulnerability.xss.impl.UrlParamBasedImgTagAttrInjection;
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

	private IEndPointResolver<IGetInjectionPayload> endPointResolver;

	private IGetAllSupportedEndPoints getAllSupportedEndPoints;

	@Autowired
	public SecurityTestingController(IEndPointResolver<IGetInjectionPayload> endPointResolver,
			IGetAllSupportedEndPoints getAllSupportedEndPoints) {
		this.endPointResolver = endPointResolver;
		this.getAllSupportedEndPoints = getAllSupportedEndPoints;
	}

	@RequestMapping("/vulnerable/{level}/{endPoint}")
	public String vulnerable(@RequestParam Map<String, String> allParams, @PathVariable("endPoint") String endPoint,
			@PathVariable("level") LevelEnum level) throws IOException {
		UrlParamBean urlParamBean = new UrlParamBean();
		if (allParams != null) {
			urlParamBean.setParamKeyValueMap(allParams);
		}
		String generalPayload = "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1> %s </body></html>";
		UrlParamBasedImgTagAttrInjection payload = (UrlParamBasedImgTagAttrInjection)endPointResolver.resolve(urlParamBean, endPoint);
		if (payload.inclusionInBodyTag()) {
			 return String.format(generalPayload, payload.getVulnerablePayloadLevelMedium());
		} else {
			 return payload.getVulnerablePayloadLevelLow();
		}
		
	}

	@RequestMapping("/allEndPoint")
	public String allEndPoints() throws JsonProcessingException {
		return "<pre>" + getAllSupportedEndPoints.getSupportedEndPoints() + "</pre>";
	}

}
