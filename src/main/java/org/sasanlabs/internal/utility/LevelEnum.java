package org.sasanlabs.internal.utility;

import org.sasanlabs.service.exception.ExceptionStatusCodeEnum;
import org.sasanlabs.service.exception.ServiceApplicationException;

/**
 * Represents the Level of the Vulnerability Type.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public enum LevelEnum {
    LEVEL_1,
    LEVEL_2,
    LEVEL_3,
    LEVEL_4,
    LEVEL_5,
    LEVEL_6,
    LEVEL_7,
    LEVEL_8,
    LEVEL_9,
    LEVEL_10,
    LEVEL_11,
    LEVEL_12,
    SECURE;

    public static LevelEnum getLevelEnumByName(String name) throws ServiceApplicationException {
        for (LevelEnum levelEnum : LevelEnum.values()) {
            if (levelEnum.name().equalsIgnoreCase(name)) {
                return levelEnum;
            }
        }
        throw new ServiceApplicationException(ExceptionStatusCodeEnum.INVALID_LEVEL, name);
    }
}
