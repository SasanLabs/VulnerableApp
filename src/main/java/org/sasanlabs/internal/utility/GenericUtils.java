package org.sasanlabs.internal.utility;

/**
 * Generic Internal Utility class.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
public final class GenericUtils {

    public static final String LOCALHOST = "127.0.0.1";

    private GenericUtils() {}

    /**
     * @deprecated
     * @param payload
     * @return
     */
    @Deprecated
    public static String wrapPayloadInGenericVulnerableAppTemplate(String payload) {
        String generalPayload =
                "<html><title>Security Testing</title><body><h1>Vulnerable Application </h1> %s </body></html>";
        return String.format(generalPayload, payload);
    }
}
