package org.sasanlabs.service.impl;

import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.vulnerability.xss.IGetInjectionPayload;
import org.sasanlabs.service.vulnerability.xss.UrlParamBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
@Service
public class EndPointResolver implements IEndPointResolver<IGetInjectionPayload> {

	@Autowired
	private EnvUtils envUtils;

	@Override
	public IGetInjectionPayload resolve(UrlParamBean urlParamBean, String name) {
		IGetInjectionPayload getInjectionPayload = envUtils.getInstance(IGetInjectionPayload.class, name);
		getInjectionPayload.setUrlParamBean(urlParamBean);
		return getInjectionPayload;
	}

}
