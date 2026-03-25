package client.ui;

import client.network.ClientTCP;
import shared.model.Commande;
import shared.model.User;
import shared.network.Request;
import shared.network.Response;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderView {
    private VBox view;
    private ClientTCP clientTCP;
    private User currentUser;
    private TableView<Commande> table;
    private Label messageLabel;

    public OrderView(ClientTCP clientTCP, User user) {
        this.clientTCP = clientTCP;
        this.currentUser = user;
        buildView();
        loadData();
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("My Orders");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        table = new TableView<>();

        TableColumn<Commande, Integer> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(80);

        TableColumn<Commande, Date> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        dateCol.setPrefWidth(180);

        TableColumn<Commande, Double> totalCol = new TableColumn<>("Total ($)");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        totalCol.setPrefWidth(100);

        TableColumn<Commande, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(120);

        table.getColumns().addAll(idCol, dateCol, totalCol, statusCol);

        HBox actionsBox = new HBox(10);
        ComboBox<String> methodCombo = new ComboBox<>();
        methodCombo.getItems().addAll("CREDIT_CARD", "PAYPAL");
        methodCombo.setValue("CREDIT_CARD");

        Button payBtn = new Button("Pay Selected Order");
        payBtn.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");

        messageLabel = new Label();

        payBtn.setOnAction(e -> {
            Commande selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showMessage("Please select an order to pay.", false);
                return;
            }
            if (!"PENDING".equals(selected.getStatus())) {
                showMessage("Order is already paid.", false);
                return;
            }

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("orderId", selected.getId());
            paymentData.put("amount",  selected.getTotal());
            paymentData.put("method",  methodCombo.getValue());

            // If CREDIT_CARD is selected, show the card + address dialog first
            if ("CREDIT_CARD".equals(methodCombo.getValue())) {
                Map<String, String> cardInfo = showCreditCardDialog();
                if (cardInfo == null) {
                    // User cancelled – abort payment
                    return;
                }
                paymentData.put("cardNumber",      cardInfo.get("cardNumber"));
                paymentData.put("cardHolder",      cardInfo.get("cardHolder"));
                paymentData.put("expiry",          cardInfo.get("expiry"));
                paymentData.put("cvv",             cardInfo.get("cvv"));
                paymentData.put("deliveryAddress", cardInfo.get("deliveryAddress"));
            }

            Response resp = clientTCP.sendRequest(new Request("PROCESS_PAYMENT", paymentData));
            showMessage(resp != null ? resp.getMessage() : "Payment failed.", resp != null && resp.isSuccess());
            loadData();
        });

        actionsBox.getChildren().addAll(new Label("Payment Method:"), methodCombo, payBtn);

        view.getChildren().addAll(title, table, actionsBox, messageLabel);
    }

    /**
     * Displays a dialog collecting credit card info and delivery address.
     * Returns a Map of the entered values, or null if the user cancelled.
     */
    private Map<String, String> showCreditCardDialog() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Credit Card Payment");
        dialog.setHeaderText("Enter your payment & delivery details");

        ButtonType confirmBtn = new ButtonType("Confirm Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmBtn, ButtonType.CANCEL);

        // --- Form fields ---
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("1234 5678 9012 3456");

        TextField cardHolderField = new TextField();
        cardHolderField.setPromptText("Full name as it appears on card");

        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");
        expiryField.setPrefWidth(90);

        PasswordField cvvField = new PasswordField();
        cvvField.setPromptText("CVV");
        cvvField.setPrefWidth(80);

        TextArea deliveryAddressArea = new TextArea();
        deliveryAddressArea.setPromptText("Street, City, Postal Code, Country");
        deliveryAddressArea.setPrefRowCount(3);
        deliveryAddressArea.setWrapText(true);

        // --- Layout ---
        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 20, 10, 20));
        content.setPrefWidth(440);

        HBox expiryRow = new HBox(15,
            section("📅  Expiry (MM/YY)", expiryField),
            section("🔒  CVV", cvvField)
        );

        content.getChildren().addAll(
            section("💳  Card Number",      cardNumberField),
            section("👤  Cardholder Name",  cardHolderField),
            expiryRow,
            section("🏠  Delivery Address", deliveryAddressArea)
        );

        dialog.getDialogPane().setContent(content);

        // Disable "Confirm" until all fields are non-empty
        javafx.scene.Node confirmNode = dialog.getDialogPane().lookupButton(confirmBtn);
        confirmNode.setDisable(true);

        Runnable validate = () -> {
            boolean ready = !cardNumberField.getText().trim().isEmpty()
                    && !cardHolderField.getText().trim().isEmpty()
                    && !expiryField.getText().trim().isEmpty()
                    && !cvvField.getText().trim().isEmpty()
                    && !deliveryAddressArea.getText().trim().isEmpty();
            confirmNode.setDisable(!ready);
        };

        cardNumberField.textProperty().addListener((o, p, n)      -> validate.run());
        cardHolderField.textProperty().addListener((o, p, n)      -> validate.run());
        expiryField.textProperty().addListener((o, p, n)          -> validate.run());
        cvvField.textProperty().addListener((o, p, n)             -> validate.run());
        deliveryAddressArea.textProperty().addListener((o, p, n)  -> validate.run());

        dialog.setResultConverter(btn -> {
            if (btn == confirmBtn) {
                Map<String, String> result = new HashMap<>();
                result.put("cardNumber",      cardNumberField.getText().trim());
                result.put("cardHolder",      cardHolderField.getText().trim());
                result.put("expiry",          expiryField.getText().trim());
                result.put("cvv",             cvvField.getText().trim());
                result.put("deliveryAddress", deliveryAddressArea.getText().trim());
                return result;
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    /** Wraps a label + field into a small VBox section. */
    private VBox section(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        VBox box = new VBox(4, lbl, field);
        if (field instanceof Control) {
            ((Control) field).setMaxWidth(Double.MAX_VALUE);
        }
        return box;
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        Response resp = clientTCP.sendRequest(new Request("GET_MY_ORDERS", null));
        if (resp != null && resp.isSuccess()) {
            List<Commande> orders = (List<Commande>) resp.getData();
            ObservableList<Commande> data = FXCollections.observableArrayList(orders);
            table.setItems(data);
        } else {
            showMessage("Failed to load orders.", false);
        }
    }

    private void showMessage(String msg, boolean success) {
        messageLabel.setText(msg);
        messageLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
    }

    public VBox getView() {
        return view;
    }
}
