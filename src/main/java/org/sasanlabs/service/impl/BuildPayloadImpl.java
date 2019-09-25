package org.sasanlabs.service.impl;

import org.sasanlabs.internal.utility.GenericUtils;
import org.sasanlabs.internal.utility.LevelEnum;
import org.sasanlabs.service.BuildPayload;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.bean.RequestBean;
import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.IGetInjectionPayload;
import org.sasanlabs.service.vulnerability.ParameterBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BuildPayloadImpl implements BuildPayload {

	private IEndPointResolver<IGetInjectionPayload> endPointResolver;

	@Autowired
	public BuildPayloadImpl(IEndPointResolver<IGetInjectionPayload> endPointResolver) {
		this.endPointResolver = endPointResolver;
	}

	@Override
	public ResponseBean build(RequestBean request) throws ServiceApplicationException {
		String level = request.getLevel();
		String endPoint = request.getEndPoint();
		LevelEnum levelEnum = LevelEnum.getLevelEnumByName(level);
		IGetInjectionPayload payload = (IGetInjectionPayload) endPointResolver.resolve(endPoint);

		ParameterBean paramBean = new ParameterBean();
		paramBean.setQueryParamKeyValueMap(request.getQueryParams());
		payload.setParameterBean(paramBean);
		paramBean.setUrl(request.getUrl());

		return GenericUtils.invokeMethod(payload, levelEnum);
	}

}
