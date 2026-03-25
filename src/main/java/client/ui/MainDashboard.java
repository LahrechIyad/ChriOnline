package client.ui;

import client.network.ClientTCP;
import shared.model.User;
import shared.network.Request;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainDashboard {
    private BorderPane view;
    private ClientTCP clientTCP;
    private Stage stage;
    private User currentUser;

    public MainDashboard(ClientTCP clientTCP, Stage stage, User user) {
        this.clientTCP = clientTCP;
        this.stage = stage;
        this.currentUser = user;
        buildView();
    }

    private void buildView() {
        view = new BorderPane();

        // Sidebar
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: #2b2b2b;");
        sidebar.setPrefWidth(200);

        Label welcomeLabel = new Label("Hi, " + currentUser.getUsername());
        welcomeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Button productsBtn = createMenuButton("Catalog");
        Button cartBtn = createMenuButton("My Cart");
        Button ordersBtn = createMenuButton("My Orders");
        Button logoutBtn = createMenuButton("Logout");

        sidebar.getChildren().addAll(welcomeLabel, productsBtn, cartBtn, ordersBtn, logoutBtn);
        view.setLeft(sidebar);

        // Initial default view
        view.setCenter(new ProductView(clientTCP, currentUser).getView());

        productsBtn.setOnAction(e -> view.setCenter(new ProductView(clientTCP, currentUser).getView()));
        cartBtn.setOnAction(e -> view.setCenter(new CartView(clientTCP, currentUser).getView()));
        ordersBtn.setOnAction(e -> view.setCenter(new OrderView(clientTCP, currentUser).getView()));

        logoutBtn.setOnAction(e -> {
            clientTCP.sendRequest(new Request("LOGOUT", null));
            AuthView authView = new AuthView(clientTCP, stage);
            stage.setScene(new Scene(authView.getView(), 800, 600));
        });
    }

    private Button createMenuButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #555555; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #3b3b3b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px;"));
        return btn;
    }

    public BorderPane getView() {
        return view;
    }
}
