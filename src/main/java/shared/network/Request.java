package shared.network;

import java.io.Serializable;

/**
 * Represents a request sent from the client to the server.
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private Object data;
    
    // Security fields (TP2 & TP5)
    private long timestamp;
    private String nonce;
    private String sessionToken;

    public Request(String type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() { return type; }
    public Object getData() { return data; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
    
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}
