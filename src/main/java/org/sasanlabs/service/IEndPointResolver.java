package org.sasanlabs.service;

import org.sasanlabs.service.exception.ServiceApplicationException;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public interface IEndPointResolver<T> {

	T resolve(String name) throws ServiceApplicationException;
	
}
