package org.sasanlabs.service.impl;

import org.sasanlabs.internal.utility.GenericUtils;
import org.sasanlabs.internal.utility.LevelEnum;
import org.sasanlabs.service.BuildPayload;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.xss.IGetInjectionPayload;
import org.sasanlabs.service.vulnerability.xss.UrlParamBean;
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
	public String build(UrlParamBean urlParamBean, String endPoint, String level, String generalPayload)
			throws ServiceApplicationException {
		LevelEnum levelEnum = LevelEnum.getLevelEnumByName(level);
		IGetInjectionPayload payload = (IGetInjectionPayload) endPointResolver.resolve(urlParamBean, endPoint);
		if (payload.inclusionInBodyTag()) {
			return String.format(generalPayload, GenericUtils.invokeMethod(payload, levelEnum));
		} else {
			return GenericUtils.invokeMethod(payload, levelEnum);
		}
	}

}
