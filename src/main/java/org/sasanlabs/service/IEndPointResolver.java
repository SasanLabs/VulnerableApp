package org.sasanlabs.service;

import org.sasanlabs.controller.VulnerableAppRestController;
import org.sasanlabs.service.exception.ServiceApplicationException;

/**
 * Used for resolving the VulnerableEndPoint which is annotated by {@link
 * VulnerableAppRestController}
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public interface IEndPointResolver<T> {

    T resolve(String name) throws ServiceApplicationException;
}
