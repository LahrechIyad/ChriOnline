package security;

import java.security.KeyPair;
import java.security.PublicKey;

import javax.crypto.SecretKey;

public class SecureSession {

    public static void simulate() throws Exception {

        System.out.println("=== Simulation HTTPS - ChriOnline ===\n");

        // ÉTAPE 1 : Serveur génère sa paire de clés RSA
        KeyPair serverKeyPair = RSAUtil.generateKeyPair();
        System.out.println("✔ 1. Serveur : paire de clés RSA générée");

        // ÉTAPE 2 : Serveur envoie sa clé publique au client
        PublicKey serverPublicKey = serverKeyPair.getPublic();
        System.out.println("✔ 2. Serveur : clé publique envoyée au client");

        // ÉTAPE 3 : Client génère une clé AES
        SecretKey aesKey = AESUtil.generateKey();
        System.out.println("✔ 3. Client : clé AES générée");

        // ÉTAPE 4 : Client chiffre la clé AES avec la clé publique RSA
        byte[] encryptedAESKey = RSAUtil.encryptAESKey(aesKey, serverPublicKey);
        System.out.println("✔ 4. Client : clé AES chiffrée avec RSA");

        // ÉTAPE 5 : Serveur déchiffre la clé AES avec sa clé privée
        SecretKey recoveredKey = RSAUtil.decryptAESKey(
                encryptedAESKey,
                serverKeyPair.getPrivate()
        );
        System.out.println("✔ 5. Serveur : clé AES déchiffrée avec succès");

        // ÉTAPE 6 : Communication sécurisée avec AES
        String message = "Commande : 3x Produit A - Client ChriOnline";
        String encrypted = AESUtil.encrypt(message, recoveredKey);
        String decrypted = AESUtil.decrypt(encrypted, recoveredKey);

        System.out.println("\n✔ 6. Communication sécurisée AES :");
        System.out.println("   Message original  : " + message);
        System.out.println("   Message chiffré   : " + encrypted);
        System.out.println("   Message déchiffré : " + decrypted);

        System.out.println("\n=== Handshake terminé avec succès ===");
    }
}