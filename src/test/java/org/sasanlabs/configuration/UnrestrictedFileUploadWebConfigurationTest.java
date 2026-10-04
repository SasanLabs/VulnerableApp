package org.sasanlabs.configuration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.sasanlabs.service.vulnerability.fileupload.UnrestrictedFileUpload;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class UnrestrictedFileUploadWebConfigurationTest {

    @Test
    void addResourceHandlers_servesUploadFromTheRealWritableDirectory() throws IOException {
        // Forces the directory to exist, same as the constructor would at startup.
        new UnrestrictedFileUpload();
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);
        when(registry.addResourceHandler("/upload/**")).thenReturn(registration);

        new UnrestrictedFileUploadWebConfiguration().addResourceHandlers(registry);

        String expectedLocation =
                "file:" + UnrestrictedFileUpload.rootUploadDirectory().toAbsolutePath() + "/";
        org.mockito.Mockito.verify(registration).addResourceLocations(eq(expectedLocation));
    }
}
