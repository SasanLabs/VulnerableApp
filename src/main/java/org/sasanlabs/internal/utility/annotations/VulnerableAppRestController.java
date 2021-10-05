package org.sasanlabs.internal.utility.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This annotation marks the class as the vulnerable rest controller for VulnerableApp. Can be
 * visualized as {@link org.springframework.stereotype.Controller} Annotation
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RestController
@RequestMapping
public @interface VulnerableAppRestController {

    /**
     * Unique name (Endpoint Name)
     *
     * @return
     */
    @AliasFor(annotation = RequestMapping.class)
    String value();

    /**
     * This is used for describing about the vulnerability like say LFI is there than this helps in
     * understanding what does LFI means and how that can impact an application if present in that
     * application. it can also have links to resources.
     *
     * @return Localization key
     */
    String descriptionLabel();
}
