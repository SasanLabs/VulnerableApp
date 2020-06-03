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
 * This is the Configuration Class for Injecting Configurations into the Context.
 * 
 * @author KSASAN preetkaran20@gmail.com 
 */
@Configuration
public class VulnerableAppConfiguration {

	private static final String I18N_MESSAGE_FILE_LOCATION = "classpath:i18n/messages";
	private static final String ATTACK_VECTOR_PAYLOAD_PROPERTY_FILES_LOCATION_PATTERN = "classpath:/attackvectors/*.properties";
	
	/**
	 * Will Inject MessageBundle into messageSource bean.
	 * 
	 * @return resourceBundle
	 */
	@Bean
	public ReloadableResourceBundleMessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasename(I18N_MESSAGE_FILE_LOCATION);
		messageSource.setCacheSeconds(100);
		messageSource.setDefaultEncoding("UTF-8");
		return messageSource;
	}

	/**
	 * This method reads all the property which are useful for vulnerableApp and
	 * then injects them into the context so that entire application can use it.
	 * 
	 * @param resourceLoader
	 * @return {@link VulnerableAppProperties} which is injected in spring context.
	 * @throws IOException
	 */
	@Bean
	public VulnerableAppProperties propertyLoader(ResourceLoader resourceLoader) throws IOException {
		Resource[] attackVectorsResources = new PathMatchingResourcePatternResolver()
				.getResources(ATTACK_VECTOR_PAYLOAD_PROPERTY_FILES_LOCATION_PATTERN);
		Properties attackVectorProperties = new Properties();
		for (Resource attackVectorResource : attackVectorsResources) {
			PropertiesLoaderUtils.fillProperties(attackVectorProperties, attackVectorResource);
		}
		VulnerableAppProperties vulnerableAppProperties = new VulnerableAppProperties(attackVectorProperties);
		return vulnerableAppProperties;
	}
}
