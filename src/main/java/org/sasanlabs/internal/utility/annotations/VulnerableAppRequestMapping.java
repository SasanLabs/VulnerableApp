package org.sasanlabs.internal.utility.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sasanlabs.internal.utility.Variant;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/** @author KSASAN preetkaran20@gmail.com */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@RequestMapping
public @interface VulnerableAppRequestMapping {

    /**
     * Specify the level of the vulnerability. Url end point is exposed for each level.
     *
     * @return level
     */
    @AliasFor(annotation = RequestMapping.class)
    String value();

    /**
     * Specify whether the implementation can be considered secure, as in, non-exploitable.
     *
     * @return variant
     */
    Variant variant() default Variant.UNSECURE;

    /**
     * Template name is used to construct the url for static resources like js/css/html. UI will
     * look for path to static templates like static/templates/JWTVulnerabilities/{htmlTemplate}.{js
     * or css or html} to construct final Html.
     *
     * @return template name
     */
    String htmlTemplate() default "";

    /**
     * This information is not useful for now because we are for now only handling Get requests but
     * going further we might use this information and hence this is very useful for scanner.
     *
     * @return {@code RequestMethod} for the Level
     */
    @AliasFor(attribute = "method", annotation = RequestMapping.class)
    RequestMethod requestMethod() default RequestMethod.GET;
}
