package security;

import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class AESUtil {

    // Générer une clé AES
    public static SecretKey generateKey() throws Exception {

        KeyGenerator keyGen =
                KeyGenerator.getInstance("AES");

        keyGen.init(256);

        return keyGen.generateKey();
    }

    // Chiffrement
    public static String encrypt(String message,
                                 SecretKey key)
            throws Exception {

        Cipher cipher =
                Cipher.getInstance("AES/GCM/NoPadding");

        byte[] iv = new byte[12];

        SecureRandom random =
                new SecureRandom();

        random.nextBytes(iv);

        GCMParameterSpec spec =
                new GCMParameterSpec(128, iv);

        cipher.init(
                Cipher.ENCRYPT_MODE,
                key,
                spec
        );

        byte[] encrypted =
                cipher.doFinal(message.getBytes());

        byte[] encryptedIVAndText =
                new byte[iv.length + encrypted.length];

        System.arraycopy(
                iv,
                0,
                encryptedIVAndText,
                0,
                iv.length
        );

        System.arraycopy(
                encrypted,
                0,
                encryptedIVAndText,
                iv.length,
                encrypted.length
        );

        return Base64.getEncoder()
                .encodeToString(encryptedIVAndText);
    }

    // Déchiffrement
    public static String decrypt(String encryptedMessage,
                                 SecretKey key)
            throws Exception {

        byte[] decoded =
                Base64.getDecoder()
                        .decode(encryptedMessage);

        byte[] iv = new byte[12];

        System.arraycopy(decoded,
                0,
                iv,
                0,
                iv.length);

        byte[] encryptedBytes =
                new byte[decoded.length - 12];

        System.arraycopy(decoded,
                12,
                encryptedBytes,
                0,
                encryptedBytes.length);

        Cipher cipher =
                Cipher.getInstance("AES/GCM/NoPadding");

        GCMParameterSpec spec =
                new GCMParameterSpec(128, iv);

        cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                spec
        );

        byte[] decrypted =
                cipher.doFinal(encryptedBytes);

        return new String(decrypted);
    }
}