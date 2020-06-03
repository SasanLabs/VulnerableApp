package org.sasanlabs.service;

import org.sasanlabs.service.bean.RequestBean;
import org.sasanlabs.service.bean.ResponseBean;
import org.sasanlabs.service.exception.ServiceApplicationException;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public interface RequestDelegator {

	<T> ResponseBean<T> delegate(RequestBean requestBean) throws ServiceApplicationException;
	
}
