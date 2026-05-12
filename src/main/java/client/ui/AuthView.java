package client.ui;

import client.network.ClientTCP;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import shared.model.User;
import shared.network.Request;
import shared.network.Response;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class AuthView {
    private VBox view;
    private final ClientTCP clientTCP;
    private final Stage stage;
    private int currentMode = 0;
    private TextField usernameField;
    private TextField emailField;
    private PasswordField passwordField;
    private TextField verifyCodeField;
    private Label title;
    private Button mainBtn;
    private Button switchBtn;
    private Button resendBtn;
    private Label messageLabel;

    public AuthView(ClientTCP clientTCP, Stage stage) {
        this.clientTCP = clientTCP;
        this.stage = stage;
        buildView();
    }

    private void buildView() {
        view = new VBox();
        view.getStyleClass().add("auth-root");

        HBox split = new HBox(28);
        split.setPadding(new Insets(28));
        split.setAlignment(Pos.CENTER);

        VBox hero = new VBox(18);
        hero.getStyleClass().add("hero-pane");
        hero.setPadding(new Insets(40));
        hero.setPrefWidth(520);
        HBox.setHgrow(hero, Priority.ALWAYS);
        Label brand = new Label("ChriOnline Tech");
        brand.getStyleClass().add("hero-brand");
        Label headline = new Label("Secure electronics shopping with a premium storefront.");
        headline.getStyleClass().add("hero-title");
        headline.setWrapText(true);
        Label sub = new Label("Secure electronics shopping with AES/RSA protected communication");
        sub.getStyleClass().add("hero-subtitle");
        sub.setWrapText(true);
        HBox badges = new HBox(10, badge("AES session"), badge("RSA handshake"), badge("Verified account"));
        Label note = new Label("Discover smartphones, laptops, tablets and accessories over an encrypted client-server channel.");
        note.getStyleClass().add("hero-caption");
        note.setWrapText(true);
        hero.getChildren().addAll(brand, headline, sub, badges, note);

        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(380);
        card.setPrefWidth(380);

        title = new Label("Welcome back");
        title.getStyleClass().add("section-title");
        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setVisible(false);
        usernameField.setManaged(false);
        emailField = new TextField();
        emailField.setPromptText("Email address");
        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        verifyCodeField = new TextField();
        verifyCodeField.setPromptText("6-digit verification code");
        verifyCodeField.setVisible(false);
        verifyCodeField.setManaged(false);
        mainBtn = new Button("Login");
        mainBtn.getStyleClass().add("primary-button");
        mainBtn.setMaxWidth(Double.MAX_VALUE);
        switchBtn = new Button("Create an account");
        switchBtn.getStyleClass().add("ghost-button");
        switchBtn.setMaxWidth(Double.MAX_VALUE);
        resendBtn = new Button("Resend code");
        resendBtn.getStyleClass().add("secondary-button");
        resendBtn.setMaxWidth(Double.MAX_VALUE);
        resendBtn.setVisible(false);
        resendBtn.setManaged(false);
        messageLabel = new Label();
        messageLabel.getStyleClass().add("muted-label");
        messageLabel.setWrapText(true);

        mainBtn.setOnAction(e -> handleAction());
        switchBtn.setOnAction(e -> switchMode());
        resendBtn.setOnAction(e -> resendVerificationCode());
        card.getChildren().addAll(title, usernameField, emailField, passwordField, verifyCodeField, mainBtn, resendBtn, switchBtn, messageLabel);
        split.getChildren().addAll(hero, new StackPane(card));
        view.getChildren().add(split);
    }

    private Label badge(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("security-badge");
        return label;
    }

    private void switchMode() {
        if (currentMode == 0) {
            currentMode = 1;
            title.setText("Create your account");
            usernameField.setVisible(true);
            usernameField.setManaged(true);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            verifyCodeField.setVisible(false);
            verifyCodeField.setManaged(false);
            emailField.setDisable(false);
            mainBtn.setText("Register");
            resendBtn.setVisible(false);
            resendBtn.setManaged(false);
            switchBtn.setText("Back to login");
        } else {
            currentMode = 0;
            title.setText("Welcome back");
            usernameField.setVisible(false);
            usernameField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            verifyCodeField.setVisible(false);
            verifyCodeField.setManaged(false);
            emailField.setDisable(false);
            mainBtn.setText("Login");
            resendBtn.setVisible(false);
            resendBtn.setManaged(false);
            switchBtn.setText("Create an account");
        }
        clearFeedback();
    }

    private void setVerifyMode(String emailToVerify) {
        currentMode = 2;
        title.setText("Verify your account");
        emailField.setText(emailToVerify);
        emailField.setDisable(true);
        usernameField.setVisible(false);
        usernameField.setManaged(false);
        passwordField.setVisible(false);
        passwordField.setManaged(false);
        verifyCodeField.setVisible(true);
        verifyCodeField.setManaged(true);
        mainBtn.setText("Verify code");
        resendBtn.setVisible(true);
        resendBtn.setManaged(true);
        switchBtn.setText("Back to login");
        messageLabel.setText("Enter the verification code sent to your email.");
        messageLabel.getStyleClass().setAll("success-label");
    }

    @SuppressWarnings("unchecked")
    private void handleAction() {
        if (currentMode == 0) {
            Map<String, String> creds = new HashMap<>();
            creds.put("email", emailField.getText());
            creds.put("password", hashPassword(passwordField.getText()));
            Response resp = clientTCP.sendRequest(new Request("LOGIN", creds));
            if (resp != null && resp.isSuccess()) {
                Map<String, Object> respData = (Map<String, Object>) resp.getData();
                User user = (User) respData.get("user");
                clientTCP.setSessionToken((String) respData.get("token"));
                MainDashboard dashboard = new MainDashboard(clientTCP, stage, user);
                Scene scene = new Scene(dashboard.getView(), 1320, 860);
                attachStyles(scene);
                stage.setScene(scene);
            } else if (resp != null && "NOT_VERIFIED".equals(resp.getData())) {
                setVerifyMode(emailField.getText());
            } else {
                showError(resp != null ? resp.getMessage() : "Connection failed.");
            }
            return;
        }

        if (currentMode == 1) {
            String rawPassword = passwordField.getText();
            if (rawPassword == null || rawPassword.length() < 6
                    || !rawPassword.matches(".*[A-Z].*")
                    || !rawPassword.matches(".*[^a-zA-Z0-9].*")) {
                showError("Password must contain at least 6 characters, one uppercase letter and one special character.");
                return;
            }
            User newUser = new User(usernameField.getText(), emailField.getText(), hashPassword(rawPassword), "CUSTOMER");
            Response resp = clientTCP.sendRequest(new Request("REGISTER", newUser));
            if (resp != null && resp.isSuccess()) {
                setVerifyMode(emailField.getText());
            } else {
                showError(resp != null ? resp.getMessage() : "Registration failed.");
            }
            return;
        }

        Map<String, String> verifData = new HashMap<>();
        verifData.put("email", emailField.getText());
        verifData.put("code", verifyCodeField.getText());
        Response resp = clientTCP.sendRequest(new Request("VERIFY_EMAIL", verifData));
        if (resp != null && resp.isSuccess()) {
            messageLabel.setText("Account verified. You can now log in.");
            messageLabel.getStyleClass().setAll("success-label");
            emailField.setDisable(false);
            currentMode = 0;
            title.setText("Welcome back");
            verifyCodeField.setVisible(false);
            verifyCodeField.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            mainBtn.setText("Login");
            resendBtn.setVisible(false);
            resendBtn.setManaged(false);
            switchBtn.setText("Create an account");
        } else {
            showError(resp != null ? resp.getMessage() : "Verification failed.");
        }
    }

    private void resendVerificationCode() {
        Response resp = clientTCP.sendRequest(new Request("RESEND_VERIFICATION_CODE", emailField.getText()));
        if (resp != null && resp.isSuccess()) {
            messageLabel.setText(resp.getMessage());
            messageLabel.getStyleClass().setAll("success-label");
        } else {
            showError(resp != null ? resp.getMessage() : "Failed to resend verification code.");
        }
    }

    private void attachStyles(Scene scene) {
        String stylesheet = getClass().getResource("/styles/app.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }

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

    private void clearFeedback() {
        usernameField.clear();
        passwordField.clear();
        verifyCodeField.clear();
        messageLabel.setText("");
        messageLabel.getStyleClass().setAll("muted-label");
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().setAll("error-label");
    }

    public VBox getView() {
        return view;
    }
}
