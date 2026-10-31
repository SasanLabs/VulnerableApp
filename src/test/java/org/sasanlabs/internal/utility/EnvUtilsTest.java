package org.sasanlabs.internal.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRestController;
import org.sasanlabs.internal.utility.annotations.VulnerableAppWebSocketController;
import org.springframework.context.ApplicationContext;

class EnvUtilsTest {

    private final ApplicationContext context = mock(ApplicationContext.class);
    private final EnvUtils envUtils = new EnvUtils(context);

    @Test
    void getAllClassesAnnotatedWithVulnerableAppRestController_returnsTheRestBeans() {
        Map<String, Object> beans = Map.of("Rest", new Object());
        when(context.getBeansWithAnnotation(VulnerableAppRestController.class)).thenReturn(beans);

        assertThat(envUtils.getAllClassesAnnotatedWithVulnerableAppRestController())
                .isSameAs(beans);
    }

    @Test
    void getAllClassesAnnotatedWithVulnerableAppWebSocketController_returnsTheWebSocketBeans() {
        Map<String, Object> beans = Map.of("WebSocket", new Object());
        when(context.getBeansWithAnnotation(VulnerableAppWebSocketController.class))
                .thenReturn(beans);

        assertThat(envUtils.getAllClassesAnnotatedWithVulnerableAppWebSocketController())
                .isSameAs(beans);
    }
}
