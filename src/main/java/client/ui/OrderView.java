package client.ui;

import client.network.ClientTCP;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import shared.model.Commande;
import shared.model.User;
import shared.network.Request;
import shared.network.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderView {
    private final VBox view = new VBox(16);
    private final ClientTCP clientTCP;
    private final VBox ordersBox = new VBox(14);
    private final Label feedback = new Label();

    public OrderView(ClientTCP clientTCP, User user) {
        this.clientTCP = clientTCP;
        buildView();
        loadData();
    }

    private void buildView() {
        view.setPadding(new Insets(18));
        Label title = new Label("My orders");
        title.getStyleClass().add("section-title");
        feedback.getStyleClass().add("muted-label");
        view.getChildren().addAll(title, feedback, ordersBox);
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        Response resp = clientTCP.sendRequest(new Request("GET_MY_ORDERS", null));
        ordersBox.getChildren().clear();
        if (resp != null && resp.isSuccess()) {
            List<Commande> orders = (List<Commande>) resp.getData();
            if (orders.isEmpty()) {
                feedback.setText("No orders yet.");
                feedback.getStyleClass().setAll("muted-label");
                return;
            }
            for (Commande order : orders) {
                ordersBox.getChildren().add(orderCard(order));
            }
        } else {
            feedback.setText(resp != null ? resp.getMessage() : "Failed to load orders.");
            feedback.getStyleClass().setAll("error-label");
        }
    }

    private VBox orderCard(Commande order) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card");
        card.setPadding(new Insets(18));
        Label head = new Label("Order #" + order.getId());
        head.getStyleClass().add("product-title");
        Label meta = new Label(order.getOrderDate() + " • $" + String.format("%.2f", order.getTotal()));
        meta.getStyleClass().add("muted-label");
        Label status = new Label(order.getStatus());
        status.getStyleClass().add(statusClass(order.getStatus()));
        card.getChildren().addAll(head, meta, status);

        if ("PENDING".equalsIgnoreCase(order.getStatus())) {
            ComboBox<String> method = new ComboBox<>();
            method.getItems().addAll("CREDIT_CARD", "PAYPAL");
            method.setValue("CREDIT_CARD");
            Button pay = new Button("Simulate payment");
            pay.getStyleClass().add("primary-button");
            pay.setOnAction(e -> processPayment(order, method.getValue()));
            card.getChildren().addAll(method, pay);
        }
        return card;
    }

    private void processPayment(Commande order, String method) {
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("orderId", order.getId());
        paymentData.put("amount", order.getTotal());
        paymentData.put("method", method);
        if ("CREDIT_CARD".equals(method)) {
            Map<String, String> cardInfo = showCreditCardDialog();
            if (cardInfo == null) {
                return;
            }
            paymentData.put("maskedCard", maskCard(cardInfo.get("cardNumber")));
            paymentData.put("deliveryAddress", cardInfo.get("deliveryAddress"));
        }
        Response resp = clientTCP.sendRequest(new Request("PROCESS_PAYMENT", paymentData));
        feedback.setText(resp != null ? resp.getMessage() : "Payment failed.");
        feedback.getStyleClass().setAll(resp != null && resp.isSuccess() ? "success-label" : "error-label");
        loadData();
    }

    private Map<String, String> showCreditCardDialog() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Secure payment");
        dialog.setHeaderText("Card details are used for simulation only. CVV is never stored.");
        ButtonType confirm = new ButtonType("Pay", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, ButtonType.CANCEL);

        TextField cardNumber = new TextField();
        cardNumber.setPromptText("1234 5678 9012 3456");
        TextField cardHolder = new TextField();
        cardHolder.setPromptText("Cardholder name");
        TextField expiry = new TextField();
        expiry.setPromptText("MM/YY");
        PasswordField cvv = new PasswordField();
        cvv.setPromptText("CVV");
        TextArea address = new TextArea();
        address.setPromptText("Delivery address");
        address.setPrefRowCount(3);

        VBox content = new VBox(12,
                labeledField("Card number", cardNumber),
                labeledField("Cardholder", cardHolder),
                labeledField("Expiry", expiry),
                labeledField("CVV", cvv),
                labeledField("Delivery address", address)
        );
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(button -> {
            if (button == confirm) {
                Map<String, String> data = new HashMap<>();
                data.put("cardNumber", cardNumber.getText().trim());
                data.put("deliveryAddress", address.getText().trim());
                return data;
            }
            return null;
        });
        return dialog.showAndWait().orElse(null);
    }

    private VBox labeledField(String labelText, javafx.scene.Node node) {
        Label label = new Label(labelText);
        label.getStyleClass().add("muted-label");
        return new VBox(5, label, node);
    }

    private String maskCard(String cardNumber) {
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\s+", "");
        if (digits.length() < 4) {
            return "****";
        }
        return "**** **** **** " + digits.substring(digits.length() - 4);
    }

    private String statusClass(String status) {
        return switch (status.toUpperCase()) {
            case "VALIDATED", "PAID", "SHIPPED" -> "success-badge";
            case "PENDING" -> "warning-badge";
            default -> "danger-badge";
        };
    }

    public VBox getView() {
        return view;
    }
}
