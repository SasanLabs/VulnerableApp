package org.sasanlabs.internal.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.IGetInjectionPayload;

public class GenericUtils {

	/**
	 * @param iGetInjectionPayload
	 * @param level
	 * @return
	 * @throws ServiceApplicationException
	 * 
	 *                                     Invokes the Method as per the Level given
	 */
	public static ResponseBean invokeMethod(IGetInjectionPayload iGetInjectionPayload, LevelEnum level)
			throws ServiceApplicationException {
		for (Method method : iGetInjectionPayload.getClass().getMethods()) {
			if (method.isAnnotationPresent(VulnerabilityLevel.class)) {
				VulnerabilityLevel vulnerabilityLevel = method.getAnnotation(VulnerabilityLevel.class);
				if (vulnerabilityLevel.value() == level) {
					try {
						return (ResponseBean) method.invoke(iGetInjectionPayload);
					} catch (IllegalAccessException e) {
						throw new ServiceApplicationException(
								"Access Issue, please check the visibility of the annotated method ", e,
								ExceptionStatusCodeEnum.INVALID_ACCESS, method.getName());
					} catch (IllegalArgumentException e) {
						throw new ServiceApplicationException(ExceptionStatusCodeEnum.INVALID_ARGUMENTS,
								"Access Issue, please check the arguments of the annotated method ", e,
								ExceptionStatusCodeEnum.INVALID_ACCESS, method.getName());
					} catch (InvocationTargetException e) {
						throw new ServiceApplicationException("Some exception occurred ", e,
								ExceptionStatusCodeEnum.SYSTEM_ERROR);
					}
				}
			}
		}
		throw new ServiceApplicationException("Unable to find the Method for Level :- " + level,
				ExceptionStatusCodeEnum.UNAVAILABLE_LEVEL, level,
				iGetInjectionPayload.getClass().getAnnotation(VulnerableServiceRestEndPoint.class).value());
	}

	public static String wrapPayloadInGenericVulnerableAppTemplate(String payload) {
		String generalPayload = "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1> %s </body></html>";
		return String.format(generalPayload, payload);
	}
}
