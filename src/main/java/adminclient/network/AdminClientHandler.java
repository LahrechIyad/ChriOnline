package adminclient.network;

import shared.network.Request;
import shared.network.Response;
import shared.security.Signer;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

public class AdminClientHandler {
    private String serverAddress;
    private int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String sessionToken;

    public AdminClientHandler(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public void connect() throws Exception {
        socket = new Socket(serverAddress, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Response sendRequest(Request request) throws Exception {
        if (sessionToken != null) {
            request.setSessionToken(sessionToken);
        }
        out.writeObject(request);
        out.flush();
        return (Response) in.readObject();
    }

    public boolean authenticateAdmin(String email, PrivateKey privateKey) {
        try {
            // Step 1: Request Challenge
            System.out.println("[AdminClient] 1. Requesting challenge for " + email);
            Request req = new Request("ADMIN_AUTH_REQUEST", email);
            Response resp1 = sendRequest(req);

            if (!resp1.isSuccess()) {
                System.out.println("[AdminClient] Error: " + resp1.getMessage());
                return false;
            }

            String challenge = (String) resp1.getData();
            System.out.println("[AdminClient] 2. Received challenge: " + challenge);

            // Step 2: Sign Challenge
            System.out.println("[AdminClient] 3. Signing challenge with private key...");
            byte[] signature = Signer.sign(challenge, privateKey);

            // Step 3: Verify Challenge
            System.out.println("[AdminClient] 4. Sending signature to server...");
            Map<String, Object> verifyData = new HashMap<>();
            verifyData.put("email", email);
            verifyData.put("signature", signature);

            Request verifyReq = new Request("ADMIN_AUTH_VERIFY", verifyData);
            Response resp2 = sendRequest(verifyReq);

            if (resp2.isSuccess()) {
                Map<String, Object> data = (Map<String, Object>) resp2.getData();
                this.sessionToken = (String) data.get("token");
                System.out.println("[AdminClient] 5. Authentication SUCCESS! Access Granted.");
                return true;
            } else {
                System.out.println("[AdminClient] 5. Authentication FAILED. Access Denied: " + resp2.getMessage());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Dummy method for dashboard data
    public void getDashboardData() {
        if (sessionToken == null) {
            System.out.println("You are not authenticated.");
            return;
        }
        System.out.println("[AdminClient] Fetching sensitive dashboard data using session token: " + sessionToken);
        // Implement real requests here...
    }
}
