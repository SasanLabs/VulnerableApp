package org.sasanlabs;

import java.util.Locale;

import javax.servlet.ServletRequest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
@SpringBootApplication
public class Application {

	@Bean
	public LocaleResolver LocaleResolver(ServletRequest request) {
		SessionLocaleResolver sessionLocaleResolver = new SessionLocaleResolver();
		sessionLocaleResolver.setDefaultLocale(Locale.US);
		return sessionLocaleResolver;
	}

	@Bean
	public ReloadableResourceBundleMessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasename("classpath:i18n/messages");
		messageSource.setCacheSeconds(100);
		messageSource.setDefaultEncoding("UTF-8");
		return messageSource;
	}
	
	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(Application.class, args);		
	}

}
