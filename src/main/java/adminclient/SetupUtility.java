package adminclient;

import server.dao.UserDAO;
import shared.model.User;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Scanner;

public class SetupUtility {
    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   CONFIGURATION DE L'ADMINISTRATEUR (SETUP)   ");
        System.out.println("=================================================");
        System.out.println("Ce script extrait la clé publique d'un Keystore (.p12) existant et l'enregistre dans la base de données.");
        System.out.println("1) Générez un Keystore avec Keytool :");
        System.out.println("   keytool -genkeypair -alias admin -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore admin_keystore.p12 -validity 365 -dname \"CN=Admin, OU=IT, O=ChriOnline, L=Paris, S=Paris, C=FR\"\n");

        Scanner scanner = new Scanner(System.in);

        System.out.print("Entrez l'email du compte administrateur : ");
        String email = scanner.nextLine();

        System.out.print("Chemin vers le fichier Keystore (appuyez sur Entrée pour 'admin_keystore.p12') : ");
        String keystorePath = scanner.nextLine();
        if (keystorePath.isEmpty()) keystorePath = "admin_keystore.p12";

        System.out.print("Mot de passe du Keystore : ");
        String password = scanner.nextLine();

        System.out.print("Alias de la clé (appuyez sur Entrée pour 'admin') : ");
        String alias = scanner.nextLine();
        if (alias.isEmpty()) alias = "admin";

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keyStore.load(fis, password.toCharArray());
            }

            PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
            String base64PublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());

            System.out.println("\n[*] Clé publique extraite avec succès depuis le Keystore !");

            UserDAO userDAO = new UserDAO();
            User user = userDAO.findByEmail(email);

            if (user == null) {
                System.out.println("[*] L'utilisateur " + email + " n'existe pas. Création d'un profil administrateur fictif.");
                User newUser = new User();
                newUser.setUsername("Admin");
                newUser.setEmail(email);
                newUser.setPassword("N/A_AUTH_RSA");
                newUser.setRole("ADMIN");
                userDAO.save(newUser, "ADMIN-CODE");
                System.out.println("[*] Utilisateur créé.");
            } else if (!"ADMIN".equals(user.getRole())) {
                System.out.println("[*] L'utilisateur n'avait pas le rôle ADMIN. Action requise dans la BDD pour changer son rôle, ou il le faut manuellement.");
                // Optionnel: on pourrait forcer le rôle ici, mais laissons l'Admin public key insertion le requerir.
            }

            boolean success = userDAO.saveAdminPublicKey(email, base64PublicKey);
            if (success) {
                System.out.println("[SUCCESS] Clé publique enregistrée en base de données pour " + email);
                System.out.println("Vous pouvez maintenant tester l'authentification avec 'AdminMain' !");
            } else {
                System.out.println("[ERROR] Impossible de sauvegarder en base. Assurez-vous que le rôle de l'utilisateur est bien 'ADMIN'.");
            }

        } catch (Exception e) {
            System.err.println("[ERROR] Erreur technique : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
