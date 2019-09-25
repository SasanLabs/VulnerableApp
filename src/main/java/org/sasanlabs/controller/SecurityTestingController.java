package org.sasanlabs.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.sasanlabs.controller.exception.ControllerException;
import org.sasanlabs.internal.utility.ResponseMapper;
import org.sasanlabs.service.BuildPayload;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.IGetAllSupportedEndPoints;
import org.sasanlabs.service.bean.RequestBean;
import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.IGetInjectionPayload;
import org.sasanlabs.service.vulnerability.ParameterBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
	public ResponseEntity<String> vulnerable(@RequestParam Map<String, String> allParams, @PathVariable("endPoint") String endPoint,
			@PathVariable("level") String level, HttpServletRequest request) throws IOException, ControllerException {
		ParameterBean urlParamBean = new ParameterBean();
		if (allParams != null) {
			urlParamBean.setQueryParamKeyValueMap(allParams);
		}
		RequestBean requestBean = new RequestBean();
		requestBean.setEndPoint(endPoint);
		requestBean.setLevel(level);
		requestBean.setQueryParams(allParams);
		requestBean.setUrl(request.getRequestURL().toString());
		try {
			ResponseBean responseBean = buildPayload.build(requestBean);
			return ResponseMapper.buildResponseEntity(responseBean);
		} catch (ServiceApplicationException e) {
			throw new ControllerException(e);
		}
	}

	@RequestMapping("/allEndPoint")
	public String allEndPoints() throws JsonProcessingException {
		return "<pre>" + getAllSupportedEndPoints.getSupportedEndPoints() + "</pre>";
	}

}
