package org.sasanlabs.internal.utility;

/**
 * This is the Framework Constants which are used internally by the VulnerableApp Framework.
 *
 * @author preetkaran20@gmail.com KSASAN
 */
public interface FrameworkConstants {

    // Site map related constants
    String GENERAL_XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    String SITEMAP_URLSET_TAG_START =
            "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\" >\n";
    String SITEMAP_URL_TAG_START = "<url>";
    String SITEMAP_URL_TAG_END = "</url>";
    String SITEMAP_LOC_TAG_START = "<loc>";
    String SITEMAP_LOC_TAG_END = "</loc>";
    String SITEMAP_URLSET_TAG_END = "</urlset>";

    String HTTP = "http://";
    String HTTPS = "https://";

    String WWW = "www.";
    String COLON = ":";
    String SLASH = "/";
    String NEXT_LINE = "\n";

    // As VulnerableApp is added to each URL hence creating a constant for reference.
    String VULNERABLE_APP = "VulnerableApp";
}
