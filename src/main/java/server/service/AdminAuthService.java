package server.service;

import server.dao.UserDAO;
import shared.security.ChallengeGenerator;
import shared.security.Verifier;
import shared.model.User;

import java.security.PublicKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdminAuthService {
    private UserDAO userDAO;
    
    // Store active challenges. Map<Email, ChallengeData>
    private static Map<String, ChallengeData> activeChallenges = new ConcurrentHashMap<>();

    private static class ChallengeData {
        String challenge;
        long timestamp;

        ChallengeData(String challenge) {
            this.challenge = challenge;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public AdminAuthService() {
        this.userDAO = new UserDAO();
    }

    public String generateChallenge(String email) {
        User user = userDAO.findByEmail(email);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return null; // Not an admin or not found
        }
        String challenge = ChallengeGenerator.generateChallenge();
        activeChallenges.put(email, new ChallengeData(challenge));
        return challenge;
    }

    public User verifyChallenge(String email, byte[] signatureBytes) {
        ChallengeData data = activeChallenges.remove(email);
        if (data == null) {
            System.err.println("No active challenge found for email: " + email);
            return null;
        }

        // Check expiration (30 seconds)
        if (System.currentTimeMillis() - data.timestamp > 30000) {
            System.err.println("Challenge expired for email: " + email);
            return null;
        }

        String base64PublicKey = userDAO.getPublicKey(email);
        if (base64PublicKey == null) {
            System.err.println("No public key registered for admin: " + email);
            return null;
        }

        try {
            PublicKey publicKey = Verifier.getPublicKeyFromBase64(base64PublicKey);
            boolean isValid = Verifier.verify(data.challenge, signatureBytes, publicKey);
            if (isValid) {
                return userDAO.findByEmail(email);
            } else {
                System.err.println("Invalid signature for admin: " + email);
            }
        } catch (Exception e) {
            System.err.println("Error verifying signature: " + e.getMessage());
        }

        return null; // Invalid signature
    }
}
