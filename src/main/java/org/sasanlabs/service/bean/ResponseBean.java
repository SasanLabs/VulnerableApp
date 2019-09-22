package org.sasanlabs.service.bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author KSASAN preetkaran20@gmail.com
 * 
 *         DTO which is send from the Service Layer to Controller Layer
 */
public class ResponseBean {

	private Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
	private String body;
	private int httpStatusCode;

	public Map<String, List<String>> getResponseHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}

	public void setHttpStatusCode(int httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}

	public ResponseBean(String body) {
		this.body = body;
	}

	public ResponseBean(Map<String, List<String>> responseHeader, String body) {
		this.responseHeaders = responseHeader;
		this.body = body;
	}

	public ResponseBean(int httpStatusCode, String body) {
		this.httpStatusCode = httpStatusCode;
		this.body = body;
	}
	
	public ResponseBean(int httpStatusCode,Map<String, List<String>> responseHeader) {
		this.httpStatusCode = httpStatusCode;
		this.responseHeaders = responseHeader;
	}

	public ResponseBean(Map<String, List<String>> responseHeader, String body, int httpStatusCode) {
		this.responseHeaders = responseHeader;
		this.body = body;
		this.httpStatusCode = httpStatusCode;
	}
}
