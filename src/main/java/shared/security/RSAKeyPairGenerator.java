package shared.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class RSAKeyPairGenerator {
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
