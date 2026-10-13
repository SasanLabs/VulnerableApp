package org.sasanlabs.analytics.mapping;

import java.util.Map;
import org.sasanlabs.analytics.model.Finding;

/** Resolves a benchmark finding to the VulnerableApp challenge it belongs to. */
public interface ChallengeMapper {

    ChallengeRef map(Finding finding);

    /** Text identifying where the finding is: a URL path for DAST, {@code path:line} for SAST. */
    String location(Finding finding);

    static ChallengeMapper forScanType(boolean sast, Map<String, String> sastClassToChallenge) {
        return sast ? new SastChallengeMapper(sastClassToChallenge) : new DastChallengeMapper();
    }
}
