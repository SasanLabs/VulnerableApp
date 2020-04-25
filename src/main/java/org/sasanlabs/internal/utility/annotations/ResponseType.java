package org.sasanlabs.internal.utility.annotations;

/**
 * Usage of this is to distinguish what is the response type from the VulnerableRestEndpoint.
 * Actually we want backend to provide entire information to frontend so that frontend 
 * is not tightly coupled with backend. This is done with an intent that the backend can
 * be consumer by any application without the use of frontend like say a CTF hosting 
 * platform need not to use the UserInterface provided by vulnerableApp.
 * 
 * So this information will be returned with the response of /allEndPoints and /allEndPointsJson
 * so that consumer can write the code as per the provided information by these endpoints.
 * 
 * @author KSASAN preetkaran20@gmail.com
 */
public enum ResponseType {
	ENTIRE_HTML_PAGE,
	JSON,
	HTML_TAGS_ONLY
}
