package shared.security;

import java.security.PrivateKey;
import java.security.Signature;

public class Signer {
    public static byte[] sign(String challenge, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(challenge.getBytes());
        return signature.sign();
    }
}
