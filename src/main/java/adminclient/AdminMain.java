package adminclient;

import adminclient.network.AdminClientHandler;
import adminclient.ui.AdminUI;

import java.util.Scanner;

public class AdminMain {
    public static void main(String[] args) {
        System.out.println("Démarrage du Client d'Administration...");
        
        AdminClientHandler clientHandler = new AdminClientHandler("localhost", 8081);
        
        try {
            clientHandler.connect();
            System.out.println("Connecté au serveur sur le port 8081.");
            
            AdminUI ui = new AdminUI(clientHandler);
            ui.start();
            
        } catch (Exception e) {
            System.err.println("Impossible de se connecter au serveur : " + e.getMessage());
        } finally {
            clientHandler.disconnect();
            System.out.println("Application fermée.");
        }
    }
}
