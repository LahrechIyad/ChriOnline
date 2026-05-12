package adminclient.ui;

import adminclient.network.AdminClientHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;

public class AdminAuthView {
    private final VBox view = new VBox(14);
    private final AdminClientHandler clientHandler;
    private final Stage stage;
    private final Label feedback = new Label();

    public AdminAuthView(AdminClientHandler clientHandler, Stage stage) {
        this.clientHandler = clientHandler;
        this.stage = stage;
        buildView();
    }

    private void buildView() {
        view.setPadding(new Insets(40));
        view.getStyleClass().add("auth-root");

        VBox card = new VBox(12);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(420);

        Label title = new Label("Admin RSA Authentication");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Authenticate with keystore-based challenge-response.");
        subtitle.getStyleClass().add("muted-label");

        TextField emailField = new TextField();
        emailField.setPromptText("Admin email");
        TextField pathField = new TextField("admin_keystore.p12");
        pathField.setPromptText("PKCS12 keystore path");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Keystore password");
        TextField aliasField = new TextField("admin");
        aliasField.setPromptText("Alias");
        Button authButton = new Button("Authenticate");
        authButton.getStyleClass().add("primary-button");
        authButton.setMaxWidth(Double.MAX_VALUE);
        feedback.getStyleClass().add("muted-label");

        authButton.setOnAction(e -> authenticate(emailField.getText(), pathField.getText(), passwordField.getText(), aliasField.getText()));
        card.getChildren().addAll(title, subtitle, emailField, pathField, passwordField, aliasField, authButton, feedback);
        view.getChildren().add(card);
    }

    private void authenticate(String email, String keystorePath, String password, String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(keystorePath)) {
                keyStore.load(fis, password.toCharArray());
            }
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            if (privateKey == null) {
                feedback.setText("Private key not found for alias " + alias);
                feedback.getStyleClass().setAll("error-label");
                return;
            }

            boolean success = clientHandler.authenticateAdmin(email, privateKey);
            if (!success) {
                feedback.setText("Admin authentication failed.");
                feedback.getStyleClass().setAll("error-label");
                return;
            }

            AdminDashboardView dashboardView = new AdminDashboardView(clientHandler, stage);
            Scene scene = new Scene(dashboardView.getView(), 1380, 860);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception ex) {
            feedback.setText("Keystore error: " + ex.getMessage());
            feedback.getStyleClass().setAll("error-label");
        }
    }

    public VBox getView() {
        return view;
    }
}
