package security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class RSAUtil {

    // Générer paire de clés RSA (Serveur)
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator gen = 
                KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    // Client chiffre la clé AES avec clé publique RSA
    public static byte[] encryptAESKey(SecretKey aesKey, 
                                        PublicKey rsaPublicKey) 
            throws Exception {
        Cipher cipher = Cipher.getInstance(
                "RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        return cipher.doFinal(aesKey.getEncoded());
    }

    // Serveur déchiffre la clé AES avec clé privée RSA
    public static SecretKey decryptAESKey(byte[] encryptedKey, 
                                           PrivateKey rsaPrivateKey) 
            throws Exception {
        Cipher cipher = Cipher.getInstance(
                "RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] decoded = cipher.doFinal(encryptedKey);
        return new SecretKeySpec(decoded, "AES");
    }
}