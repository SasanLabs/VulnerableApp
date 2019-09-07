package org.sasanlabs;

import static org.sasanlabs.vulnerability.utils.Constants.NULL_BYTE_CHARACTER;

import java.io.IOException;
import java.util.Map;

import org.sasanlabs.vulnerability.UrlParamBean;
import org.sasanlabs.vulnerability.xss.UrlParamWithNullByteBasedInsideDivTagInjection;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author KSASAN preetkaran20@gmail.com
 */
@RestController
public class SecurityTestingController {

	@RequestMapping("/vulnerable")
	public String vulnerable(@RequestParam Map<String, String> allParams) throws IOException {
		UrlParamBean urlParamBean = new UrlParamBean();
		if (allParams != null) {
			urlParamBean.setParamKeyValueMap(allParams);
		}
		String generalPayload = "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1> %s </body></html>";

		String x = "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1>" + NULL_BYTE_CHARACTER
				+ "<script>;alert(1);</script></body></html>";

		String comment = "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1> <!--"
				+ NULL_BYTE_CHARACTER + "--> <script>alert(1)</script><!--" + " --></body></html>";
		String dummy = allParams.get("a");

		// UrlWithNullByteBasedInjection urlWithNullByteBasedInjection = new
		// UrlWithNullByteBasedInjection();
		// urlWithNullByteBasedInjection.setUrlParamBean(urlParamBean);
		// return String.format(generalPayload,
		// urlWithNullByteBasedInjection.getVulnerablePayload());
//		
//		UrlBasedInjection urlBasedInjection = new UrlBasedInjection();
//		urlBasedInjection.setUrlParamBean(urlParamBean);
//		return String.format(generalPayload, urlBasedInjection.getVulnerablePayload());

//		UrlParamWithNullByteBasedOutOfHtmlTagInjection urlWithNullByteBasedOutOfHtmlTagInjection = new UrlParamWithNullByteBasedOutOfHtmlTagInjection();
//		urlWithNullByteBasedOutOfHtmlTagInjection.setUrlParamBean(urlParamBean);
//		if (urlWithNullByteBasedOutOfHtmlTagInjection.inclusionInBodyTag()) {
//			return String.format(generalPayload, urlWithNullByteBasedOutOfHtmlTagInjection.getVulnerablePayload());
//		} else {
//			return urlWithNullByteBasedOutOfHtmlTagInjection.getVulnerablePayload();
//		}

		UrlParamWithNullByteBasedInsideDivTagInjection urlWithNullByteBasedOutOfHtmlTagInjection = new UrlParamWithNullByteBasedInsideDivTagInjection();
		urlWithNullByteBasedOutOfHtmlTagInjection.setUrlParamBean(urlParamBean);
		if (urlWithNullByteBasedOutOfHtmlTagInjection.inclusionInBodyTag()) {
			return String.format(generalPayload, urlWithNullByteBasedOutOfHtmlTagInjection.getVulnerablePayload());
		} else {
			return urlWithNullByteBasedOutOfHtmlTagInjection.getVulnerablePayload();
		}
	}

}
