package proyecto.dian.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class DianCryptoServiceTest {
    private static DianCryptoService service() {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        return new DianCryptoService(Base64.getEncoder().encodeToString(key));
    }

    @Test
    void encryptsAndDecryptsWithoutExposingPlaintext() {
        DianCryptoService crypto = service();
        String encrypted = crypto.encrypt("pin-super-secreto", "dian:900:HABILITACION:pin");

        assertTrue(encrypted.startsWith("v1:"));
        assertFalse(encrypted.contains("pin-super-secreto"));
        assertEquals("pin-super-secreto", crypto.decrypt(encrypted, "dian:900:HABILITACION:pin"));
    }

    @Test
    void usesRandomIvAndTenantBoundAuthentication() {
        DianCryptoService crypto = service();
        String first = crypto.encrypt("mismo-secreto", "dian:900:HABILITACION:pin");
        String second = crypto.encrypt("mismo-secreto", "dian:900:HABILITACION:pin");

        assertNotEquals(first, second);
        assertThrows(IllegalStateException.class,
                () -> crypto.decrypt(first, "dian:901:HABILITACION:pin"));
    }

    @Test
    void rejectsMissingOrIncorrectLengthKeys() {
        assertThrows(IllegalStateException.class,
                () -> new DianCryptoService("").encrypt("secret", "context"));
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalStateException.class,
                () -> new DianCryptoService(shortKey).encrypt("secret", "context"));
    }
}
