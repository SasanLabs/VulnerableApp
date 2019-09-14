package org.sasanlabs.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * @author KSASAN preetkaran20@gmail.com
 * Configuration Class for Injecting Configurations into Context.
 */
@Configuration
public class VulnerableAppConfiguration {

	/**
	 * Will Inject MessageBundle into messageSource bean.
	 * @return
	 */
	@Bean
	public ReloadableResourceBundleMessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasename("classpath:i18n/messages");
		messageSource.setCacheSeconds(100);
		messageSource.setDefaultEncoding("UTF-8");
		return messageSource;
	}
	
}
