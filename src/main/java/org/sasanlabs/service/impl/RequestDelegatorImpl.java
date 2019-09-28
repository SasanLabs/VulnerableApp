package org.sasanlabs.service.impl;

import org.sasanlabs.internal.utility.GenericUtils;
import org.sasanlabs.internal.utility.LevelEnum;
import org.sasanlabs.service.RequestDelegator;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.bean.RequestBean;
import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.ICustomVulnerableEndPoint;
import org.sasanlabs.service.vulnerability.ParameterBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequestDelegatorImpl implements RequestDelegator {

	private IEndPointResolver<ICustomVulnerableEndPoint> endPointResolver;

	@Autowired
	public RequestDelegatorImpl(IEndPointResolver<ICustomVulnerableEndPoint> endPointResolver) {
		this.endPointResolver = endPointResolver;
	}

	@Override
	public ResponseBean delegate(RequestBean request) throws ServiceApplicationException {
		String level = request.getLevel();
		String endPoint = request.getEndPoint();
		LevelEnum levelEnum = LevelEnum.getLevelEnumByName(level);
		ICustomVulnerableEndPoint payload = (ICustomVulnerableEndPoint) endPointResolver.resolve(endPoint);

		ParameterBean paramBean = new ParameterBean();
		paramBean.setQueryParamKeyValueMap(request.getQueryParams());
		paramBean.setRequestHeadersMap(request.getHeaders());
		payload.setParameterBean(paramBean);
		paramBean.setUrl(request.getUrl());

		return GenericUtils.invokeMethod(payload, levelEnum);
	}

}
