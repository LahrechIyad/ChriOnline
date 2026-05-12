package client.network;

import shared.network.Request;
import shared.network.Response;
import shared.security.AESUtil;
import shared.security.RSAUtil;
import shared.security.SecureMessage;
import shared.security.SecureSession;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import javax.crypto.SecretKey;

/**
 * Handles TCP communication with the server from the client side.
 */
public class ClientTCP {
    private String serverAddress;
    private int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String sessionToken;
    private final SecureSession secureSession = new SecureSession();

    public ClientTCP(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    /**
     * Connects to the server.
     */
    public boolean connect() {
        try {
            socket = new Socket(serverAddress, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            performHandshake();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Secure handshake failed: " + e.getMessage());
            return false;
        }
    }

    public void setSessionToken(String token) {
        this.sessionToken = token;
    }

    public String getSessionToken() {
        return this.sessionToken;
    }

    /**
     * Sends a request to the server and waits for the response.
     * @param request Request to send
     * @return Response from server
     */
    public Response sendRequest(Request request) {
        try {
            // TP2 Client side: ensure timestamps and nonces are populated
            request.setTimestamp(System.currentTimeMillis());
            request.setNonce(java.util.UUID.randomUUID().toString());
            
            // TP5 Client side: inject session token if we have one
            if (this.sessionToken != null) {
                request.setSessionToken(this.sessionToken);
            }

            SecureMessage secureMessage = AESUtil.encryptObject(request, secureSession.getAesKey());
            out.writeObject(secureMessage);
            out.flush();
            SecureMessage responseMessage = (SecureMessage) in.readObject();
            return (Response) AESUtil.decryptObject(responseMessage, secureSession.getAesKey());
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            System.err.println("Communication error: " + e.getMessage());
            return new Response(false, "Communication error: " + e.getMessage(), null);
        } catch (Exception e) {
            System.err.println("Communication error: " + e.getMessage());
            return new Response(false, "Communication error: " + e.getMessage(), null);
        }
    }

    private void performHandshake() throws Exception {
        byte[] serverPublicKeyBytes = (byte[]) in.readObject();
        System.out.println("Client: Server public key received");
        PublicKey serverPublicKey = RSAUtil.decodePublicKey(serverPublicKeyBytes);

        SecretKey aesKey = AESUtil.generateKey();
        secureSession.setAesKey(aesKey);
        System.out.println("Client: AES session key generated");

        byte[] encryptedAesKey = RSAUtil.encryptAesKey(aesKey, serverPublicKey);
        out.writeObject(encryptedAesKey);
        out.flush();
        System.out.println("Client: AES key encrypted with RSA and sent");
        System.out.println("Client: Secure AES communication established");
    }

    /**
     * Disconnects from the server.
     */
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
