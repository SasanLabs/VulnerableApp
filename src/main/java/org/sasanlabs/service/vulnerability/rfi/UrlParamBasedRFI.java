package org.sasanlabs.service.vulnerability.rfi;

import static org.sasanlabs.vulnerability.utils.Constants.NULL_BYTE_CHARACTER;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sasanlabs.internal.utility.GenericUtils;
import org.sasanlabs.internal.utility.LevelConstants;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRequestMapping;
import org.sasanlabs.internal.utility.annotations.VulnerableAppRestController;
import org.sasanlabs.service.vulnerability.pathTraversal.PathTraversalVulnerability;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

/** @author KSASAN preetkaran20@gmail.com */
@VulnerableAppRestController(
        descriptionLabel = "URL_BASED_RFI_INJECTION",
        value = "RemoteFileInclusion")
public class UrlParamBasedRFI {

    private static final transient Logger LOGGER =
            LogManager.getLogger(PathTraversalVulnerability.class);

    private static final String URL_PARAM_KEY = "url";

    @VulnerableAppRequestMapping(value = LevelConstants.LEVEL_1)
    public ResponseEntity<String> getVulnerablePayloadLevelUnsecure(
            @RequestParam Map<String, String> queryParams) {
        StringBuilder payload = new StringBuilder();
        String queryParameterURL = queryParams.get(URL_PARAM_KEY);
        if (queryParameterURL != null) {
            try {
                URL url = new URL(queryParameterURL);
                RestTemplate restTemplate = new RestTemplate();
                payload.append(restTemplate.getForObject(url.toURI(), String.class));
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Following error occurred:", e);
            }
        }

        return new ResponseEntity<>(
                GenericUtils.wrapPayloadInGenericVulnerableAppTemplate(payload.toString()),
                HttpStatus.OK);
    }

    @VulnerableAppRequestMapping(value = LevelConstants.LEVEL_2)
    public ResponseEntity<String> getVulnerablePayloadLevelUnsecureLevel2(
            @RequestParam Map<String, String> queryParams) {
        StringBuilder payload = new StringBuilder();
        String queryParameterURL = queryParams.get(URL_PARAM_KEY);
        if (queryParameterURL != null && queryParameterURL.contains(NULL_BYTE_CHARACTER)) {
            try {
                URL url = new URL(queryParameterURL);
                RestTemplate restTemplate = new RestTemplate();
                payload.append(restTemplate.getForObject(url.toURI(), String.class));
            } catch (IOException | URISyntaxException e) {
                LOGGER.error("Following error occurred:", e);
            }
        }

        return new ResponseEntity<>(
                GenericUtils.wrapPayloadInGenericVulnerableAppTemplate(payload.toString()),
                HttpStatus.OK);
    }
}
