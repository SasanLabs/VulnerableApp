package org.sasanlabs.service;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public interface IGetAllSupportedEndPoints {

	String getSupportedEndPoints() throws JsonProcessingException;
	
}
