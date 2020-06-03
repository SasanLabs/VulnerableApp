package org.sasanlabs.service;

import org.sasanlabs.service.bean.RequestBean;
import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ServiceApplicationException;

/**
 * {@code RequestDelegator} is used for intelligent routing of Rest call to the exact serving endpoint
 * in the {@code VulnerableAppRestController}.
 * 
 * @author KSASAN preetkaran20@gmail.com
 */
public interface RequestDelegator {

	<T> ResponseBean<T> delegate(RequestBean requestBean) throws ServiceApplicationException;
	
}
