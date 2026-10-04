package org.sasanlabs.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sasanlabs.service.vulnerability.fileupload.UnrestrictedFileUpload;
import org.springframework.core.io.Resource;
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

        ArgumentCaptor<Resource> location = ArgumentCaptor.forClass(Resource.class);
        verify(registration).addResourceLocations(location.capture());
        assertThat(location.getValue().getFile().toPath())
                .isEqualTo(UnrestrictedFileUpload.rootUploadDirectory().toAbsolutePath());
    }
}
