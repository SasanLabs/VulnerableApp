package org.sasanlabs.service.impl;

import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.IGetInjectionPayload;
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
	public IGetInjectionPayload resolve(String name) throws ServiceApplicationException {
		IGetInjectionPayload getInjectionPayload = envUtils.getInstance(IGetInjectionPayload.class, name);
		return getInjectionPayload;
	}

}
