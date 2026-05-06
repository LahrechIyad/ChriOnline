package server.service;

import server.dao.UserDAO;
import shared.model.User;
import shared.network.Response;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Service handling User Business Logic and Security.
 */
public class UserService {
    private UserDAO userDAO;
    private EmailService emailService;

    // TP1 - Brute Force Mitigation: Tracking failed logins
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_MS = 60000; // 1 minute lock
    private Map<String, Integer> failedAttempts;
    private Map<String, Long> lockedAccounts;

    // TP7 - Strict Input Validation Regex
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");

    public UserService() {
        this.userDAO = new UserDAO();
        this.emailService = new EmailService();
        this.failedAttempts = new ConcurrentHashMap<>();
        this.lockedAccounts = new ConcurrentHashMap<>();
    }

    /**
     * Registers a new user.
     */
    public Response registerUser(User user) {
        if (user.getUsername() == null || user.getEmail() == null || user.getPassword() == null) {
            return new Response(false, "Missing required fields.", null);
        }

        // TP7: Strict Server-Side Validation
        if (!USERNAME_PATTERN.matcher(user.getUsername()).matches()) {
            return new Response(false, "Invalid username format (3-20 alphanumeric or underscore).", null);
        }
        if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            return new Response(false, "Invalid email format.", null);
        }

        if (userDAO.findByEmail(user.getEmail()) != null) {
            return new Response(false, "Email already exists.", null);
        }

        // Generate Registration Code
        String verificationCode = String.format("%06d", new Random().nextInt(999999));

        if (userDAO.save(user, verificationCode)) {
            // Send mail
            new Thread(() -> emailService.sendRegistrationVerification(user.getEmail(), verificationCode)).start();
            return new Response(true, "User registered successfully. Please verify your email.", null);
        } else {
            return new Response(false, "Failed to register user.", null);
        }
    }

    /**
     * Verifies the user account.
     */
    public Response verifyAccount(String email, String code) {
        if (userDAO.verifyUser(email, code)) {
            return new Response(true, "Account verified successfully. You can now login.", null);
        } else {
            return new Response(false, "Invalid verification code or already verified.", null);
        }
    }

    /**
     * Authenticates a user with Brute Force protection.
     */
    public Response login(String email, String password) {
        if (email == null || password == null) {
            return new Response(false, "Email and password are required.", null);
        }

        // TP1: Check if locked
        if (lockedAccounts.containsKey(email)) {
            long lockTime = lockedAccounts.get(email);
            if (System.currentTimeMillis() - lockTime < LOCK_TIME_MS) {
                return new Response(false, "Account is temporarily locked due to too many failed attempts.", null);
            } else {
                lockedAccounts.remove(email);
                failedAttempts.remove(email);
            }
        }

        User user = userDAO.authenticate(email, password);
        if (user != null) {
            // Login successful
            failedAttempts.remove(email);

            // Require email verification
            if (!user.isVerified()) {
                return new Response(false, "Account not verified. Please check your email for the verification code.", "NOT_VERIFIED");
            }

            return new Response(true, "Login successful.", user);
        } else {
            // TP1: Failed login, increment attempts
            int attempts = failedAttempts.getOrDefault(email, 0) + 1;
            failedAttempts.put(email, attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                lockedAccounts.put(email, System.currentTimeMillis());
                return new Response(false, "Too many failed attempts. Account locked for 1 minute.", null);
            }

            return new Response(false, "Invalid email or password. Attempts remaining: " + (MAX_FAILED_ATTEMPTS - attempts), null);
        }
    }
}
