package org.sasanlabs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * This is the starting class for Vulnerable App application. Lets begin this amazing Journey!!!
 *
 * @author KSASAN preetkaran20@gmail.com
 */
// @boundary between #browser and #app (#bdy-browser-app) -- "Internet user to application trust
// boundary"
// @flows #browser -> #web-controllers via HTTP -- "All requests enter via Spring MVC controllers"
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
