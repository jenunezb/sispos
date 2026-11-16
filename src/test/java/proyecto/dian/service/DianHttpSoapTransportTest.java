package proyecto.dian.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DianHttpSoapTransportTest {
    @Test
    void blocksBeforeNetworkWhenTransmissionIsDisabled() {
        DianHttpSoapTransport transport = new DianHttpSoapTransport(false, 2, 2);
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> transport.exchange("https://127.0.0.1:1/dian", "action", "<soap/>".getBytes()));
        assertTrue(error.getMessage().contains("desactivada"));
    }

    @Test
    void rejectsNonHttpsDestinations() {
        DianHttpSoapTransport transport = new DianHttpSoapTransport(true, 2, 2);
        assertThrows(IllegalArgumentException.class,
                () -> transport.exchange("http://example.com", "action", "<soap/>".getBytes()));
    }
}
