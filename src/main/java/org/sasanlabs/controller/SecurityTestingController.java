package org.sasanlabs.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sasanlabs.controller.exception.ControllerException;
import org.sasanlabs.internal.utility.ResponseMapper;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.IGetAllSupportedEndPoints;
import org.sasanlabs.service.RequestDelegator;
import org.sasanlabs.service.bean.RequestBean;
import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.ICustomVulnerableEndPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

	private RequestDelegator requestDelegator;

	private IGetAllSupportedEndPoints getAllSupportedEndPoints;

	@Autowired
	public SecurityTestingController(RequestDelegator buildPayload,
			IEndPointResolver<ICustomVulnerableEndPoint> endPointResolver,
			IGetAllSupportedEndPoints getAllSupportedEndPoints) {
		this.requestDelegator = buildPayload;
		this.getAllSupportedEndPoints = getAllSupportedEndPoints;
	}

	@RequestMapping("/vulnerable/{endPoint}/{level}")
	public ResponseEntity<String> endPointHandler(@RequestParam Map<String, String> allParams,
			@PathVariable("endPoint") String endPoint, @PathVariable("level") String level,
			RequestEntity<String> requestEntity) throws ControllerException {
		RequestBean requestBean = new RequestBean();
		requestBean.setEndPoint(endPoint);
		requestBean.setLevel(level);
		requestBean.setQueryParams(allParams);
		requestBean.setUrl(requestEntity.getUrl().toString());
		Set<String> headerNames = requestEntity.getHeaders().keySet();

		for (String headerName : headerNames) {
			requestBean.getHeaders().put(headerName, new ArrayList<>());
			List<String> headerValues = requestEntity.getHeaders().get(headerName);
			for (String headerValue : headerValues) {
				requestBean.getHeaders().get(headerName).add(headerValue);
			}
		}
		if (requestEntity.getMethod().equals(HttpMethod.POST) && requestEntity.hasBody()) {
			requestBean.setBody(requestEntity.getBody());
		}
		try {
			ResponseBean responseBean = requestDelegator.delegate(requestBean);
			return ResponseMapper.buildResponseEntity(responseBean);
		} catch (ServiceApplicationException e) {
			throw new ControllerException(e);
		}
	}
	

	@RequestMapping("/allEndPoint")
	public String allEndPoints() throws JsonProcessingException {
		return "<pre>" + getAllSupportedEndPoints.getSupportedEndPoints() + "</pre>";
	}

	@GetMapping("/")
	public String getTemplate() throws JsonProcessingException {
		return "";
	}

}
