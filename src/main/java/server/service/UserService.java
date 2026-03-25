package server.service;

import server.dao.UserDAO;
import shared.model.User;
import shared.network.Response;

/**
 * Service handling User Business Logic.
 */
public class UserService {
    private UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * Registers a new user.
     */
    public Response registerUser(User user) {
        if (user.getUsername() == null || user.getEmail() == null || user.getPassword() == null) {
            return new Response(false, "Missing required fields.", null);
        }

        if (userDAO.findByEmail(user.getEmail()) != null) {
            return new Response(false, "Email already exists.", null);
        }

        if (userDAO.save(user)) {
            return new Response(true, "User registered successfully.", null);
        } else {
            return new Response(false, "Failed to register user.", null);
        }
    }

    /**
     * Authenticates a user.
     */
    public Response login(String email, String password) {
        if (email == null || password == null) {
            return new Response(false, "Email and password are required.", null);
        }

        User user = userDAO.authenticate(email, password);
        if (user != null) {
            return new Response(true, "Login successful.", user);
        } else {
            return new Response(false, "Invalid email or password.", null);
        }
    }
}
