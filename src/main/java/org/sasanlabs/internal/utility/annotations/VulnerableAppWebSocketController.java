package org.sasanlabs.internal.utility.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * This annotation marks the class as a vulnerable WebSocket controller for VulnerableApp, the
 * WebSocket counterpart of {@link VulnerableAppRestController}. Each method annotated with {@link
 * VulnerableAppWebSocketMapping} is registered as the WebSocket endpoint of one level.
 *
 * @author KSASAN preetkaran20@gmail.com
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface VulnerableAppWebSocketController {
    /**
     * Unique name (Endpoint Name), it is the first segment of the WebSocket path.
     *
     * @return name
     */
    @AliasFor(annotation = Component.class)
    String value();

    /**
     * This is used for describing about the vulnerability, same as {@link
     * VulnerableAppRestController#descriptionLabel()}.
     *
     * @return Localization key
     */
    String descriptionLabel();
}
