package org.sasanlabs.configuration;

import org.sasanlabs.service.vulnerability.fileupload.UnrestrictedFileUpload;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link UnrestrictedFileUpload} writes the uploads of its {@code root}-backed levels to a real OS
 * directory outside the application's classpath (see {@link
 * UnrestrictedFileUpload#uploadsBaseDirectory()}), so Spring's default static resource handler,
 * which only serves {@code classpath:/static/}, never sees them. This registers the same directory
 * as the source for the {@code /upload/**} URLs those levels return, so an uploaded file is
 * actually fetchable back.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@Profile("unsafe")
@Configuration
public class UnrestrictedFileUploadWebConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(
                        "file:"
                                + UnrestrictedFileUpload.rootUploadDirectory().toAbsolutePath()
                                + "/");
    }
}
