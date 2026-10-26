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
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/VulnerableApp/h2/login.do");
        request.setContextPath("/VulnerableApp");
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
        request.setParameter("url", "jdbc:h2:tcp://internal-host:9092/x");

        assertNotNull(run(filter, request).getRequest());
    }
}
