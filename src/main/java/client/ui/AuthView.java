package client.ui;

import client.network.ClientTCP;
import shared.model.User;
import shared.network.Request;
import shared.network.Response;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class AuthView {
    private VBox view;
    private ClientTCP clientTCP;
    private Stage stage;
    
    // View States: 0 = Login, 1 = Register, 2 = Verify Email
    private int currentMode = 0; 

    private TextField usernameField;
    private TextField emailField;
    private PasswordField passwordField;
    private TextField verifyCodeField; // For Email Verification
    private Label title;
    private Button mainBtn;
    private Button switchBtn;
    private Label messageLabel;

    public AuthView(ClientTCP clientTCP, Stage stage) {
        this.clientTCP = clientTCP;
        this.stage = stage;
        buildView();
    }

    private void buildView() {
        view = new VBox(15);
        view.setPadding(new Insets(30));
        view.setAlignment(Pos.CENTER);
        view.setStyle("-fx-background-color: #f8f9fa;");

        title = new Label("Login to ChriOnline");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #343a40;");

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(300);
        usernameField.setVisible(false);
        usernameField.setManaged(false);

        emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setMaxWidth(300);

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(300);
        
        verifyCodeField = new TextField();
        verifyCodeField.setPromptText("6-digit Verification Code");
        verifyCodeField.setMaxWidth(300);
        verifyCodeField.setVisible(false);
        verifyCodeField.setManaged(false);

        mainBtn = new Button("Login");
        mainBtn.setStyle("-fx-background-color: #0d6efd; -fx-text-fill: white; -fx-font-weight: bold;");
        mainBtn.setPrefWidth(300);

        switchBtn = new Button("Don't have an account? Register");
        switchBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0d6efd; -fx-underline: true;");

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);

        mainBtn.setOnAction(e -> handleAction());
        switchBtn.setOnAction(e -> switchMode());

        view.getChildren().addAll(title, usernameField, emailField, passwordField, verifyCodeField, mainBtn, switchBtn, messageLabel);
    }

    private void switchMode() {
        if (currentMode == 0) {
            // From Login -> Register
            currentMode = 1;
            title.setText("Create an Account");
            usernameField.setVisible(true);
            usernameField.setManaged(true);
            verifyCodeField.setVisible(false);
            verifyCodeField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            mainBtn.setText("Register");
            switchBtn.setText("Already have an account? Login");
            switchBtn.setVisible(true);
        } else if (currentMode == 1) {
            // From Register -> Login
            currentMode = 0;
            title.setText("Login to ChriOnline");
            usernameField.setVisible(false);
            usernameField.setManaged(false);
            verifyCodeField.setVisible(false);
            verifyCodeField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            mainBtn.setText("Login");
            switchBtn.setText("Don't have an account? Register");
            switchBtn.setVisible(true);
        } else if (currentMode == 2) {
            // From Verify -> Login manually
            currentMode = 0;
            title.setText("Login to ChriOnline");
            usernameField.setVisible(false);
            usernameField.setManaged(false);
            verifyCodeField.setVisible(false);
            verifyCodeField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            mainBtn.setText("Login");
            switchBtn.setText("Don't have an account? Register");
            switchBtn.setVisible(true);
        }
        
        messageLabel.setText("");
        passwordField.clear();
        usernameField.clear();
        verifyCodeField.clear();
    }
    
    private void setVerifyMode(String emailToVerify) {
        currentMode = 2; // Verify State
        title.setText("Verify Email");
        emailField.setText(emailToVerify);
        emailField.setDisable(true); // Don't let them change it now
        usernameField.setVisible(false);
        usernameField.setManaged(false);
        passwordField.setVisible(false);
        passwordField.setManaged(false);
        
        verifyCodeField.setVisible(true);
        verifyCodeField.setManaged(true);
        
        mainBtn.setText("Verify Code");
        switchBtn.setText("Back to Login");
        switchBtn.setVisible(true);
        messageLabel.setText("Please enter the verification code sent to your email.");
        messageLabel.setStyle("-fx-text-fill: green;");
    }

    private void handleAction() {
        if (currentMode == 0) {
            // LOGIN
            Map<String, String> creds = new HashMap<>();
            creds.put("email", emailField.getText());
            creds.put("password", hashPassword(passwordField.getText()));

            Response resp = clientTCP.sendRequest(new Request("LOGIN", creds));
            if (resp != null && resp.isSuccess()) {
                // TP5 - Retrieve and set session Token
                Map<String, Object> respData = (Map<String, Object>) resp.getData();
                User user = (User) respData.get("user");
                String token = (String) respData.get("token");
                
                clientTCP.setSessionToken(token); // Set it globally for the client
                
                MainDashboard dashboard = new MainDashboard(clientTCP, stage, user);
                stage.setScene(new Scene(dashboard.getView(), 900, 600));
            } else if (resp != null && "NOT_VERIFIED".equals(resp.getData())) {
                // Requires Verification
                setVerifyMode(emailField.getText());
            } else {
                showError(resp != null ? resp.getMessage() : "Connection failed.");
            }
        } else if (currentMode == 1) {
            // REGISTER
            String rawPassword = passwordField.getText();
            
            // Password Complexity Validation
            if (rawPassword == null || rawPassword.length() < 6 
                || !rawPassword.matches(".*[A-Z].*") 
                || !rawPassword.matches(".*[^a-zA-Z0-9].*")) {
                showError("Password must be at least 6 characters, with 1 uppercase and 1 special character.");
                return;
            }

            User newUser = new User(usernameField.getText(), emailField.getText(), hashPassword(rawPassword), "CUSTOMER");
            Response resp = clientTCP.sendRequest(new Request("REGISTER", newUser));
            if (resp != null && resp.isSuccess()) {
                setVerifyMode(emailField.getText());
            } else {
                showError(resp != null ? resp.getMessage() : "Connection failed.");
            }
        } else if (currentMode == 2) {
            // VERIFY
            Map<String, String> verifData = new HashMap<>();
            verifData.put("email", emailField.getText());
            verifData.put("code", verifyCodeField.getText());

            Response resp = clientTCP.sendRequest(new Request("VERIFY_EMAIL", verifData));
            if (resp != null && resp.isSuccess()) {
                messageLabel.setText("Account Verified! You can now log in.");
                messageLabel.setStyle("-fx-text-fill: green;");
                emailField.setDisable(false);
                switchMode(); // Back to login
            } else {
                showError(resp != null ? resp.getMessage() : "Verification failed.");
            }
        }
    }

    /**
     * Hashes password on client side with SHA-256 before transmitting.
     */
    private String hashPassword(String password) {
        if (password == null || password.isEmpty()) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: red;");
    }

    public VBox getView() {
        return view;
    }
}
