package server.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordHashService {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String PREFIX = "pbkdf2_sha256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHashService() {
    }

    public static String hash(String password) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            SECURE_RANDOM.nextBytes(salt);
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
            return PREFIX + "$" + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash password.", e);
        }
    }

    public static boolean verify(String password, String storedPassword) {
        if (password == null || storedPassword == null || !isHash(storedPassword)) {
            return false;
        }

        try {
            String[] parts = storedPassword.split("\\$");
            if (parts.length != 4 || !PREFIX.equals(parts[0])) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] candidateHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length * 8);

            return MessageDigest.isEqual(expectedHash, candidateHash);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isHash(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(PREFIX + "$");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }
}
