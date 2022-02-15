package org.sasanlabs.internal.utility;

/**
 * This is the Framework Constants which are used internally by the VulnerableApp Framework.
 *
 * @author preetkaran20@gmail.com KSASAN
 */
public class FrameworkConstants {

    // Site map related constants
    public static final String GENERAL_XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    public static final String SITEMAP_URLSET_TAG_START =
            "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" >\n";
    public static final String SITEMAP_URL_TAG_START = "<url>";
    public static final String SITEMAP_URL_TAG_END = "</url>";
    public static final String SITEMAP_LOC_TAG_START = "<loc>";
    public static final String SITEMAP_LOC_TAG_END = "</loc>";
    public static final String SITEMAP_URLSET_TAG_END = "</urlset>";

    public static final String HTTP = "http://";
    public static final String HTTPS = "https://";
    public static final String COLON = ":";
    public static final String SLASH = "/";
    public static final String NEXT_LINE = "\n";

    // As VulnerableApp is added to each URL hence creating a constant for reference.
    public static final String VULNERABLE_APP = "VulnerableApp";
}
