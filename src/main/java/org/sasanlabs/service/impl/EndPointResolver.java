package org.sasanlabs.service.impl;

import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.ICustomVulnerableEndPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author KSASAN preetkaran20@gmail.com
 */
@Service
public class EndPointResolver implements IEndPointResolver<ICustomVulnerableEndPoint> {

	@Autowired
	private EnvUtils envUtils;

	@Override
	public ICustomVulnerableEndPoint resolve(String name) throws ServiceApplicationException {
		ICustomVulnerableEndPoint iCustomVulnerableEndPoint = envUtils.getInstance(ICustomVulnerableEndPoint.class, name);
		return iCustomVulnerableEndPoint;
	}

}
