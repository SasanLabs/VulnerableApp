package org.sasanlabs.service;

import java.util.List;

import org.sasanlabs.beans.AllEndPointsResponseBean;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public interface IGetAllSupportedEndPoints {

	String getSupportedEndPoints() throws JsonProcessingException;
	
	List<AllEndPointsResponseBean> getSupportedEndPoint() throws JsonProcessingException;
}
