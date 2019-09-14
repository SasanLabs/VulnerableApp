package org.sasanlabs.service;

import org.sasanlabs.service.vulnerability.xss.UrlParamBean;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public interface IEndPointResolver<T> {

	T resolve(UrlParamBean urlParamBean, String name);
	
}
