package adminclient.ui;

import adminclient.network.AdminClientHandler;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Scanner;

public class AdminUI {
    private AdminClientHandler clientHandler;
    private Scanner scanner;

    public AdminUI(AdminClientHandler clientHandler) {
        this.clientHandler = clientHandler;
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=========================================");
        System.out.println("       PORTAIL D'ADMINISTRATION          ");
        System.out.println(" Authentification Challenge-Response RSA ");
        System.out.println("=========================================");

        System.out.print("Email administrateur : ");
        String email = scanner.nextLine();

        System.out.print("Chemin du Keystore PKCS12 (ex: admin_keystore.p12) : ");
        String keystorePath = scanner.nextLine();

        System.out.print("Mot de passe Keystore : ");
        String password = scanner.nextLine();

        System.out.print("Alias de la clé : ");
        String alias = scanner.nextLine();

        PrivateKey privateKey = null;
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keyStore.load(fis, password.toCharArray());
            }
            privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());

            if (privateKey == null) {
                System.out.println("Erreur: Clé privée introuvable pour l'alias " + alias);
                return;
            }

            System.out.println("[*] Clé privée chargée avec succès depuis le Keystore professionnel.");

        } catch (Exception e) {
            System.out.println("Échec du chargement du Keystore: " + e.getMessage());
            return;
        }

        System.out.println("\n[*] Début de la séquence d'authentification sans mot de passe...");
        boolean success = clientHandler.authenticateAdmin(email, privateKey);

        if (success) {
            showDashboard();
        } else {
            System.out.println("[!] Échec critique de l'authentification. Session refusée.");
        }
    }

    private void showDashboard() {
        System.out.println("\n=========================================");
        System.out.println("          DASHBOARD ADMINISTRATEUR         ");
        System.out.println("=========================================");
        System.out.println("Bienvenue dans le panneau de contrôle ultra-sécurisé.");
        boolean inDashboard = true;
        
        while (inDashboard) {
            System.out.println("\nOptions :");
            System.out.println("1) Voir les statistiques du serveur (Simulation)");
            System.out.println("2) Quitter");
            System.out.print("> ");
            
            String choice = scanner.nextLine();
            switch(choice) {
                case "1":
                    clientHandler.getDashboardData();
                    break;
                case "2":
                    System.out.println("Déconnexion...");
                    inDashboard = false;
                    break;
                default:
                    System.out.println("Choix invalide.");
            }
        }
    }
}
