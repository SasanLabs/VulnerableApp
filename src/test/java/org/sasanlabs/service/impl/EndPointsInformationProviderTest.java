package org.sasanlabs.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sasanlabs.configuration.VulnerableAppProperties;
import org.sasanlabs.internal.utility.EnvUtils;
import org.sasanlabs.internal.utility.MessageBundle;

/**
 * Unit tests for {@link EndPointsInformationProvider#getMetaInformation(String)}. The method is
 * private, so it is exercised through reflection.
 */
class EndPointsInformationProviderTest {

    private EndPointsInformationProvider provider;
    private MessageBundle messageBundle;

    private static final String NOT_APPLICABLE = "NOT_APPLICABLE";

    @BeforeEach
    void setUp() {
        messageBundle = mock(MessageBundle.class);
        EnvUtils envUtils = mock(EnvUtils.class);
        VulnerableAppProperties vulnerableAppProperties =
                new VulnerableAppProperties(new Properties());
        provider =
                new EndPointsInformationProvider(
                        envUtils, messageBundle, vulnerableAppProperties, 9090);
    }

    private String invokeGetMetaInformation(String key)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method method =
                EndPointsInformationProvider.class.getDeclaredMethod(
                        "getMetaInformation", String.class);
        method.setAccessible(true);
        return (String) method.invoke(provider, key);
    }

    @Test
    void shouldReturnNullWhenInputIsNotApplicable() throws Exception {
        assertNull(invokeGetMetaInformation(NOT_APPLICABLE));
    }

    @Test
    void shouldReturnNullWhenInputIsNull() throws Exception {
        assertNull(invokeGetMetaInformation(null));
    }

    @Test
    void shouldReturnNullWhenInputIsBlank() throws Exception {
        assertNull(invokeGetMetaInformation(""));
        assertNull(invokeGetMetaInformation("   "));
    }

    @Test
    void shouldReturnNullWhenMessageBundleReturnsBlank() throws Exception {
        doReturn("").when(messageBundle).getString(anyString(), any());
        assertNull(invokeGetMetaInformation("SOME_KEY"));
    }

    @Test
    void shouldReturnNullWhenMessageBundleReturnsNotApplicable() throws Exception {
        doReturn(NOT_APPLICABLE).when(messageBundle).getString(anyString(), any());
        assertNull(invokeGetMetaInformation("SOME_KEY"));
    }

    @Test
    void shouldReturnValidStringWhenMessageBundleResolvesKey() throws Exception {
        doReturn("Cookie").when(messageBundle).getString("JWT_SOURCE_COOKIE", null);
        assertEquals("Cookie", invokeGetMetaInformation("JWT_SOURCE_COOKIE"));
    }
}
