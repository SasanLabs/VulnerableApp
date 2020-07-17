package org.sasanlabs.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.controller.exception.ControllerException;
import org.sasanlabs.internal.utility.FrameworkConstants;
import org.sasanlabs.internal.utility.JSONSerializationUtils;
import org.sasanlabs.internal.utility.ResponseMapper;
import org.sasanlabs.service.IEndPointResolver;
import org.sasanlabs.service.IEndPointsInformationProvider;
import org.sasanlabs.service.RequestDelegator;
import org.sasanlabs.service.bean.RequestBean;
import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ServiceApplicationException;
import org.sasanlabs.service.vulnerability.ICustomVulnerableEndPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

/** @author KSASAN preetkaran20@gmail.com */
@RestController
public class VulnerableAppRestController {

    private RequestDelegator requestDelegator;

    private IEndPointsInformationProvider getAllSupportedEndPoints;

    private int port;

    @Autowired
    public VulnerableAppRestController(
            RequestDelegator buildPayload,
            IEndPointResolver<ICustomVulnerableEndPoint> endPointResolver,
            IEndPointsInformationProvider getAllSupportedEndPoints,
            @Value("${server.port}") int port) {
        this.requestDelegator = buildPayload;
        this.getAllSupportedEndPoints = getAllSupportedEndPoints;
        this.port = port;
    }

    /**
     * Rest end point which is used to route calls to respective VulnerableServiceRestEndpoints
     * based on the Level and vulnerability type.
     *
     * <p>This is the backbone behind all the intelligent routing in the VulnerableApp.
     *
     * @param <T> ResonseType
     * @param allParams Represents the Query Params
     * @param endPoint This is the vulnerability name
     * @param level Level of the Vulnerability.
     * @param requestEntity
     * @return ResponseEntity
     * @throws ControllerException
     */
    @RequestMapping("/vulnerable/{endPoint}/{level}")
    public <T> ResponseEntity<T> endPointHandler(
            @RequestParam Map<String, String> allParams,
            @PathVariable("endPoint") String endPoint,
            @PathVariable("level") String level,
            RequestEntity<String> requestEntity)
            throws ControllerException {
        RequestBean requestBean = new RequestBean();
        // Added to restrict buffer overflow reported by ZAP and Burp
        if (endPoint.length() > 250) {
            endPoint = endPoint.substring(0, 250);
        }

        if (level.length() > 250) {
            level = level.substring(0, 250);
        }
        requestBean.setEndPoint(endPoint);
        requestBean.setLevel(level);
        requestBean.setQueryParams(allParams);
        requestBean.setUrl(requestEntity.getUrl().toString());
        Set<String> headerNames = requestEntity.getHeaders().keySet();

        for (String headerName : headerNames) {
            requestBean.getHeaders().put(headerName, new ArrayList<>());
            List<String> headerValues = requestEntity.getHeaders().get(headerName);
            for (String headerValue : headerValues) {
                requestBean.getHeaders().get(headerName).add(headerValue);
            }
        }
        if (requestEntity.getMethod().equals(HttpMethod.POST) && requestEntity.hasBody()) {
            requestBean.setBody(requestEntity.getBody());
        }
        try {
            ResponseBean<T> responseBean = requestDelegator.delegate(requestBean);
            return ResponseMapper.buildResponseEntity(responseBean);
        } catch (ServiceApplicationException e) {
            throw new ControllerException(e);
        }
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
     * This Endpoint is used to provide the vulnerability information which is useful for testing scanners
     * like ZAP/Burp
     * 
     * @return {@link ScannerResponseBean}s
     * @throws JsonProcessingException
     * @throws UnknownHostException
     */
    @GetMapping
    @RequestMapping("/scanner")
    public List<ScannerResponseBean> getScannerRelatedEndpointInformation() throws JsonProcessingException, UnknownHostException {
        return getAllSupportedEndPoints.getScannerRelatedEndPointInformation();
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
    public String sitemapForPassiveScanners()
            throws JsonProcessingException, UnknownHostException {
        List<AllEndPointsResponseBean> allEndPoints = allEndPointsJsonResponse();
        StringBuilder xmlBuilder =
                new StringBuilder(
                        FrameworkConstants.GENERAL_XML_HEADER
                                + FrameworkConstants.SITEMAP_URLSET_TAG_START);
        String ipAddress = InetAddress.getLocalHost().getHostAddress();
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
                                        .append(ipAddress)
                                        .append(FrameworkConstants.COLON)
                                        .append(port)
                                        .append(FrameworkConstants.SLASH)
                                        .append(FrameworkConstants.VULNERABLE)
                                        .append(FrameworkConstants.SLASH)
                                        .append(endPoint.getName())
                                        .append(FrameworkConstants.SLASH)
                                        .append(level.getLevelEnum().name())
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
