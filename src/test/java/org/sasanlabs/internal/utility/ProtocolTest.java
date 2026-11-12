package org.sasanlabs.internal.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProtocolTest {

    @Test
    void orDefault_returnsHttpForNull() {
        assertThat(Protocol.orDefault(null)).isEqualTo(Protocol.HTTP);
    }

    @Test
    void orDefault_keepsAnExplicitProtocol() {
        assertThat(Protocol.orDefault(Protocol.HTTP)).isEqualTo(Protocol.HTTP);
        assertThat(Protocol.orDefault(Protocol.WS)).isEqualTo(Protocol.WS);
    }
}
