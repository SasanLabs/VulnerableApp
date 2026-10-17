package org.sasanlabs.analytics.mapping;

/**
 * The VulnerableApp challenge and level a finding belongs to. Either part is empty when it cannot
 * be resolved, e.g. a DAST alert on {@code /}.
 */
public record ChallengeRef(String challenge, String level) {

    public static final ChallengeRef NONE = new ChallengeRef("", "");
}
