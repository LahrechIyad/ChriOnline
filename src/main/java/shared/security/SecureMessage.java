package shared.security;

import java.io.Serializable;

public class SecureMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] iv;
    private final byte[] ciphertext;

    public SecureMessage(byte[] iv, byte[] ciphertext) {
        this.iv = iv;
        this.ciphertext = ciphertext;
    }

    public byte[] getIv() {
        return iv;
    }

    public byte[] getCiphertext() {
        return ciphertext;
    }
}
