package org.sasanlabs.internal.utility.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sasanlabs.vulnerability.types.VulnerabilityType;

/**
 * Purpose of this file is just to help user to know various payloads and ways to bypass security
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@Repeatable(AttackVector.AttackVectors.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface AttackVector {

    /**
     * Major usage is when we can do a Combined Attack.
     *
     * @return Vulnerability Types
     */
    VulnerabilityType[] vulnerabilityExposed();

    /**
     * This Key's value will be picked up from attackvectors resource folder. Please make sure Key
     * mentioned in AttackVector annotation is present in the vulnerability's payload properties
     * file.
     *
     * @return Key that Identifies Curl request
     */
    String payload() default "NOT_APPLICABLE";

    /**
     * Should be a Label Key
     *
     * @return description of the AttackVector.
     */
    String description();

    /**
     * Where the untrusted value that triggers the vulnerability is found, e.g. COOKIE, URL,
     * QUERY_PARAMETER or HEADER. Should be a Label Key. Use "NOT_APPLICABLE" when it does not
     * apply.
     *
     * @return source/location of the vulnerable value.
     */
    String source() default "NOT_APPLICABLE";

    /**
     * Should be a Label Key providing the remediation/fix for the vulnerability. Use
     * "NOT_APPLICABLE" when it does not apply.
     *
     * @return solution/remediation of the AttackVector.
     */
    String solution() default "NOT_APPLICABLE";

    /**
     * Should be a Label Key providing references/links to learn more about the vulnerability. Use
     * "NOT_APPLICABLE" when it does not apply.
     *
     * @return references of the AttackVector.
     */
    String reference() default "NOT_APPLICABLE";

    @Retention(RetentionPolicy.RUNTIME)
    @Target(value = ElementType.METHOD)
    public @interface AttackVectors {
        AttackVector[] value();
    }
}
