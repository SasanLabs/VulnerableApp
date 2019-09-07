package org.sasanlabs;

import org.sasanlabs.internal.utility.AnnotationScannerUtility;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(Application.class, args);
		AnnotationScannerUtility annotationScannerUtility = new AnnotationScannerUtility();
		
	}

}
