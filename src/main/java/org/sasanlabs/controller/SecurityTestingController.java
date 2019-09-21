package org.sasanlabs.controller;

import java.io.IOException;
import java.util.Map;

import org.sasanlabs.controller.exception.ControllerException;
import org.sasanlabs.service.BuildPayload;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.IGetAllSupportedEndPoints;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.xss.IGetInjectionPayload;
import org.sasanlabs.service.vulnerability.xss.UrlParamBean;
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

	private BuildPayload buildPayload;

	private IGetAllSupportedEndPoints getAllSupportedEndPoints;

	@Autowired
	public SecurityTestingController(BuildPayload buildPayload,
			IEndPointResolver<IGetInjectionPayload> endPointResolver,
			IGetAllSupportedEndPoints getAllSupportedEndPoints) {
		this.buildPayload = buildPayload;
		this.getAllSupportedEndPoints = getAllSupportedEndPoints;
	}

	@RequestMapping("/vulnerable/{level}/{endPoint}")
	public String vulnerable(@RequestParam Map<String, String> allParams, @PathVariable("endPoint") String endPoint,
			@PathVariable("level") String level) throws IOException, ControllerException {
		UrlParamBean urlParamBean = new UrlParamBean();
		if (allParams != null) {
			urlParamBean.setParamKeyValueMap(allParams);
		}
		String generalPayload = "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1> %s </body></html>";
		try {
			return buildPayload.build(urlParamBean, endPoint, level, generalPayload);
		} catch (ServiceApplicationException e) {
			throw new ControllerException(e);
		}
	}

	@RequestMapping("/allEndPoint")
	public String allEndPoints() throws JsonProcessingException {
		return "<pre>" + getAllSupportedEndPoints.getSupportedEndPoints() + "</pre>";
	}

}
