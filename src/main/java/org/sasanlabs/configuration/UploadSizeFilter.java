package org.sasanlabs.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.sasanlabs.internal.utility.LevelConstants;
import org.sasanlabs.service.vulnerability.fileupload.UnrestrictedFileUpload;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Replaces the old MultipartFilter override. Uploads are unlimited globally (see
 * application.properties), so this filter re-applies a size limit on every path except the
 * deliberately unrestricted upload level.
 */
@Component
public class UploadSizeFilter extends OncePerRequestFilter {

    private static final long DEFAULT_LIMIT = 1024 * 1024; // 1 MB
    private static final String UNRESTRICTED_PATH =
            "/" + UnrestrictedFileUpload.CONTROLLER_PATH + "/" + LevelConstants.LEVEL_9;

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String contentType = req.getContentType();
        boolean isMultipart =
                contentType != null && contentType.toLowerCase().startsWith("multipart/");

        if (isMultipart
                && !UNRESTRICTED_PATH.equals(req.getServletPath())
                && req.getContentLengthLong() > DEFAULT_LIMIT) {
            res.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            return;
        }
        chain.doFilter(req, res);
    }
}
