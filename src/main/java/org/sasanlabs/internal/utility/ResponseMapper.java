package org.sasanlabs.internal.utility;

import java.util.List;
import java.util.Map;

import org.sasanlabs.service.bean.ResponseBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public class ResponseMapper {

	private static HttpHeaders populateHeaderInResponseEntity(Map<String, List<String>> headers) {
		HttpHeaders httpHeaders = new HttpHeaders();
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			httpHeaders.addAll(entry.getKey(), entry.getValue());
		}
		return httpHeaders;
	}

	public static <T> ResponseEntity<T> buildResponseEntity(ResponseBean<T> responseBean) {
		HttpHeaders httpHeaders = populateHeaderInResponseEntity(responseBean.getResponseHeaders());
		ResponseEntity<T> responseEntity = new ResponseEntity<T>(responseBean.getBody(), httpHeaders,
				HttpStatus.valueOf(responseBean.getHttpStatusCode()));
		return responseEntity;
	}

}
