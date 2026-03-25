package shared.network;

import java.io.Serializable;

/**
 * Represents a request sent from the client to the server.
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private Object data;

    public Request(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }
    public Object getData() { return data; }
}
