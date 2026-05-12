package client.ui;

import client.network.ClientTCP;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import shared.model.User;
import shared.network.Request;

public class MainDashboard {
    private final BorderPane view = new BorderPane();
    private final ClientTCP clientTCP;
    private final Stage stage;
    private final User currentUser;

    public MainDashboard(ClientTCP clientTCP, Stage stage, User user) {
        this.clientTCP = clientTCP;
        this.stage = stage;
        this.currentUser = user;
        buildView();
    }

    private void buildView() {
        view.getStyleClass().add("app-shell");
        view.setPadding(new Insets(18));

        VBox sidebar = new VBox(16);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(22));
        sidebar.setPrefWidth(220);

        Label brand = new Label("ChriOnline Tech");
        brand.getStyleClass().add("sidebar-brand");
        Label greeting = new Label("Hello, " + currentUser.getUsername());
        greeting.getStyleClass().add("sidebar-subtitle");

        Button storeBtn = navButton("Store");
        Button cartBtn = navButton("Cart");
        Button ordersBtn = navButton("Orders");
        Button logoutBtn = navButton("Logout");

        sidebar.getChildren().addAll(brand, greeting, storeBtn, cartBtn, ordersBtn, logoutBtn);
        view.setLeft(sidebar);
        view.setTop(topBar());
        view.setCenter(new StackPane(new ProductView(clientTCP, currentUser).getView()));

        storeBtn.setOnAction(e -> view.setCenter(new ProductView(clientTCP, currentUser).getView()));
        cartBtn.setOnAction(e -> view.setCenter(new CartView(clientTCP, currentUser).getView()));
        ordersBtn.setOnAction(e -> view.setCenter(new OrderView(clientTCP, currentUser).getView()));
        logoutBtn.setOnAction(e -> {
            clientTCP.sendRequest(new Request("LOGOUT", null));
            clientTCP.setSessionToken(null);
            AuthView authView = new AuthView(clientTCP, stage);
            Scene scene = new Scene(authView.getView(), 1180, 780);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            stage.setScene(scene);
        });
    }

    private StackPane topBar() {
        Label status = new Label("AES/RSA secured session active");
        status.getStyleClass().add("topbar-title");
        StackPane pane = new StackPane(status);
        pane.getStyleClass().add("topbar");
        pane.setPadding(new Insets(18, 24, 18, 24));
        return pane;
    }

    private Button navButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    public BorderPane getView() {
        return view;
    }
}
