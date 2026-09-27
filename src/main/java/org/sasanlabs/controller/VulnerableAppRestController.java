package org.sasanlabs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.ScannerMetaResponseBean;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.benchmark.model.ExpectedIssue;
import org.sasanlabs.benchmark.service.IExpectedIssuesProvider;
import org.sasanlabs.internal.utility.FrameworkConstants;
import org.sasanlabs.internal.utility.JSONSerializationUtils;
import org.sasanlabs.internal.utility.annotations.RequestParameterLocation;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.sasanlabs.vulnerableapp.facade.schema.VulnerabilityDefinition;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author KSASAN preetkaran20@gmail.com
 */
@Profile("public")
@RestController
public class VulnerableAppRestController {

    private static final String DEPRECATION_HEADER = "Deprecation";

    private static final String LINK_HEADER = "Link";

    private static final String SUNSET_HEADER = "Sunset";

    private static final String DAST_PATH = "scanner/dast";

    private IEndPointsInformationProvider getAllSupportedEndPoints;

    private IExpectedIssuesProvider expectedIssuesProvider;

    private int port;

    public VulnerableAppRestController(
            IEndPointsInformationProvider getAllSupportedEndPoints,
            IExpectedIssuesProvider expectedIssuesProvider) {
        this.getAllSupportedEndPoints = getAllSupportedEndPoints;
        this.expectedIssuesProvider = expectedIssuesProvider;
        this.port = port;
    }

    /**
     * @return Entire information for the application.
     * @throws JsonProcessingException
     */
    @GetMapping
    @RequestMapping("/allEndPoint")
    public String allEndPoints() throws JsonProcessingException {
        return "<pre>"
                + JSONSerializationUtils.serializeWithPrettyPrintJSON(
                        getAllSupportedEndPoints.getSupportedEndPoints())
                + "</pre>";
    }

    /**
     * Endpoint used by VulnerableApp-nginx for making a distributed vulnerable application.
     *
     * @return
     * @throws JsonProcessingException
     */
    @GetMapping
    @RequestMapping("/VulnerabilityDefinitions")
    public List<VulnerabilityDefinition> getVulnerabilityDefinitions()
            throws JsonProcessingException {
        return getAllSupportedEndPoints.getVulnerabilityDefinitions();
    }

    /**
     * This Endpoint is used to provide the entire information about the application like Supported
     * Vulnerabilities, Levels etc. Currently our thought process is that UI can be built entirely
     * using this information alone and we have build the UI by only using information provided by
     * this Rest Endpoint.
     *
     * <p>This is the backbone behind the entire UI of VulnerableApp.
     *
     * @return Entire information for the application.
     * @throws JsonProcessingException
     */
    @GetMapping
    @RequestMapping("/allEndPointJson")
    public List<AllEndPointsResponseBean> allEndPointsJsonResponse()
            throws JsonProcessingException {
        return getAllSupportedEndPoints.getSupportedEndPoints();
    }

    /**
     * This Endpoint is used to provide the vulnerability information which is useful for testing
     * scanners like ZAP/Burp
     *
     * @return {@link ScannerResponseBean}s
     * @throws JsonProcessingException
     * @throws UnknownHostException
     * @deprecated use {@code /scanner/dast}, which serves the same body under the name every
     *     application uses. This path stays until September 2027 so existing scanners keep working;
     *     its responses carry {@code Deprecation} and {@code Sunset} headers, plus a {@code Link}
     *     header identifying the successor.
     */
    @Deprecated(forRemoval = true)
    @GetMapping
    @RequestMapping("/scanner")
    public ResponseEntity<List<ScannerResponseBean>> getScannerRelatedInformation(
            HttpServletRequest request) throws JsonProcessingException, UnknownHostException {
        String appUrl = applicationUrl(request);
        return ResponseEntity.ok()
                .header(DEPRECATION_HEADER, "@1786896221")
                .header(SUNSET_HEADER, "Thu, 30 Sep 2027 23:59:59 GMT")
                .header(LINK_HEADER, "<" + appUrl + DAST_PATH + ">; rel=\"successor-version\"")
                .body(getAllSupportedEndPoints.getScannerRelatedEndPointInformation(appUrl));
    }

    /**
     * Serves the same response as the deprecated bare {@code /scanner} endpoint, under the {@code
     * /scanner/dast} name used across the applications so that a scanner can ask every application
     * for its DAST ground truth the same way.
     *
     * @return {@link ScannerResponseBean}s
     * @throws JsonProcessingException
     * @throws UnknownHostException
     */
    @GetMapping
    @RequestMapping("/scanner/dast")
    public List<ScannerResponseBean> getDastScannerRelatedInformation(HttpServletRequest request)
            throws JsonProcessingException, UnknownHostException {
        return getAllSupportedEndPoints.getScannerRelatedEndPointInformation(
                applicationUrl(request));
    }

