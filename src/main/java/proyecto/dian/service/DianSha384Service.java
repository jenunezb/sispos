package proyecto.dian.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
final class DianSha384Service {

    String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-384")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-384 no está disponible", exception);
        }
    }
}
