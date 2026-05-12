package server.dao;

import shared.model.User;
import server.database.DBConnection;
import server.security.DatabaseCryptoService;

import java.sql.*;

/**
 * Handles database operations for Users.
 */
public class UserDAO {
    private Connection connection;
    private final DatabaseCryptoService cryptoService;

    public UserDAO() {
        this.connection = DBConnection.getConnection();
        this.cryptoService = new DatabaseCryptoService();
    }

    /**
     * Saves a new user to the database.
     * @param user User object containing registration details
     * @return True if insertion is successful, false otherwise
     */
    public boolean save(User user, String verificationCode) {
        String query = "INSERT INTO users (username, email, password, role, is_verified, verification_code, encrypted_verification_code) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole() != null ? user.getRole() : "CUSTOMER");
            stmt.setBoolean(5, false); // always false on register
            stmt.setString(6, verificationCode);
            stmt.setString(7, cryptoService.encryptToBase64(verificationCode));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds a user by email (useful for checking if user exists).
     * @param email User email
     * @return User object if found, null otherwise
     */
    public User findByEmail(String email) {
        String query = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getBoolean("is_verified")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks if email and password match.
     * @param email User email
     * @param password User password
     * @return User object if authentication is successful, null otherwise
     */
    public User authenticate(String email, String password) {
        String query = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getBoolean("is_verified")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds a user by ID.
     */
    public User findById(int id) {
        String query = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getBoolean("is_verified")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Verifies the user if the code matches.
     */
    public boolean verifyUser(String email, String code) {
        String query = "SELECT id, is_verified, encrypted_verification_code, verification_code FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next() || rs.getBoolean("is_verified")) {
                return false;
            }
            String encryptedCode = rs.getString("encrypted_verification_code");
            String storedCode = encryptedCode != null ? cryptoService.decryptFromBase64(encryptedCode) : rs.getString("verification_code");
            if (!code.equals(storedCode)) {
                return false;
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE users SET is_verified = 1, verification_code = NULL, encrypted_verification_code = NULL WHERE id = ?")) {
                update.setInt(1, rs.getInt("id"));
                return update.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error verifying user: " + e.getMessage());
            return false;
        }
    }

    public boolean updateVerificationCode(String email, String verificationCode) {
        String query = "UPDATE users SET verification_code = ?, encrypted_verification_code = ?, is_verified = 0 WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, verificationCode);
            stmt.setString(2, cryptoService.encryptToBase64(verificationCode));
            stmt.setString(3, email);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating verification code: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets the public key of an admin user.
     */
    public String getPublicKey(String email) {
        String query = "SELECT public_key FROM users WHERE email = ? AND role = 'ADMIN'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("public_key");
            }
        } catch (SQLException e) {
            System.err.println("Error finding public key by email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the public key for an admin user.
     */
    public boolean saveAdminPublicKey(String email, String publicKeyBase64) {
        String query = "UPDATE users SET public_key = ? WHERE email = ? AND role = 'ADMIN'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, publicKeyBase64);
            stmt.setString(2, email);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving public key: " + e.getMessage());
            return false;
        }
    }
}
