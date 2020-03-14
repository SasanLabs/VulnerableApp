package org.sasanlabs.service.bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author KSASAN preetkaran20@gmail.com
 * 
 *         DTO which is send from the Service Layer to Controller Layer
 */
public class ResponseBean<T> {

	private Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
	private T body;
	private int httpStatusCode = 200;

	public Map<String, List<String>> getResponseHeaders() {
		return responseHeaders;
	}

	public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
		this.responseHeaders = responseHeaders;
	}

	public T getBody() {
		return body;
	}

	public void setBody(T body) {
		this.body = body;
	}

	public int getHttpStatusCode() {
		return httpStatusCode;
	}

	public void setHttpStatusCode(int httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}

	public ResponseBean(T body) {
		this.body = body;
	}

	public ResponseBean(Map<String, List<String>> responseHeader, T body) {
		this.responseHeaders = responseHeader;
		this.body = body;
	}

	public ResponseBean(int httpStatusCode, T body) {
		this.httpStatusCode = httpStatusCode;
		this.body = body;
	}
	
	public ResponseBean(int httpStatusCode,Map<String, List<String>> responseHeader) {
		this.httpStatusCode = httpStatusCode;
		this.responseHeaders = responseHeader;
	}

	public ResponseBean(Map<String, List<String>> responseHeader, T body, int httpStatusCode) {
		this.responseHeaders = responseHeader;
		this.body = body;
		this.httpStatusCode = httpStatusCode;
	}
}
