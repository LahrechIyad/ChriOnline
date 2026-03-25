package server.network;

import server.service.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main TCP Server that listens for client connections.
 */
public class ServerTCP {
    private int port;
    private boolean isRunning;
    
    // Shared Services
    private UserService userService;
    private ProduitService produitService;
    private PanierService panierService;
    private CommandeService commandeService;
    private PaiementService paiementService;

    public ServerTCP(int port) {
        this.port = port;
        
        // Initialize services
        this.userService = new UserService();
        this.produitService = new ProduitService();
        this.panierService = new PanierService(); // Shared instance since it holds in-memory carts
        this.commandeService = new CommandeService(this.panierService);
        this.paiementService = new PaiementService(this.panierService);
    }

    /**
     * Starts the server.
     */
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port: " + port);
            isRunning = true;

            while (isRunning) {
                System.out.println("Waiting for client connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // Delegate to a new ClientHandler thread
                ClientHandler handler = new ClientHandler(
                    clientSocket, 
                    userService, 
                    produitService, 
                    panierService, 
                    commandeService, 
                    paiementService
                );
                
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
    }
}
