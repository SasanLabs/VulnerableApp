package org.sasanlabs.internal.utility;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

public class AnnotationScannerUtility {

	public static final Map<String, Class<?>> CONTEXT = new HashMap<>();

	public AnnotationScannerUtility() {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(true);
		scanner.addIncludeFilter(new AnnotationTypeFilter(VulnerableServiceRestEndPoint.class));
		for (BeanDefinition beanDefinition : scanner.findCandidateComponents("org.sasanlabs.vulnerability")) {
			System.out.println(beanDefinition.getBeanClassName());
			CONTEXT.put(beanDefinition.getClass().getAnnotation(VulnerableServiceRestEndPoint.class).name(),
					beanDefinition.getClass());
		}
	}

}
