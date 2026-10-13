package org.sasanlabs.analytics.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sasanlabs.analytics.model.Finding;

class SastChallengeMapperTest {

    private final SastChallengeMapper mapper =
            new SastChallengeMapper(Map.of("JWTValidator", "JWTVulnerability"));

    @Test
    void challenge_isTheClassName() {
        assertThat(
                        mapper.map(
                                sast(
                                        "src/main/java/org/sasanlabs/service/vulnerability/jwt/JWTVulnerability.java",
                                        42)))
                .isEqualTo(new ChallengeRef("JWTVulnerability", ""));
    }

    @Test
    void mappedClass_takesTheConfiguredChallenge() {
        assertThat(mapper.map(sast("src/main/java/org/sasanlabs/jwt/impl/JWTValidator.java", 9)))
                .isEqualTo(new ChallengeRef("JWTVulnerability", ""));
    }

    @Test
    void windowsSeparators_areHandled() {
        assertThat(mapper.map(sast("src\\main\\java\\Foo.java", 1)))
                .isEqualTo(new ChallengeRef("Foo", ""));
    }

    @Test
    void missingPath_mapsToNothing() {
        assertThat(mapper.map(sast(null, 1))).isEqualTo(ChallengeRef.NONE);
        assertThat(mapper.map(sast("  ", 1))).isEqualTo(ChallengeRef.NONE);
    }

    @Test
    void location_isPathAndLine() {
        assertThat(mapper.location(sast("src/Foo.java", 7))).isEqualTo("src/Foo.java:7");
        assertThat(mapper.location(sast("src/Foo.java", null))).isEqualTo("src/Foo.java");
    }

    private static Finding sast(String path, Integer line) {
        return new Finding(null, null, path, line, null, null, null);
    }
}
