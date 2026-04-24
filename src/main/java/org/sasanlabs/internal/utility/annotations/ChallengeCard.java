package org.sasanlabs.internal.utility.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define learning challenges for vulnerabilities. * @author Aryan
 * mr.aryankaushal@gmail.com
 */
@Repeatable(ChallengeCard.ChallengeCards.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface ChallengeCard {

    String challengeText();

    Hint[] hints() default {};

    Payload payload();

    /** Container annotation for making {@link ChallengeCard} repeatable. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(value = ElementType.METHOD)
    public @interface ChallengeCards {
        ChallengeCard[] value();
    }

    /** Defines a hint for the challenge. */
    public @interface Hint {
        int order();

        String text();
    }

    /** Defines the exploit payload for the challenge. */
    public @interface Payload {
        String description();

        String value();
    }
}
