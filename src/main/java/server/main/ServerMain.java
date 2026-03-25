package server.main;

import server.database.DBConnection;
import server.network.ServerTCP;

public class ServerMain {
    public static void main(String[] args) {
        try {
            System.out.println("Initializing ChriOnline Server...");

            DBConnection.initializeDatabase();

            int port = 8081;
            ServerTCP server = new ServerTCP(port);

            System.out.println("Starting server on port " + port + "...");
            server.start();

        } catch (Exception e) {
            System.err.println("Server startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}