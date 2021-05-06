package org.sasanlabs.internal.utility;

/**
 * Represents the Level of the Vulnerability Type.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public interface LevelConstants {
    String LEVEL_1 = "LEVEL_1";
    String LEVEL_2 = "LEVEL_2";
    String LEVEL_3 = "LEVEL_3";
    String LEVEL_4 = "LEVEL_4";
    String LEVEL_5 = "LEVEL_5";
    String LEVEL_6 = "LEVEL_6";
    String LEVEL_7 = "LEVEL_7";
    String LEVEL_8 = "LEVEL_8";
    String LEVEL_9 = "LEVEL_9";
    String LEVEL_10 = "LEVEL_10";
    String LEVEL_11 = "LEVEL_11";
    String LEVEL_12 = "LEVEL_12";

    static int getOrdinal(String level) {
        if (level.indexOf("_") > 0) {
            return Integer.parseInt(level.substring(level.indexOf("_") + 1));
        }
        return Integer.MAX_VALUE;
    }
}
