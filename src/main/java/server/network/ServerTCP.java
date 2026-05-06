package server.network;

import server.service.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    private AdminAuthService adminAuthService;
    
    // TP3 - Thread Pool to prevent resource exhaustion (SYN Flood / simple connection flood)
    private ExecutorService threadPool = Executors.newFixedThreadPool(50); // Limit to 50 concurrent connections
    
    // TP4 - Application Layer IP Rate Limiter (Since UDP is absent)
    private Map<String, Integer> connectionCounts = new ConcurrentHashMap<>();
    private static final int MAX_CONN_PER_IP = 10; // Max 10 simultaneous connections per IP

    public ServerTCP(int port) {
        this.port = port;
        
        // Initialize services
        this.userService = new UserService();
        this.produitService = new ProduitService();
        this.panierService = new PanierService(); // Shared instance since it holds in-memory carts
        this.commandeService = new CommandeService(this.panierService);
        this.paiementService = new PaiementService(this.panierService);
        this.adminAuthService = new AdminAuthService();
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
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                
                // Track and limit connections by IP
                int currentConns = connectionCounts.getOrDefault(clientIP, 0);
                if (currentConns >= MAX_CONN_PER_IP) {
                    System.err.println("Rejected connection from " + clientIP + ": Rate limit exceeded.");
                    clientSocket.close();
                    continue;
                }
                
                connectionCounts.put(clientIP, currentConns + 1);
                System.out.println("Client connected: " + clientIP);

                // Delegate to a new ClientHandler thread safely via ThreadPool
                ClientHandler handler = new ClientHandler(
                    clientSocket, 
                    userService, 
                    produitService, 
                    panierService, 
                    commandeService, 
                    paiementService,
                    adminAuthService
                );
                
                // Wrap in runnable to decrement counter when finished
                threadPool.execute(() -> {
                    try {
                        handler.run();
                    } finally {
                        // Decrease connection count for this IP
                        connectionCounts.put(clientIP, connectionCounts.get(clientIP) - 1);
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    public void stop() {
        isRunning = false;
        threadPool.shutdown();
    }
}
