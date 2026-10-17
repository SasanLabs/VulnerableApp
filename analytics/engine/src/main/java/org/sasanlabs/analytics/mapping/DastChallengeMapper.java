package org.sasanlabs.analytics.mapping;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sasanlabs.analytics.model.Finding;

/**
 * Maps a DAST finding by its URL. The benchmark endpoint already reduces URLs to the
 * context-path-free form {@code /<Challenge>/<LEVEL_n>}, so the first segment is the challenge and
 * the second, when it is a level, is the level.
 */
public class DastChallengeMapper implements ChallengeMapper {

    private static final Pattern CHALLENGE_PATH =
            Pattern.compile("^/([^/?#;]+)(?:/(LEVEL_\\d+))?(?:[/?#;].*)?$");

    @Override
    public ChallengeRef map(Finding finding) {
        if (finding.url() == null) {
            return ChallengeRef.NONE;
        }
        Matcher m = CHALLENGE_PATH.matcher(finding.url().trim());
        if (!m.matches()) {
            return ChallengeRef.NONE;
        }
        String level = m.group(2) != null ? m.group(2) : "";
        return new ChallengeRef(m.group(1), level);
    }

    @Override
    public String location(Finding finding) {
        return finding.url() != null ? finding.url() : "";
    }
}
