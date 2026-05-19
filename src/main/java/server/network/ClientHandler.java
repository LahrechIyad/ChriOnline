package server.network;

import shared.network.Request;
import shared.network.Response;
import shared.model.User;
import shared.model.Produit;
import server.service.*;
import server.security.IntrusionDetectionService;
import server.security.SecurityEventLogger;
import server.security.ServerKeyManager;
import shared.security.AESUtil;
import shared.security.RSAUtil;
import shared.security.SecureMessage;
import shared.security.SecureSession;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;

/**
 * Handles communication with a single connected client.
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private final String clientIp;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private final SecureSession secureSession = new SecureSession();
    
    // Services
    private UserService userService;
    private ProduitService produitService;
    private PanierService panierService;
    private CommandeService commandeService;
    private PaiementService paiementService;
    private AdminAuthService adminAuthService;
    private ServerKeyManager serverKeyManager;

    // TP5 - Session State
    // We bind tokens to Users globally, so if TCP connection drops, token is still valid.
    private static Map<String, User> activeSessions = new ConcurrentHashMap<>();
    
    // TP2 - Replay Attack Cache
    private static Set<String> usedNonces = ConcurrentHashMap.newKeySet();
    private static final long MAX_TIMESTAMP_AGE_MS = 5 * 60 * 1000; // 5 minutes

    public ClientHandler(Socket clientSocket, UserService userService, ProduitService produitService,
                         PanierService panierService, CommandeService commandeService, PaiementService paiementService,
                         AdminAuthService adminAuthService, ServerKeyManager serverKeyManager) {
        this.clientSocket = clientSocket;
        this.clientIp = clientSocket.getInetAddress().getHostAddress();
        this.userService = userService;
        this.produitService = produitService;
        this.panierService = panierService;
        this.commandeService = commandeService;
        this.paiementService = paiementService;
        this.adminAuthService = adminAuthService;
        this.serverKeyManager = serverKeyManager;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());
            performHandshake();

            while (true) {
                if (IntrusionDetectionService.isBlocked(clientIp)) {
                    sendResponse(new Response(false, "IP temporarily blocked by IPS.", null));
                    break;
                }

                SecureMessage secureMessage = (SecureMessage) in.readObject();
                if (secureMessage == null) break;
                Request request = (Request) AESUtil.decryptObject(secureMessage, secureSession.getAesKey());
                IntrusionDetectionService.recordRequest(clientIp);

                // TP2 - Replay Attack Check
                if (!isValidRequest(request)) {
                    SecurityEventLogger.alert(getRequestUser(request), clientIp, request.getType(), "Invalid timestamp or replayed nonce");
                    sendResponse(new Response(false, "Security Check Failed: Invalid or replayed request.", null));
                    continue;
                }

                Response response = processRequest(request);
                auditRequest(request, response);
                sendResponse(response);
            }
        } catch (Exception e) {
            SecurityEventLogger.log("CONNECTION", "-", clientIp, "DISCONNECT", e.getMessage());
            System.out.println("Client disconnected or error: " + e.getMessage());
        } finally {
            closeConnections();
        }
    }
    
    private void sendResponse(Response response) throws IOException {
        try {
            out.reset();
            out.writeObject(AESUtil.encryptObject(response, secureSession.getAesKey()));
            out.flush();
        } catch (Exception e) {
            throw new IOException("Failed to encrypt response", e);
        }
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

    private void auditRequest(Request request, Response response) {
        String type = request.getType();
        String user = getRequestUser(request);
        String status = response.isSuccess() ? "SUCCESS" : "FAILED: " + response.getMessage();

        switch (type) {
            case "LOGIN":
                SecurityEventLogger.log("AUTH", user, clientIp, type, status);
                if (!response.isSuccess()) {
                    IntrusionDetectionService.recordLoginFailure(user, clientIp);
                }
                break;

            case "VERIFY_EMAIL":
                SecurityEventLogger.log("OTP", user, clientIp, type, status);
                if (!response.isSuccess()) {
                    IntrusionDetectionService.recordOtpFailure(user, clientIp);
                }
                break;

            case "ADMIN_AUTH_REQUEST":
            case "ADMIN_AUTH_VERIFY":
                SecurityEventLogger.log("ADMIN_AUTH", user, clientIp, type, status);
                if (!response.isSuccess()) {
                    IntrusionDetectionService.recordLoginFailure(user, clientIp);
                }
                break;

            case "GET_MY_ORDERS":
            case "ADMIN_GET_ORDERS":
            case "ADMIN_GET_ORDER_DETAILS":
            case "PROCESS_PAYMENT":
                SecurityEventLogger.log("SENSITIVE_ACCESS", user, clientIp, type, status);
                break;

            default:
                if (type != null && type.startsWith("ADMIN_")) {
                    SecurityEventLogger.log("ADMIN_ACTION", user, clientIp, type, status);
                    if (!response.isSuccess() && response.getMessage() != null
                            && response.getMessage().contains("Admin role required")) {
                        IntrusionDetectionService.recordAdminDenied(user, clientIp, type);
                    }
                }
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private String getRequestUser(Request request) {
        if (request.getSessionToken() != null && activeSessions.containsKey(request.getSessionToken())) {
            return activeSessions.get(request.getSessionToken()).getEmail();
        }
        Object data = request.getData();
        if (data instanceof Map) {
            Object email = ((Map<String, Object>) data).get("email");
            if (email instanceof String) {
                return (String) email;
            }
        }
        if (data instanceof String && request.getType() != null && request.getType().contains("AUTH")) {
            return (String) data;
        }
        return "-";
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

            case "RESEND_VERIFICATION_CODE":
                if (data instanceof String) {
                    return userService.resendVerificationCode((String) data);
                }
                return new Response(false, "Invalid email format for resend request.", null);

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

            case "SEARCH_PRODUCTS":
                return produitService.searchProducts((String) data);

            case "FILTER_PRODUCTS_BY_CATEGORY":
                return produitService.filterProductsByCategory((String) data);

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
                    String maskedCard = (String) paymentData.get("maskedCard");
                    if ((maskedCard == null || maskedCard.isBlank()) && paymentData.get("cardNumber") instanceof String) {
                        maskedCard = maskCardNumber((String) paymentData.get("cardNumber"));
                    }
                    String deliveryAddress = (String) paymentData.get("deliveryAddress");
                    return paiementService.processPayment(
                        (Integer) paymentData.get("orderId"),
                        (Double) paymentData.get("amount"),
                        (String) paymentData.get("method"),
                        maskedCard,
                        deliveryAddress
                    );
                }
                return new Response(false, "Invalid payment data.", null);

            case "GET_MY_ORDERS":
                if (loggedInUser == null) return new Response(false, "Unauthenticated session.", null);
                return commandeService.getOrdersByUser(loggedInUser.getId());

            case "ADMIN_GET_DASHBOARD_STATS":
                if (!isAdmin(loggedInUser)) return new Response(false, "Admin role required.", null);
                return commandeService.getDashboardStats();

            case "ADMIN_GET_PRODUCTS":
                if (!isAdmin(loggedInUser)) return new Response(false, "Admin role required.", null);
                return produitService.getAllProducts();

            case "ADMIN_CREATE_PRODUCT":
                if (!isAdmin(loggedInUser)) return new Response(false, "Admin role required.", null);
                return produitService.createProduct((Produit) data);

            case "ADMIN_UPDATE_PRODUCT":
                if (!isAdmin(loggedInUser)) return new Response(false, "Admin role required.", null);
                return produitService.updateProduct((Produit) data);

            case "ADMIN_DELETE_PRODUCT":
                if (!isAdmin(loggedInUser)) return new Response(false, "Admin role required.", null);
                return produitService.deleteProduct((Integer) data);

            case "ADMIN_GET_ORDERS":
                if (!isAdmin(loggedInUser)) return new Response(false, "Admin role required.", null);
                return commandeService.getAllOrders((String) data);

            case "ADMIN_GET_ORDER_DETAILS":
                if (!isAdmin(loggedInUser)) return new Response(false, "Admin role required.", null);
                return commandeService.getOrderDetails((Integer) data);

            case "ADMIN_UPDATE_ORDER_STATUS":
                if (!isAdmin(loggedInUser)) return new Response(false, "Admin role required.", null);
                if (data instanceof Map) {
                    Map<String, Object> statusData = (Map<String, Object>) data;
                    return commandeService.updateOrderStatus((Integer) statusData.get("orderId"), (String) statusData.get("status"));
                }
                return new Response(false, "Invalid order status data.", null);

            case "ADMIN_GET_LOW_STOCK":
                if (!isAdmin(loggedInUser)) return new Response(false, "Admin role required.", null);
                Integer threshold = data instanceof Integer ? (Integer) data : 5;
                return produitService.getLowStockProducts(threshold);

            default:
                return new Response(false, "Unknown request type.", null);
        }
    }

    private void performHandshake() throws Exception {
        System.out.println("Server: Secure handshake started");
        out.writeObject(serverKeyManager.getPublicKey().getEncoded());
        out.flush();
        System.out.println("Server: Server public key sent");
        byte[] encryptedAesKey = (byte[]) in.readObject();
        SecretKey aesKey = RSAUtil.decryptAesKey(encryptedAesKey, serverKeyManager.getPrivateKey());
        secureSession.setAesKey(aesKey);
        System.out.println("Server: AES session key received and decrypted");
        System.out.println("Server: Secure AES communication established");
    }

    private boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return null;
        }
        String digits = cardNumber.replaceAll("\\s+", "");
        if (digits.length() < 4) {
            return "****";
        }
        return "**** **** **** " + digits.substring(digits.length() - 4);
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
