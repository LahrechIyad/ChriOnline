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

import java.util.HashMap;
import java.util.Map;

public class AuthView {
    private VBox view;
    private ClientTCP clientTCP;
    private Stage stage;
    private boolean isLoginMode = true;

    private TextField usernameField;
    private TextField emailField;
    private PasswordField passwordField;
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

        view.getChildren().addAll(title, usernameField, emailField, passwordField, mainBtn, switchBtn, messageLabel);
    }

    private void switchMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            title.setText("Login to ChriOnline");
            usernameField.setVisible(false);
            usernameField.setManaged(false);
            mainBtn.setText("Login");
            switchBtn.setText("Don't have an account? Register");
        } else {
            title.setText("Create an Account");
            usernameField.setVisible(true);
            usernameField.setManaged(true);
            mainBtn.setText("Register");
            switchBtn.setText("Already have an account? Login");
        }
        messageLabel.setText("");
        emailField.clear();
        passwordField.clear();
        usernameField.clear();
    }

    private void handleAction() {
        if (isLoginMode) {
            Map<String, String> creds = new HashMap<>();
            creds.put("email", emailField.getText());
            creds.put("password", passwordField.getText());

            Response resp = clientTCP.sendRequest(new Request("LOGIN", creds));
            if (resp != null && resp.isSuccess()) {
                User user = (User) resp.getData();   
                MainDashboard dashboard = new MainDashboard(clientTCP, stage, user);
                stage.setScene(new Scene(dashboard.getView(), 900, 600));
            } else {
                showError(resp != null ? resp.getMessage() : "Connection failed.");
            }
        } else {
            User newUser = new User(usernameField.getText(), emailField.getText(), passwordField.getText(), "CUSTOMER");
            Response resp = clientTCP.sendRequest(new Request("REGISTER", newUser));
            if (resp != null && resp.isSuccess()) {
                messageLabel.setText(resp.getMessage() + " You can now login.");
                messageLabel.setStyle("-fx-text-fill: green;");
                switchMode();
            } else {
                showError(resp != null ? resp.getMessage() : "Connection failed.");
            }
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
