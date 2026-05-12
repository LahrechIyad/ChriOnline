package server.security;

import io.github.cdimascio.dotenv.Dotenv;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import shared.security.AESUtil;
import shared.security.SecureMessage;

public class DatabaseCryptoService {
    private static final String ENV_NAME = "DB_AES_MASTER_KEY_BASE64";
    private final SecretKey masterKey;

    public DatabaseCryptoService() {
        this.masterKey = loadMasterKey();
    }

    public String encryptToBase64(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            return plaintext;
        }
        try {
            SecureMessage secureMessage = AESUtil.encryptObject(plaintext, masterKey);
            ByteBuffer buffer = ByteBuffer.allocate(4 + secureMessage.getIv().length + secureMessage.getCiphertext().length);
            buffer.putInt(secureMessage.getIv().length);
            buffer.put(secureMessage.getIv());
            buffer.put(secureMessage.getCiphertext());
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt database field.", e);
        }
    }

    public String decryptFromBase64(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            return encryptedBase64;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encryptedBase64);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            int ivLength = buffer.getInt();
            byte[] iv = new byte[ivLength];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            return (String) AESUtil.decryptObject(new SecureMessage(iv, ciphertext), masterKey);
        } catch (IllegalArgumentException e) {
            return encryptedBase64;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decrypt database field.", e);
        }
    }

    private SecretKey loadMasterKey() {
        String keyBase64 = System.getenv(ENV_NAME);
        if (keyBase64 == null || keyBase64.isBlank()) {
            try {
                keyBase64 = Dotenv.configure().ignoreIfMissing().load().get(ENV_NAME);
            } catch (Exception ignored) {
            }
        }
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalStateException(
                    "Missing " + ENV_NAME + ". Generate one with: " +
                    "powershell -Command \"[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))\""
            );
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyBase64.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(ENV_NAME + " must be valid Base64.", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(ENV_NAME + " must decode to 32 bytes for AES-256.");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}
