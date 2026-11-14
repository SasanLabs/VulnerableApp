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

    @Test
    void getMethodIfApplicable_keepsTheMethodForHttp() {
        assertThat(Protocol.HTTP.getMethodIfApplicable("POST")).isEqualTo("POST");
    }

    @Test
    void getMethodIfApplicable_dropsTheMethodForWs() {
        assertThat(Protocol.WS.getMethodIfApplicable("GET")).isNull();
    }

    @Test
    void getMethodIfApplicable_keepsANullMethodNull() {
        assertThat(Protocol.HTTP.getMethodIfApplicable(null)).isNull();
        assertThat(Protocol.WS.getMethodIfApplicable(null)).isNull();
    }
}
