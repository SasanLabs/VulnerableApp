package org.sasanlabs.internal.utility;

import java.util.Base64;

public class EncodingUtils {
    public static String bytesToHex(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 2);
        for (byte value : data) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    public static String encodeBase64(String rawText) {
        return Base64.getEncoder().encodeToString(rawText.getBytes());
    }
}
