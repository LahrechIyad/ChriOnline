package adminclient.ui;

import adminclient.network.AdminClientHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AdminDashboardView {
    private final BorderPane view = new BorderPane();
    private final AdminClientHandler clientHandler;
    private final Stage stage;

    public AdminDashboardView(AdminClientHandler clientHandler, Stage stage) {
        this.clientHandler = clientHandler;
        this.stage = stage;
        buildView();
    }

    private void buildView() {
        view.setPadding(new Insets(18));
        VBox sidebar = new VBox(14);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(22));
        sidebar.setPrefWidth(240);
        Label brand = new Label("ChriOnline Admin");
        brand.getStyleClass().add("sidebar-brand");
        Label state = new Label("RSA auth + AES session");
        state.getStyleClass().add("sidebar-subtitle");
        Button stats = navButton("Dashboard");
        Button products = navButton("Products");
        Button orders = navButton("Orders");
        Button logout = navButton("Logout");
        sidebar.getChildren().addAll(brand, state, stats, products, orders, logout);
        view.setLeft(sidebar);
        view.setTop(topBar());
        view.setCenter(new StackPane(new AdminStatsView(clientHandler).getView()));

        stats.setOnAction(e -> view.setCenter(new StackPane(new AdminStatsView(clientHandler).getView())));
        products.setOnAction(e -> view.setCenter(new StackPane(new AdminProductsView(clientHandler).getView())));
        orders.setOnAction(e -> view.setCenter(new StackPane(new AdminOrdersView(clientHandler).getView())));
        logout.setOnAction(e -> {
            try {
                clientHandler.sendAdminRequest("LOGOUT", null);
            } catch (Exception ignored) {
            }
            AdminAuthView authView = new AdminAuthView(clientHandler, stage);
            Scene scene = new Scene(authView.getView(), 1220, 820);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            stage.setScene(scene);
        });
    }

    private StackPane topBar() {
        Label title = new Label("Professional electronics administration");
        title.getStyleClass().add("topbar-title");
        StackPane pane = new StackPane(title);
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
