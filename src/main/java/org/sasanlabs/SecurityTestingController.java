package org.sasanlabs;

import java.util.Map;

import org.sasanlabs.vulnerability.UrlParamBean;
import org.sasanlabs.vulnerability.xss.UrlWithNullByteBasedInjection;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author KSASAN preetkaran20@gmail.com
 */
@RestController
public class SecurityTestingController {

	@RequestMapping("/vulnerable")
	public String vulnerable(@RequestParam Map<String, String> allParams) {
		UrlParamBean urlParamBean = new UrlParamBean();
		if (allParams != null) {
			urlParamBean.setParamKeyValueMap(allParams);
		}
		String generalPayload = "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1> %s <body><html>";
		UrlWithNullByteBasedInjection urlWithNullByteBasedInjection = new UrlWithNullByteBasedInjection();
		urlWithNullByteBasedInjection.setUrlParamBean(urlParamBean);
		return String.format(generalPayload, urlWithNullByteBasedInjection.getVulnerablePayload());
	}

}
