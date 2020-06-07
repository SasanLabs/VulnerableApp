package org.sasanlabs.internal.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.sasanlabs.internal.utility.annotations.VulnerabilityLevel;
import org.sasanlabs.internal.utility.annotations.VulnerableServiceRestEndPoint;
import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.ICustomVulnerableEndPoint;
import org.sasanlabs.service.vulnerability.ParameterBean;

/**
 * Generic Internal Utility class.
 * @author KSASAN preetkaran20@gmail.com
 */
public class GenericUtils {

	/**
	 * This is the utility method for invoking the {@link ICustomVulnerableEndPoint} 
	 * providing {@code ParameterBean} which wraps around the HttpRequest bean.
	 * 
	 * @param customVulnerableEndPoint
	 * @param parameterBean
	 * @param level
	 * @return
	 * @throws ServiceApplicationException
	 */
	@SuppressWarnings("unchecked")
	public static <T> ResponseBean<T> invokeMethod(ICustomVulnerableEndPoint customVulnerableEndPoint, ParameterBean parameterBean, LevelEnum level)
			throws ServiceApplicationException {
		for (Method method : customVulnerableEndPoint.getClass().getMethods()) {
			if (method.isAnnotationPresent(VulnerabilityLevel.class)) {
				VulnerabilityLevel vulnerabilityLevel = method.getAnnotation(VulnerabilityLevel.class);
				if (vulnerabilityLevel.value() == level) {
					try {
						return (ResponseBean<T>) method.invoke(customVulnerableEndPoint, parameterBean);
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
				customVulnerableEndPoint.getClass().getAnnotation(VulnerableServiceRestEndPoint.class).value());
	}

	/**
	 * @deprecated
	 * @param payload
	 * @return
	 */
	public static String wrapPayloadInGenericVulnerableAppTemplate(String payload) {
		String generalPayload = "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1> %s </body></html>";
		return String.format(generalPayload, payload);
	}
}
