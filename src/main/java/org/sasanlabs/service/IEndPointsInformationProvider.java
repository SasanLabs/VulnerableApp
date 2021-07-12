package org.sasanlabs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.UnknownHostException;
import java.util.List;
import org.sasanlabs.beans.AllEndPointsResponseBean;
import org.sasanlabs.beans.ScannerResponseBean;
import org.sasanlabs.vulnerableapp.facade.schema.VulnerabilityDefinition;

/**
 * This is used for providing the entire information about the vulnerableApp like
 * VulnerableEndpoints/Levels. This information is very useful in building UI for the VulnerableApp
 * application.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public interface IEndPointsInformationProvider {

    /**
     * This Api provides entire information about the exposed vulnerable endpoints by the
     * application like all the supported vulnerabilities, levels, attack vectors etc.
     *
     * @return
     * @throws JsonProcessingException
     */
    List<AllEndPointsResponseBean> getSupportedEndPoints() throws JsonProcessingException;

    /**
     * This Api provides information required by VulnerableApp-Facade for building a facade User
     * Interface. For more information visit: {@link
     * https://github.com/SasanLabs/VulnerableApp-facade}
     */
    List<VulnerabilityDefinition> getVulnerabilityDefinitions() throws JsonProcessingException;

    /**
     * This Api provides information about the exposed vulnerable endpoints by the application. This
     * Api doesn't expose the \"help\" related information because this Api is designed for the
     * purpose of testing scanning tools like ZAP/Burp etc.
     *
     * @return {@link ScannerResponseBean}'s List
     * @throws JsonProcessingException
     * @throws UnknownHostException
     */
    List<ScannerResponseBean> getScannerRelatedEndPointInformation()
            throws JsonProcessingException, UnknownHostException;
}
