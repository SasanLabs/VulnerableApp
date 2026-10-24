package org.sasanlabs.analytics.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sasanlabs.analytics.model.Finding;

class DastChallengeMapperTest {

    private final DastChallengeMapper mapper = new DastChallengeMapper();

    @Test
    void challengeAndLevel_areReadFromThePath() {
        assertThat(mapper.map(dast("/JWTVulnerability/LEVEL_12")))
                .isEqualTo(new ChallengeRef("JWTVulnerability", "LEVEL_12"));
    }

    @Test
    void subPathsAndQueries_afterTheLevel_areIgnored() {
        assertThat(mapper.map(dast("/PathTraversal/LEVEL_3/files?name=x")))
                .isEqualTo(new ChallengeRef("PathTraversal", "LEVEL_3"));
        assertThat(mapper.map(dast("/BlindSQLInjectionVulnerability/LEVEL_1;(SELECT 1)")))
                .isEqualTo(new ChallengeRef("BlindSQLInjectionVulnerability", "LEVEL_1"));
    }

    @Test
    void pathWithoutLevel_mapsToChallengeOnly() {
        assertThat(mapper.map(dast("/CachePoisoning")))
                .isEqualTo(new ChallengeRef("CachePoisoning", ""));
        assertThat(mapper.map(dast("/CachePoisoning/static/app.js")))
                .isEqualTo(new ChallengeRef("CachePoisoning", ""));
    }

    @Test
    void rootOrMissingUrl_mapsToNothing() {
        assertThat(mapper.map(dast("/"))).isEqualTo(ChallengeRef.NONE);
        assertThat(mapper.map(dast(null))).isEqualTo(ChallengeRef.NONE);
    }

    @Test
    void location_isTheUrl() {
        assertThat(mapper.location(dast("/XSS/LEVEL_1"))).isEqualTo("/XSS/LEVEL_1");
        assertThat(mapper.location(dast(null))).isEmpty();
    }

    private static Finding dast(String url) {
        return new Finding(url, null, null, null, null, null, null);
    }
}
