package org.sasanlabs.service;

import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.xss.UrlParamBean;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public interface BuildPayload {

	String build(UrlParamBean urlParamBean, String endPoint, String level, String generalPayload) throws ServiceApplicationException;
	
}
