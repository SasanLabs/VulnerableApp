package org.sasanlabs.analytics.mapping;

import java.util.Map;
import org.sasanlabs.analytics.model.Finding;

/**
 * Maps a SAST finding by its file. The challenge is the class name, e.g. {@code
 * .../jwt/JWTVulnerability.java} maps to {@code JWTVulnerability}, unless {@code
 * analytics/challenge-mapping.json} names a different challenge for that class (a helper such as
 * {@code JWTValidator}, or a controller whose URL name differs from its class name). The level is
 * left empty: the ground truth records a line, not a level.
 */
public class SastChallengeMapper implements ChallengeMapper {

    private static final String JAVA_SUFFIX = ".java";

    private final Map<String, String> classToChallenge;

    public SastChallengeMapper(Map<String, String> classToChallenge) {
        this.classToChallenge = classToChallenge;
    }

    @Override
    public ChallengeRef map(Finding finding) {
        if (finding.filePath() == null || finding.filePath().isBlank()) {
            return ChallengeRef.NONE;
        }
        String path = finding.filePath().trim().replace('\\', '/');
        String className = path.substring(path.lastIndexOf('/') + 1);
        if (className.endsWith(JAVA_SUFFIX)) {
            className = className.substring(0, className.length() - JAVA_SUFFIX.length());
        }
        return new ChallengeRef(classToChallenge.getOrDefault(className, className), "");
    }

    @Override
    public String location(Finding finding) {
        if (finding.filePath() == null) {
            return "";
        }
        return finding.line() != null
                ? finding.filePath() + ":" + finding.line()
                : finding.filePath();
    }
}
