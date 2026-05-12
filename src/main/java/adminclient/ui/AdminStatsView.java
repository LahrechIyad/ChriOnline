package adminclient.ui;

import adminclient.network.AdminClientHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import shared.model.Commande;
import shared.model.DashboardStats;
import shared.model.Produit;
import shared.network.Response;

public class AdminStatsView {
    private final VBox view = new VBox(18);
    private final AdminClientHandler clientHandler;

    public AdminStatsView(AdminClientHandler clientHandler) {
        this.clientHandler = clientHandler;
        buildView();
        loadStats();
    }

    private void buildView() {
        view.setPadding(new Insets(18));
        Label title = new Label("Dashboard statistics");
        title.getStyleClass().add("section-title");
        view.getChildren().add(title);
    }

    private void loadStats() {
        try {
            Response response = clientHandler.sendAdminRequest("ADMIN_GET_DASHBOARD_STATS", null);
            if (response == null || !response.isSuccess()) {
                view.getChildren().add(errorLabel(response != null ? response.getMessage() : "Failed to load dashboard stats."));
                return;
            }

            DashboardStats stats = (DashboardStats) response.getData();
            FlowPane cards = new FlowPane(16, 16);
            cards.getChildren().addAll(
                    statCard("Total products", String.valueOf(stats.getTotalProducts())),
                    statCard("Low stock", String.valueOf(stats.getLowStockProducts())),
                    statCard("Total orders", String.valueOf(stats.getTotalOrders())),
                    statCard("Revenue", String.format("$%.2f", stats.getTotalRevenue()))
            );

            VBox recentOrders = new VBox(10);
            recentOrders.getChildren().add(new Label("Recent orders"));
            for (Commande order : stats.getRecentOrders()) {
                recentOrders.getChildren().add(new Label("#" + order.getId() + " • " + order.getStatus() + " • " + order.getUserEmail()));
            }

            VBox ratedProducts = new VBox(10);
            ratedProducts.getChildren().add(new Label("Best rated electronics"));
            for (Produit produit : stats.getBestRatedProducts()) {
                ratedProducts.getChildren().add(new Label(produit.getDisplayName() + " • " + produit.getRating()));
            }

            view.getChildren().addAll(cards, recentOrders, ratedProducts);
        } catch (Exception e) {
            view.getChildren().add(errorLabel(e.getMessage()));
        }
    }

    private VBox statCard(String title, String value) {
        VBox card = new VBox(6);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(18));
        card.setPrefWidth(220);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("muted-label");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("price-label");
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private Label errorLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("error-label");
        return label;
    }

    public VBox getView() {
        return view;
    }
}
