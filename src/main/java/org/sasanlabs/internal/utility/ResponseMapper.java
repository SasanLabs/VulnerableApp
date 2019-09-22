package org.sasanlabs.internal.utility;

import java.util.List;
import java.util.Map;

import org.sasanlabs.service.bean.ResponseBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public class ResponseMapper {

	private static <T> void populateHeaderInResponseEntity(ResponseEntity<T> responseEntity,
			Map<String, List<String>> headers) {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			responseEntity.getHeaders().addAll(entry.getKey(), entry.getValue());
		}
	}

	public static ResponseEntity<String> buildResponseEntity(ResponseBean responseBean) {
		ResponseEntity<String> responseEntity = new ResponseEntity<String>(responseBean.getBody(),
				HttpStatus.valueOf(responseBean.getHttpStatusCode()));
		populateHeaderInResponseEntity(responseEntity, responseBean.getResponseHeaders());
		return responseEntity;
	}

}
