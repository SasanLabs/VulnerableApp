package org.sasanlabs.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.sasanlabs.controller.exception.ControllerException;
import org.sasanlabs.internal.utility.ResponseMapper;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.IGetAllSupportedEndPoints;
import org.sasanlabs.service.RequestDelegator;
import org.sasanlabs.service.bean.RequestBean;
import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.ICustomVulnerableEndPoint;
import org.sasanlabs.service.vulnerability.nosqlInjection.mongo.MongoDBInjectionController;
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

	private RequestDelegator requestDelegator;

	private IGetAllSupportedEndPoints getAllSupportedEndPoints;
	
	@Autowired
	private MongoDBInjectionController mongoDBInjectionController;

	@Autowired
	public SecurityTestingController(RequestDelegator buildPayload,
			IEndPointResolver<ICustomVulnerableEndPoint> endPointResolver,
			IGetAllSupportedEndPoints getAllSupportedEndPoints) {
		this.requestDelegator = buildPayload;
		this.getAllSupportedEndPoints = getAllSupportedEndPoints;
	}

	@RequestMapping("/vulnerable/{endPoint}/{level}")
	public ResponseEntity<String> vulnerable(@RequestParam Map<String, String> allParams, @PathVariable("endPoint") String endPoint,
			@PathVariable("level") String level, HttpServletRequest request) throws IOException, ControllerException {
		RequestBean requestBean = new RequestBean();
		requestBean.setEndPoint(endPoint);
		requestBean.setLevel(level);
		requestBean.setQueryParams(allParams);
		requestBean.setUrl(request.getRequestURL().toString());
		Enumeration<String> headerNames = request.getHeaderNames();
		
		while(headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			requestBean.getHeaders().put(headerName, new ArrayList<>());
			Enumeration<String> headerValueEnumeration = request.getHeaders(headerName);
			while(headerValueEnumeration.hasMoreElements()) {
				requestBean.getHeaders().get(headerName).add(headerValueEnumeration.nextElement());
			}
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

	
	@RequestMapping("/dummyTesting")
	public ResponseEntity<String> dummyTesting() throws JsonProcessingException {
		
		return ResponseMapper.buildResponseEntity(mongoDBInjectionController.simpleWhereClauseInjectionLow());
	}
}
