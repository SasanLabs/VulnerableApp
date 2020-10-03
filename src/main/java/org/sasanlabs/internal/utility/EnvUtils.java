package org.sasanlabs.internal.utility;

import java.util.Map;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRestController;
import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * This class is used to get the instance of CustomVulnerableEndpoint. We are using
 * ApplicationContext here because we want to get the {@link ICustomVulnerableEndpoint} instance
 * using Bean Name/Vulnerability name.
 *
 * <p>Please don't inject Context at any other place.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@Component
public class EnvUtils {

    private ApplicationContext context;

    public EnvUtils(ApplicationContext context) {
        this.context = context;
    }

    /**
     * @param <T>
     * @param clazz Type of the returned Bean
     * @param name name of Spring Bean
     * @return Instance of the Spring Bean based on the provided clazz and name.
     * @throws ServiceApplicationException
     */
    public <T> T getInstance(Class<T> clazz, String name) throws ServiceApplicationException {
        if (this.context.containsBean(name)) {
            return this.context.getBean(name, clazz);
        } else {
            throw new ServiceApplicationException(
                    "Unable to find bean with name :- " + name + " and Type :-" + clazz.getName(),
                    ExceptionStatusCodeEnum.INVALID_END_POINT,
                    name);
        }
    }

    /**
     * Please use {@link this#getInstance(Class, String)} if possible as this method is not type
     * safe.
     *
     * @param name
     * @return Instance of the Spring Bean based on the provided name
     * @throws ServiceApplicationException
     */
    public Object getInstance(String name) throws ServiceApplicationException {
        if (this.context.containsBean(name)) {
            return this.context.getBean(name);
        } else {
            throw new ServiceApplicationException(ExceptionStatusCodeEnum.INVALID_END_POINT, name);
        }
    }

    public Map<String, Object> getAllClassesAnnotatedWithVulnerableAppRestController() {
        return context.getBeansWithAnnotation(VulnerableAppRestController.class);
    }
}
