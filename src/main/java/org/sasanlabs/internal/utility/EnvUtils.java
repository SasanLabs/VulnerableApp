package org.sasanlabs.internal.utility;

import java.util.Map;

import org.sasanlabs.service.vulnerability.xss.IGetInjectionPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author KSASAN preetkaran20@gmail.com
 * please don't inject Context at any other place.
 */
@Component
public class EnvUtils {

	private ApplicationContext context;
	
	@Autowired
	public EnvUtils(ApplicationContext context) {
		this.context = context;
	}
	public <T> T getInstance(Class<T> clazz, String name) {
		return this.context.getBean(name,clazz);
	}
	
	public Object getInstance(String name) {
		return this.context.getBean(name);
	}
	
	public Map<String, IGetInjectionPayload> getAllClassesExtendingIGetInjectionPayload() {
		return context.getBeansOfType(IGetInjectionPayload.class);
	}

}
