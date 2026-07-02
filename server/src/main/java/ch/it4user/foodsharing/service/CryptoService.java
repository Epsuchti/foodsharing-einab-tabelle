package ch.it4user.foodsharing.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();
    private final AppProperties appProperties;

    public CryptoService(AppProperties appProperties) { this.appProperties = appProperties; }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES]; secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception ex) { throw new IllegalStateException("Failed to encrypt Foodsharing API secret", ex); }
    }

    public String decrypt(String cipherText) {
        if (cipherText == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(cipherText);
            ByteBuffer buffer = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_LENGTH_BYTES]; buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()]; buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) { throw new IllegalStateException("Failed to decrypt Foodsharing API secret", ex); }
    }

    private SecretKeySpec key() {
        String configured = appProperties.getFoodsharing().getTokenEncryptionKey();
        byte[] bytes = configured == null || configured.isBlank()
                ? "local-dev-foodsharing-token-key!!".getBytes(StandardCharsets.UTF_8)
                : Base64.getDecoder().decode(configured);
        if (bytes.length != 32) throw new IllegalStateException("app.foodsharing.token-encryption-key must be a base64 encoded 32 byte key");
        return new SecretKeySpec(bytes, "AES");
    }
}
