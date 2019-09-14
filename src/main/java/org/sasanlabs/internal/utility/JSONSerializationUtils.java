package org.sasanlabs.internal.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author KSASAN preetkaran20@gmail.com
 *
 */
public class JSONSerializationUtils {

	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	public static <T> String serialize(T object) throws JsonProcessingException {
		return MAPPER.writeValueAsString(object);
	}
	
	public static <T> String serializeWithPrettyPrintJSON(T object) throws JsonProcessingException {
		return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
	}
	
}
