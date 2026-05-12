package client.ui;

import client.network.ClientTCP;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import shared.model.LignePanier;
import shared.model.Panier;
import shared.model.User;
import shared.network.Request;
import shared.network.Response;

public class CartView {
    private final BorderPane view = new BorderPane();
    private final ClientTCP clientTCP;
    private final VBox itemsBox = new VBox(14);
    private final Label totalLabel = new Label("$0.00");
    private final Label discountLabel = new Label();
    private final Label feedback = new Label();

    public CartView(ClientTCP clientTCP, User user) {
        this.clientTCP = clientTCP;
        buildView();
        loadData();
    }

    private void buildView() {
        view.setPadding(new Insets(18));
        HBox content = new HBox(20);

        ScrollPane scrollPane = new ScrollPane(itemsBox);
        scrollPane.setFitToWidth(true);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        VBox summary = new VBox(14);
        summary.getStyleClass().add("glass-card");
        summary.setPadding(new Insets(22));
        summary.setPrefWidth(300);
        Label title = new Label("Order summary");
        title.getStyleClass().add("section-title");
        totalLabel.getStyleClass().add("price-label");
        discountLabel.getStyleClass().add("muted-label");
        feedback.getStyleClass().add("muted-label");
        Button checkout = new Button("Validate order");
        checkout.getStyleClass().add("primary-button");
        checkout.setMaxWidth(Double.MAX_VALUE);
        checkout.setOnAction(e -> validateOrder());
        summary.getChildren().addAll(title, totalLabel, discountLabel, checkout, feedback);

        content.getChildren().addAll(scrollPane, summary);
        view.setCenter(content);
    }

    private void loadData() {
        Response resp = clientTCP.sendRequest(new Request("VIEW_CART", null));
        if (resp != null && resp.isSuccess()) {
            renderCart((Panier) resp.getData());
        } else {
            feedback.setText(resp != null ? resp.getMessage() : "Failed to load cart.");
            feedback.getStyleClass().setAll("error-label");
        }
    }

    private void renderCart(Panier cart) {
        itemsBox.getChildren().clear();
        if (cart.getItems().isEmpty()) {
            Label empty = new Label("Your cart is empty.");
            empty.getStyleClass().add("muted-label");
            itemsBox.getChildren().add(empty);
        }
        for (LignePanier item : cart.getItems()) {
            itemsBox.getChildren().add(itemCard(item));
        }
        double subtotal = cart.getTotal();
        double estimatedDiscount = cart.getItems().stream()
                .mapToDouble(line -> (line.getProduct().getPrixUsd() - line.getProduct().getFinalPrice()) * line.getQuantity())
                .sum();
        totalLabel.setText(String.format("Total: $%.2f", subtotal));
        discountLabel.setText(String.format("$%.2f estimated discount", estimatedDiscount));
    }

    private VBox itemCard(LignePanier item) {
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setPadding(new Insets(16));
        Label name = new Label(item.getProduct().getDisplayName());
        name.getStyleClass().add("product-title");
        Label meta = new Label(item.getQuantity() + " x $" + String.format("%.2f", item.getProduct().getFinalPrice()));
        meta.getStyleClass().add("muted-label");
        Label subtotal = new Label(String.format("Subtotal: $%.2f", item.getSubTotal()));
        subtotal.getStyleClass().add("price-label");
        Button remove = new Button("Remove");
        remove.getStyleClass().add("secondary-button");
        remove.setOnAction(e -> removeItem(item.getProduct().getId()));
        card.getChildren().addAll(name, meta, subtotal, remove);
        return card;
    }

    private void removeItem(int productId) {
        Response resp = clientTCP.sendRequest(new Request("REMOVE_FROM_CART", productId));
        feedback.setText(resp != null ? resp.getMessage() : "Failed to remove item.");
        feedback.getStyleClass().setAll(resp != null && resp.isSuccess() ? "success-label" : "error-label");
        if (resp != null && resp.isSuccess()) {
            loadData();
        }
    }

    private void validateOrder() {
        Response resp = clientTCP.sendRequest(new Request("CREATE_ORDER", null));
        feedback.setText(resp != null ? resp.getMessage() : "Order validation failed.");
        feedback.getStyleClass().setAll(resp != null && resp.isSuccess() ? "success-label" : "error-label");
        if (resp != null && resp.isSuccess()) {
            loadData();
        }
    }

    public BorderPane getView() {
        return view;
    }
}
