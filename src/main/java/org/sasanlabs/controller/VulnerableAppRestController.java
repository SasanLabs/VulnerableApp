package org.sasanlabs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.ScannerMetaResponseBean;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.internal.utility.FrameworkConstants;
import org.sasanlabs.internal.utility.GenericUtils;
import org.sasanlabs.internal.utility.JSONSerializationUtils;
import org.sasanlabs.internal.utility.annotations.RequestParameterLocation;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.sasanlabs.vulnerability.types.VulnerabilityType;
import org.sasanlabs.vulnerableapp.facade.schema.VulnerabilityDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** @author KSASAN preetkaran20@gmail.com */
@RestController
public class VulnerableAppRestController {

    private IEndPointsInformationProvider getAllSupportedEndPoints;

    private int port;

    public VulnerableAppRestController(
            IEndPointsInformationProvider getAllSupportedEndPoints,
            @Value("${server.port}") int port) {
        this.getAllSupportedEndPoints = getAllSupportedEndPoints;
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
     */
    @GetMapping
    @RequestMapping("/scanner")
    public List<ScannerResponseBean> getScannerRelatedInformation()
            throws JsonProcessingException, UnknownHostException {
        return getAllSupportedEndPoints.getScannerRelatedEndPointInformation();
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
    public String sitemapForPassiveScanners() throws JsonProcessingException, UnknownHostException {
        List<AllEndPointsResponseBean> allEndPoints = allEndPointsJsonResponse();
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
                                        .append(FrameworkConstants.HTTP)
                                        .append(GenericUtils.LOCALHOST)
                                        .append(FrameworkConstants.COLON)
                                        .append(port)
                                        .append(FrameworkConstants.SLASH)
                                        .append(FrameworkConstants.VULNERABLE_APP)
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
