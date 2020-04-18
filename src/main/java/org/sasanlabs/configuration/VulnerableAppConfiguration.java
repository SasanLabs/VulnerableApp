package org.sasanlabs.configuration;

import java.io.IOException;
import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * @author KSASAN preetkaran20@gmail.com Configuration Class for Injecting
 *         Configurations into Context.
 */
@Configuration
public class VulnerableAppConfiguration {

	/**
	 * Will Inject MessageBundle into messageSource bean.
	 * 
	 * @return resourceBundle
	 */
	@Bean
	public ReloadableResourceBundleMessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasename("classpath:i18n/messages");
		messageSource.setCacheSeconds(100);
		messageSource.setDefaultEncoding("UTF-8");
		return messageSource;
	}

	/**
	 * This method reads all the property which are useful for vulnerableApp and
	 * then injects them into the context so that entire application use it.
	 * 
	 * @param resourceLoader
	 * @return {@link VulnerableAppProperties} which is injected in spring context.
	 * @throws IOException
	 */
	@Bean
	public VulnerableAppProperties propertyLoader(ResourceLoader resourceLoader) throws IOException {
		Resource[] attackVectorsResources = new PathMatchingResourcePatternResolver()
				.getResources("classpath:/attackvectors/*.properties");
		Properties attackVectorProperties = new Properties();
		for (Resource attackVectorResource : attackVectorsResources) {
			PropertiesLoaderUtils.fillProperties(attackVectorProperties, attackVectorResource);
		}
		VulnerableAppProperties vulnerableAppProperties = new VulnerableAppProperties(attackVectorProperties);
		return vulnerableAppProperties;
	}
}
