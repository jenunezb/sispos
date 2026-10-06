package proyecto.dian.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class DianCryptoService {
    private static final String PREFIX = "v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String configuredKey;

    public DianCryptoService(@Value("${dian.security.master-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public String encrypt(String plaintext, String context) {
        if (plaintext == null) return null;
        return encode(encryptBytes(plaintext.getBytes(StandardCharsets.UTF_8), context));
    }

    public byte[] encryptBytes(byte[] plaintext, String context) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, iv, context);
            byte[] encrypted = cipher.doFinal(plaintext);
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return payload;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No fue posible proteger el secreto DIAN", e);
        }
    }

    public String decrypt(String ciphertext, String context) {
        if (ciphertext == null) return null;
        return new String(decryptBytes(decode(ciphertext), context), StandardCharsets.UTF_8);
    }

    public byte[] decryptBytes(byte[] payload, String context) {
        if (payload == null) return null;
        try {
            if (payload.length <= IV_LENGTH) throw new IllegalArgumentException("Secreto DIAN invalido");
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            return cipher(Cipher.DECRYPT_MODE, iv, context).doFinal(encrypted);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("No fue posible leer el secreto DIAN", e);
        }
    }

    private String encode(byte[] payload) {
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
    }

    private byte[] decode(String ciphertext) {
        if (!ciphertext.startsWith(PREFIX)) throw new IllegalArgumentException("Formato de secreto DIAN no soportado");
        return Base64.getUrlDecoder().decode(ciphertext.substring(PREFIX.length()));
    }

    private Cipher cipher(int mode, byte[] iv, String context) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private SecretKeySpec key() {
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new IllegalStateException("La variable DIAN_MASTER_KEY no esta configurada");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(configuredKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("DIAN_MASTER_KEY debe estar codificada en Base64", e);
        }
        if (decoded.length != 32) {
            throw new IllegalStateException("DIAN_MASTER_KEY debe contener exactamente 32 bytes");
        }
        return new SecretKeySpec(decoded, "AES");
    }
}