    /**
     * Serves the SAST ground truth that {@code /scanner/benchmark} grades against, as JSON, so the
     * comparator and the facade's cross-app aggregation can consume it the same way they consume
     * the DAST ground truth from {@code /scanner/dast}.
     *
     * @return the parsed expected issues; cached after the first read
     * @throws IOException if the ground truth cannot be read
     */
    @GetMapping
    @RequestMapping("/scanner/sast")
    public List<ExpectedIssue> getSastScannerRelatedInformation() throws IOException {
        return expectedIssuesProvider.getExpectedIssues();
    }

    /**
     * Builds the externally reachable base URL of the application from the incoming request, so
     * that the URLs handed to a scanner point back at the host it actually called.
     *
     * @return base URL ending in a slash, e.g. {@code http://localhost:9090/VulnerableApp/}
     */
    private String applicationUrl(HttpServletRequest request) {
        // The deployment's own context path, not a hardcoded one:
        // `server.servlet.context-path` is configurable, so a fixed `/VulnerableApp`
        // describes this deployment only while that default is in force. Empty for a
        // root deployment.
        return new StringBuilder()
                .append(request.getScheme()) // http or https
                .append("://")
                .append(request.getServerName()) // actual hostname/IP
                .append(FrameworkConstants.COLON)
                .append(request.getServerPort()) // actual port
                .append(request.getContextPath())
                .append(FrameworkConstants.SLASH)
                .toString();
    }

    /**
     * This Endpoint is used to provide the metadata about the scanner response bean which is useful
     * for mapping naming conventions across applications.
     *
     * @return {@link ScannerMetaResponseBean}
     * @throws JsonProcessingException
     * @throws UnknownHostException
     */
    @GetMapping
    @RequestMapping("/scanner/metadata")
    public ScannerMetaResponseBean getScannerRelatedMetaInformation() {
        return new ScannerMetaResponseBean(
                Arrays.asList(VulnerabilityType.values()),
                Arrays.asList(RequestParameterLocation.values()));
    }

    /**
     * This Endpoint is exposed to help the scanners in finding the Vulnerable EndPoints. Here we
     * are not using any library as we need a very basic sitemap and we don't want to make
     * VulnerableApp heavy.
     *
     * @return XML String which is representing the sitemap format.
     * @throws JsonProcessingException
     * @throws UnknownHostException
     */
    @RequestMapping("/sitemap.xml")
    public String sitemapForPassiveScanners(HttpServletRequest request)
            throws JsonProcessingException, UnknownHostException {
        List<AllEndPointsResponseBean> allEndPoints = allEndPointsJsonResponse();
        // Dynamically resolve host from the incoming request
        String scheme = request.getScheme(); // http or https
        String serverName = request.getServerName(); // actual hostname/IP
        int serverPort = request.getServerPort(); // actual port
        // Same reasoning as the scanner endpoint: the sitemap must advertise URLs inside the
        // context path this instance is actually served under.
        String contextPath = request.getContextPath();

        StringBuilder xmlBuilder =
                new StringBuilder(
                        FrameworkConstants.GENERAL_XML_HEADER
                                + FrameworkConstants.SITEMAP_URLSET_TAG_START);
        for (AllEndPointsResponseBean endPoint : allEndPoints) {
            endPoint.getLevelDescriptionSet()
                    .forEach(
                            level -> {
                                xmlBuilder
                                        .append(FrameworkConstants.SITEMAP_URL_TAG_START)
                                        .append(FrameworkConstants.NEXT_LINE)
                                        .append(FrameworkConstants.SITEMAP_LOC_TAG_START)
                                        .append(FrameworkConstants.NEXT_LINE)
                                        .append(scheme)
                                        .append("://")
                                        .append(serverName)
                                        .append(FrameworkConstants.COLON)
                                        .append(serverPort)
                                        .append(contextPath)
                                        .append(FrameworkConstants.SLASH)
                                        .append(endPoint.getName())
                                        .append(FrameworkConstants.SLASH)
                                        .append(level.getLevel())
                                        .append(FrameworkConstants.NEXT_LINE)
                                        .append(FrameworkConstants.SITEMAP_LOC_TAG_END)
                                        .append(FrameworkConstants.NEXT_LINE)
                                        .append(FrameworkConstants.SITEMAP_URL_TAG_END)
                                        .append(FrameworkConstants.NEXT_LINE);
                            });
        }
        xmlBuilder.append(FrameworkConstants.SITEMAP_URLSET_TAG_END);
        return xmlBuilder.toString();
    }
}
