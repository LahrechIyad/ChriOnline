package server.security;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

public class ServerKeyManager {
    private static final String KEYSTORE_PATH = "config/server-keystore.p12";
    private static final String KEY_ALIAS = "server";
    private static final String DEFAULT_PASSWORD = "changeit123";

    private final KeyPair keyPair;

    public ServerKeyManager() {
        this.keyPair = loadOrCreateKeyPair();
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    private KeyPair loadOrCreateKeyPair() {
        try {
            File configDir = new File("config");
            if (!configDir.exists() && !configDir.mkdirs()) {
                throw new IllegalStateException("Unable to create config directory.");
            }

            File keystoreFile = new File(KEYSTORE_PATH);
            char[] password = resolvePassword().toCharArray();

            if (!keystoreFile.exists()) {
                generateDemoKeystore(keystoreFile, password);
            }

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                keyStore.load(fis, password);
            }

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, password);
            PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load or generate server RSA keypair.", e);
        }
    }

    private void generateDemoKeystore(File keystoreFile, char[] password) throws Exception {
        String javaHome = System.getProperty("java.home");
        File keytool = new File(javaHome, "bin/keytool.exe");
        if (!keytool.exists()) {
            keytool = new File(javaHome, "bin/keytool");
        }
        ProcessBuilder builder = new ProcessBuilder(
                keytool.getAbsolutePath(),
                "-genkeypair",
                "-alias", KEY_ALIAS,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-storetype", "PKCS12",
                "-keystore", keystoreFile.getAbsolutePath(),
                "-storepass", new String(password),
                "-keypass", new String(password),
                "-dname", "CN=ChriOnline Server, OU=Security, O=ChriOnline, L=Casablanca, S=Casablanca, C=MA",
                "-validity", "3650"
        );
        Process process = builder.inheritIO().start();
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("keytool failed to generate server keystore.");
        }
    }

    private String resolvePassword() {
        String password = System.getenv("SERVER_KEYSTORE_PASSWORD");
        if (password == null || password.isBlank()) {
            try {
                password = Dotenv.configure().ignoreIfMissing().load().get("SERVER_KEYSTORE_PASSWORD");
            } catch (Exception ignored) {
            }
        }
        return password == null || password.isBlank() ? DEFAULT_PASSWORD : password;
    }
}
