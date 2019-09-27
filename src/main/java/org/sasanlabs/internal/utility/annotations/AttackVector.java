package org.sasanlabs.internal.utility.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.sasanlabs.vulnerability.types.VulnerabilityType;

/**
 * @author KSASAN preetkaran20@gmail.com
 * 
 * 
 *         Purpose of this file is just to help user to know various payloads
 *         and ways to bypass security
 */
@Repeatable(AttackVector.AttackVectors.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface AttackVector {

	/**
	 * Major usage is when we can do a Combined Attack.
	 * @return
	 */
	VulnerabilityType[] vulnerabilityExposed();

	/**
	 * @return Key that Identifies URL payload
	 */
	String urlPayload();

	/**
	 * @return Key that Identifies Curl request
	 */
	String curlPayload();

	/**
	 * Should be a Label Key
	 * @return
	 */
	String description();

	@Retention(RetentionPolicy.RUNTIME)
	@Target(value = {})
	public @interface AttackVectors {
		AttackVector[] value();
	}
}
