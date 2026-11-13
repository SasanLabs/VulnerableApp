package org.sasanlabs.internal.utility.annotations;

import java.lang.annotation.*;

/**
 * Annotation to define learning challenges for vulnerabilities. * @author Aryan
 * mr.aryankaushal@gmail.com
 */
@Documented
@Repeatable(ChallengeCard.ChallengeCards.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface ChallengeCard {

    String challengeText();

    Hint[] hints() default {};

    Payload payload();

    /** Container annotation for making {@link ChallengeCard} repeatable. */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(value = ElementType.METHOD)
    @interface ChallengeCards {
        ChallengeCard[] value();
    }

    /** Defines a hint for the challenge. */
    @Documented
    @interface Hint {
        int order();

        String text();
    }

    /** Defines the exploit payload for the challenge. */
    @Documented
    @interface Payload {
        String description();

        String value();
    }
}
