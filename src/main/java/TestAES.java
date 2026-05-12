package security;

import javax.crypto.SecretKey;  // ← THIS WAS MISSING

public class TestAES {

    public static void main(String[] args) throws Exception {

        // ===== TEST 1 : AES seul =====
        System.out.println("=== Test AES ===\n");

        SecretKey key = AESUtil.generateKey();

        String encrypted = AESUtil.encrypt("Bonjour ChriOnline", key);
        String decrypted = AESUtil.decrypt(encrypted, key);

        System.out.println("Chiffré   : " + encrypted);
        System.out.println("Déchiffré : " + decrypted);

        System.out.println();

        // ===== TEST 2 : Simulation HTTPS =====
        SecureSession.simulate();
    }
}