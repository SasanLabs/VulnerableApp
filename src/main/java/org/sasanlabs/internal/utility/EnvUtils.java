package org.sasanlabs.internal.utility;

import java.util.Map;

import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.IGetInjectionPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author KSASAN preetkaran20@gmail.com please don't inject Context at any
 *         other place.
 */
@Component
public class EnvUtils {

	private ApplicationContext context;

	@Autowired
	public EnvUtils(ApplicationContext context) {
		this.context = context;
	}

	public <T> T getInstance(Class<T> clazz, String name) throws ServiceApplicationException {
		if (this.context.containsBean(name)) {
			return this.context.getBean(name, clazz);
		} else {
			throw new ServiceApplicationException(
					"Unable to find bean with name :- " + name + " and Type :-" + clazz.getName(),
					ExceptionStatusCodeEnum.INVALID_END_POINT, name);
		}
	}

	public Object getInstance(String name) throws ServiceApplicationException {
		if (this.context.containsBean(name)) {
			return this.context.getBean(name);
		} else {
			throw new ServiceApplicationException(ExceptionStatusCodeEnum.INVALID_END_POINT, name);
		}
	}

	public Map<String, IGetInjectionPayload> getAllClassesExtendingIGetInjectionPayload() {
		return context.getBeansOfType(IGetInjectionPayload.class);
	}

}
