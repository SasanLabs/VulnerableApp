package org.sasanlabs.vulnerability.utils;

/** @author KSASAN preetkaran20@gmail.com */
public interface Constants {

    String NULL_BYTE_CHARACTER = String.valueOf((char) 0);
    String EYE_CATCHER = "0W45pz4p";

    // Constant used by SQLInjection Vulnerability
    String ID = "id";
    String LOCALHOST = "localhost";

    static String getEyeCatcher() {
        return EYE_CATCHER;
    }
}
