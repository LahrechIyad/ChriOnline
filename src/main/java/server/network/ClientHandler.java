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

    // Session State
    private User loggedInUser = null;

    public ClientHandler(Socket clientSocket, UserService userService, ProduitService produitService,
                         PanierService panierService, CommandeService commandeService, PaiementService paiementService) {
        this.clientSocket = clientSocket;
        this.userService = userService;
        this.produitService = produitService;
        this.panierService = panierService;
        this.commandeService = commandeService;
        this.paiementService = paiementService;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                Request request = (Request) in.readObject();
                if (request == null) break;

                Response response = processRequest(request);
                out.reset(); // Prevent ObjectOutputStream from caching stale objects
                out.writeObject(response);
                out.flush();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected or error: " + e.getMessage());
        } finally {
            closeConnections();
        }
    }

    @SuppressWarnings("unchecked")
    private Response processRequest(Request request) {
        String type = request.getType();
        Object data = request.getData();

        switch (type) {
            case "REGISTER":
                return userService.registerUser((User) data);
                
            case "LOGIN":
                if (data instanceof Map) {
                    Map<String, String> credentials = (Map<String, String>) data;
                    Response loginResp = userService.login(credentials.get("email"), credentials.get("password"));
                    if (loginResp.isSuccess()) {
                        this.loggedInUser = (User) loginResp.getData();
                    }
                    return loginResp;
                }
                return new Response(false, "Invalid login data format.", null);

            case "LOGOUT":
                this.loggedInUser = null;
                return new Response(true, "Logged out successfully.", null);

            case "GET_PRODUCTS":
                return produitService.getAllProducts();

            case "GET_PRODUCT":
                return produitService.getProductDetails((Integer) data);

            case "ADD_TO_CART":
                if (loggedInUser == null) return new Response(false, "Must be logged in.", null);
                if (data instanceof Map) {
                    Map<String, Integer> cartData = (Map<String, Integer>) data;
                    return panierService.addProductToCart(loggedInUser.getId(), cartData.get("productId"), cartData.get("quantity"));
                }
                return new Response(false, "Invalid cart data.", null);

            case "VIEW_CART":
                if (loggedInUser == null) return new Response(false, "Must be logged in.", null);
                return panierService.getCart(loggedInUser.getId());

            case "REMOVE_FROM_CART":
                if (loggedInUser == null) return new Response(false, "Must be logged in.", null);
                return panierService.removeProductFromCart(loggedInUser.getId(), (Integer) data);


            case "CREATE_ORDER":
                if (loggedInUser == null) return new Response(false, "Must be logged in.", null);
                return commandeService.createOrder(loggedInUser.getId());

            case "PROCESS_PAYMENT":
                if (loggedInUser == null) return new Response(false, "Must be logged in.", null);
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
                if (loggedInUser == null) return new Response(false, "Must be logged in.", null);
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
