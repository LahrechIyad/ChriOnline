package shared.security;

import javax.crypto.SecretKey;

public class SecureSession {
    private SecretKey aesKey;

    public SecureSession() {
    }

    public SecureSession(SecretKey aesKey) {
        this.aesKey = aesKey;
    }

    public SecretKey getAesKey() {
        return aesKey;
    }

    public void setAesKey(SecretKey aesKey) {
        this.aesKey = aesKey;
    }

    public boolean isEstablished() {
        return aesKey != null;
    }
}
