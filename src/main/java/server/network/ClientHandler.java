package server.network;

import shared.network.Request;
import shared.network.Response;
import shared.model.User;
import server.service.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles communication with a single connected client.
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    // Services
    private UserService userService;
    private ProduitService produitService;
    private PanierService panierService;
    private CommandeService commandeService;
    private PaiementService paiementService;
    private AdminAuthService adminAuthService;

    // TP5 - Session State
    // We bind tokens to Users globally, so if TCP connection drops, token is still valid.
    private static Map<String, User> activeSessions = new ConcurrentHashMap<>();
    
    // TP2 - Replay Attack Cache
    private static Set<String> usedNonces = ConcurrentHashMap.newKeySet();
    private static final long MAX_TIMESTAMP_AGE_MS = 5 * 60 * 1000; // 5 minutes

    public ClientHandler(Socket clientSocket, UserService userService, ProduitService produitService,
                         PanierService panierService, CommandeService commandeService, PaiementService paiementService, AdminAuthService adminAuthService) {
        this.clientSocket = clientSocket;
        this.userService = userService;
        this.produitService = produitService;
        this.panierService = panierService;
        this.commandeService = commandeService;
        this.paiementService = paiementService;
        this.adminAuthService = adminAuthService;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                Request request = (Request) in.readObject();
                if (request == null) break;

                // TP2 - Replay Attack Check
                if (!isValidRequest(request)) {
                    sendResponse(new Response(false, "Security Check Failed: Invalid or replayed request.", null));
                    continue;
                }

                Response response = processRequest(request);
                sendResponse(response);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected or error: " + e.getMessage());
        } finally {
            closeConnections();
        }
    }
    
    private void sendResponse(Response response) throws IOException {
        out.reset(); // Prevent ObjectOutputStream from caching stale objects
        out.writeObject(response);
        out.flush();
    }

    private boolean isValidRequest(Request request) {
        // Validation against replay attack
        long currentTimestamp = System.currentTimeMillis();
        long reqTimestamp = request.getTimestamp();
        
        if (Math.abs(currentTimestamp - reqTimestamp) > MAX_TIMESTAMP_AGE_MS) {
            return false; // Request too old or from the future
        }
        
        if (request.getNonce() != null) {
            if (!usedNonces.add(request.getNonce())) {
                return false; // Nonce already used (Replay attack)
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Response processRequest(Request request) {
        String type = request.getType();
        Object data = request.getData();
        
        // Retrieve user from session token if provided (TP5/TP6)
        User loggedInUser = null;
        if (request.getSessionToken() != null) {
            loggedInUser = activeSessions.get(request.getSessionToken());
        }

        switch (type) {
            case "REGISTER":
                return userService.registerUser((User) data);
                
            case "VERIFY_EMAIL":
                if (data instanceof Map) {
                    Map<String, String> verifData = (Map<String, String>) data;
                    return userService.verifyAccount(verifData.get("email"), verifData.get("code"));
                }
                return new Response(false, "Invalid verification data format.", null);

            case "LOGIN":
                if (data instanceof Map) {
                    Map<String, String> credentials = (Map<String, String>) data;
                    Response loginResp = userService.login(credentials.get("email"), credentials.get("password"));
                    
                    // TP5 - Secure Random Session
                    if (loginResp.isSuccess()) {
                        User user = (User) loginResp.getData();
                        String sessionToken = UUID.randomUUID().toString();
                        activeSessions.put(sessionToken, user);
                        // Package both token and user back to client
                        Map<String, Object> respData = new ConcurrentHashMap<>();
                        respData.put("user", user);
                        respData.put("token", sessionToken);
                        return new Response(true, loginResp.getMessage(), respData);
                    }
                    return loginResp;
                }
                return new Response(false, "Invalid login data format.", null);

            case "ADMIN_AUTH_REQUEST":
                if (data instanceof String) {
                    String adminEmail = (String) data;
                    String challenge = adminAuthService.generateChallenge(adminEmail);
                    if (challenge != null) {
                        return new Response(true, "Challenge generated", challenge);
                    }
                    return new Response(false, "Failed to generate challenge. Invalid admin email or non-existent user.", null);
                }
                return new Response(false, "Invalid data format for admin auth request.", null);

            case "ADMIN_AUTH_VERIFY":
                if (data instanceof Map) {
                    Map<String, Object> verifyData = (Map<String, Object>) data;
                    String adminEmail = (String) verifyData.get("email");
                    byte[] signature = (byte[]) verifyData.get("signature");
                    
                    User adminUser = adminAuthService.verifyChallenge(adminEmail, signature);
                    if (adminUser != null) {
                        String sessionToken = UUID.randomUUID().toString();
                        activeSessions.put(sessionToken, adminUser);
                        Map<String, Object> respData = new ConcurrentHashMap<>();
                        respData.put("user", adminUser);
                        respData.put("token", sessionToken);
                        return new Response(true, "Admin authenticated successfully.", respData);
                    }
                    return new Response(false, "Admin verification failed. Invalid signature or expired challenge.", null);
                }
                return new Response(false, "Invalid format for admin verification.", null);

            case "LOGOUT":
                if (request.getSessionToken() != null) {
                    activeSessions.remove(request.getSessionToken());
                }
                return new Response(true, "Logged out successfully.", null);

            case "GET_PRODUCTS":
                return produitService.getAllProducts();

            case "GET_PRODUCT":
                return produitService.getProductDetails((Integer) data);

            // Protected Routes below
            case "ADD_TO_CART":
                if (loggedInUser == null) return new Response(false, "Unauthenticated session.", null);
                if (data instanceof Map) {
                    Map<String, Integer> cartData = (Map<String, Integer>) data;
                    return panierService.addProductToCart(loggedInUser.getId(), cartData.get("productId"), cartData.get("quantity"));
                }
                return new Response(false, "Invalid cart data.", null);

            case "VIEW_CART":
                if (loggedInUser == null) return new Response(false, "Unauthenticated session.", null);
                return panierService.getCart(loggedInUser.getId());

            case "REMOVE_FROM_CART":
                if (loggedInUser == null) return new Response(false, "Unauthenticated session.", null);
                return panierService.removeProductFromCart(loggedInUser.getId(), (Integer) data);


            case "CREATE_ORDER":
                if (loggedInUser == null) return new Response(false, "Unauthenticated session.", null);
                return commandeService.createOrder(loggedInUser.getId());

            case "PROCESS_PAYMENT":
                if (loggedInUser == null) return new Response(false, "Unauthenticated session.", null);
                if (data instanceof Map) {
                    Map<String, Object> paymentData = (Map<String, Object>) data;
                    return paiementService.processPayment(
                        (Integer) paymentData.get("orderId"),
                        (Double) paymentData.get("amount"),
                        (String) paymentData.get("method")
                    );
                }
                return new Response(false, "Invalid payment data.", null);

            case "GET_MY_ORDERS":
                if (loggedInUser == null) return new Response(false, "Unauthenticated session.", null);
                return commandeService.getOrdersByUser(loggedInUser.getId());

            default:
                return new Response(false, "Unknown request type.", null);
        }
    }

    private void closeConnections() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing client connections: " + e.getMessage());
        }
    }
}
