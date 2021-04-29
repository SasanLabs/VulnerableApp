package org.sasanlabs.internal.utility;

/** Represents the Level of the Vulnerability Type that is considered secure. */
public interface SecureConstants {
    String SECURE_1 = "SECURE_1";
    String SECURE_2 = "SECURE_2";
    String SECURE_3 = "SECURE_3";
    String SECURE_4 = "SECURE_4";
    String SECURE_5 = "SECURE_5";
    String SECURE_6 = "SECURE_6";
    String SECURE_7 = "SECURE_7";
    String SECURE_8 = "SECURE_8";
    String SECURE_9 = "SECURE_9";
    String SECURE_10 = "SECURE_10";
    String SECURE_11 = "SECURE_11";
    String SECURE_12 = "SECURE_12";

    /**
     * Get the incremented ordinal value so that it won't collide with non-incremented ordinal
     * values.
     */
    static int getIncrementedOrdinal(String level) {
        int separatorIndex = level.indexOf("_");

        return separatorIndex > 0
                ? Integer.MAX_VALUE / 2 + Integer.parseInt(level.substring(separatorIndex + 1))
                : Integer.MAX_VALUE;
    }
}
