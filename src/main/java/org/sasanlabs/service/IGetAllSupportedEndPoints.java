package org.sasanlabs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.sasanlabs.beans.AllEndPointsResponseBean;

/**
 * This is used for providing the entire information about the vulnerableApp like
 * VulnerableEndpoints/Levels. This information is very useful in building UI for the VulnerableApp
 * application.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public interface IGetAllSupportedEndPoints {

    String getSupportedEndPoints() throws JsonProcessingException;

    List<AllEndPointsResponseBean> getSupportedEndPoint() throws JsonProcessingException;
}
