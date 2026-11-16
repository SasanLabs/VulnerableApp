package org.sasanlabs.configuration;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Restricts the H2 console to the application's own database. The console otherwise accepts any
 * JDBC URL (e.g. jdbc:h2:tcp://host:port/...), which would let users make the server connect to
 * arbitrary hosts on its network. Read-only database users limit what can be done after login, not
 * where the console connects.
 */
@Component
@ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class H2ConsoleJdbcUrlFilter extends OncePerRequestFilter {

    private static final String H2_DRIVER = "org.h2.Driver";

    private final String consolePath;
    private final String allowedUrl;

    public H2ConsoleJdbcUrlFilter(
            @Value("${spring.h2.console.path:/h2-console}") String consolePath,
            @Value("${spring.datasource.application.url}") String allowedUrl) {
        this.consolePath = consolePath;
        this.allowedUrl = allowedUrl;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Use the path Tomcat mapped the request with, not the raw URI: path parameters (/h2;x=1),
        // percent-encoding (/%68%32) and non-normalized segments (//h2, /./h2) still reach the
        // console servlet but would not match a raw URI comparison.
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path += request.getPathInfo();
        }
        return !path.equals(consolePath) && !path.startsWith(consolePath + "/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String url = request.getParameter("url");
        String driver = request.getParameter("driver");
        if ((url != null && !allowedUrl.equals(url.trim()))
                || (driver != null && !driver.isBlank() && !H2_DRIVER.equals(driver.trim()))) {
            response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Only " + allowedUrl + " with " + H2_DRIVER + " is allowed");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
