package org.sasanlabs.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class H2ConsoleJdbcUrlFilterTest {

    private static final String ALLOWED_URL = "jdbc:h2:mem:testdb";

    private final H2ConsoleJdbcUrlFilter filter = new H2ConsoleJdbcUrlFilter("/h2", ALLOWED_URL);

    private static MockHttpServletRequest consoleLogin(String url, String driver) {
        return consoleLogin("/VulnerableApp/h2/login.do", url, driver);
    }

    // The servlet path and path info are what Tomcat derives when it maps the request to the H2
    // console servlet, regardless of how the raw request URI was written.
    private static MockHttpServletRequest consoleLogin(
            String requestUri, String url, String driver) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", requestUri);
        request.setContextPath("/VulnerableApp");
        request.setServletPath("/h2");
        request.setPathInfo("/login.do");
        request.setParameter("url", url);
        request.setParameter("driver", driver);
        return request;
    }

    private static MockFilterChain run(
            H2ConsoleJdbcUrlFilter filter, MockHttpServletRequest request) throws Exception {
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    void allowsApplicationDatabase() throws Exception {
        assertNotNull(run(filter, consoleLogin(ALLOWED_URL, "org.h2.Driver")).getRequest());
    }

    @Test
    void blocksRemoteTcpUrl() throws Exception {
        MockHttpServletRequest request =
                consoleLogin("jdbc:h2:tcp://internal-host:9092/x", "org.h2.Driver");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest());
        assertEquals(403, response.getStatus());
    }

    @Test
    void blocksRemoteTcpUrlWhenRawUriDiffersFromMappedPath() throws Exception {
        for (String rawUri :
                new String[] {
                    "/VulnerableApp/h2;probe=1/login.do",
                    "/VulnerableApp/%68%32/login.do",
                    "/VulnerableApp//h2/login.do",
                    "/VulnerableApp/./h2/login.do"
                }) {
            assertNull(
                    run(
                                    filter,
                                    consoleLogin(
                                            rawUri,
                                            "jdbc:h2:tcp://internal-host:9092/x",
                                            "org.h2.Driver"))
                            .getRequest(),
                    rawUri);
        }
    }

    @Test
    void blocksUrlWithExtraSettings() throws Exception {
        assertNull(
                run(filter, consoleLogin(ALLOWED_URL + ";INIT=RUNSCRIPT FROM 'x'", "org.h2.Driver"))
                        .getRequest());
    }

    @Test
    void blocksOtherDriver() throws Exception {
        assertNull(run(filter, consoleLogin(ALLOWED_URL, "com.example.Driver")).getRequest());
    }

    @Test
    void ignoresRequestsOutsideConsole() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/VulnerableApp/CryptographicFailures/LEVEL_1");
        request.setContextPath("/VulnerableApp");
        request.setServletPath("/CryptographicFailures/LEVEL_1");
        request.setParameter("url", "jdbc:h2:tcp://internal-host:9092/x");

        assertNotNull(run(filter, request).getRequest());
    }
}
