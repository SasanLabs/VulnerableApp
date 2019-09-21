package org.sasanlabs.internal.utility;

import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.sasanlabs.service.exception.ServiceApplicationException;

/**
 * @author KSASAN preetkaran20@gmail.com Depicts the Level of Security
 */
public enum LevelEnum {

	UNSECURE, LOW, MEDIUM, HIGH, VERY_HIGH, SECURE;

	public static LevelEnum getLevelEnumByName(String name) throws ServiceApplicationException {
		for (LevelEnum levelEnum : LevelEnum.values()) {
			if (levelEnum.name().equalsIgnoreCase(name)) {
				return levelEnum;
			}
		}
		throw new ServiceApplicationException(ExceptionStatusCodeEnum.INVALID_LEVEL, name);
	}
}
