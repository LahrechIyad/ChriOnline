package client.ui;

import client.network.ClientTCP;
import shared.model.Panier;
import shared.model.LignePanier;
import shared.model.User;
import shared.network.Request;
import shared.network.Response;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class CartView {
    private VBox view;
    private ClientTCP clientTCP;
    private User currentUser;
    private TableView<LignePanier> table;
    private Label totalLabel;
    private Label messageLabel;

    public CartView(ClientTCP clientTCP, User user) {
        this.clientTCP = clientTCP;
        this.currentUser = user;
        buildView();
        loadData();
    }

    private void buildView() {
        view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("Shopping Cart");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        table = new TableView<>();

        TableColumn<LignePanier, String> nameCol = new TableColumn<>("Product");
        nameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getProduct().getName()));
        nameCol.setPrefWidth(200);

        TableColumn<LignePanier, Double> priceCol = new TableColumn<>("Unit Price ($)");
        priceCol.setCellValueFactory(
                cellData -> new SimpleDoubleProperty(cellData.getValue().getProduct().getPrice()).asObject());
        priceCol.setPrefWidth(120);

        TableColumn<LignePanier, Integer> qtyCol = new TableColumn<>("Quantity");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setPrefWidth(100);

        TableColumn<LignePanier, Double> subCol = new TableColumn<>("Subtotal ($)");
        subCol.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getSubTotal()).asObject());
        subCol.setPrefWidth(120);

        TableColumn<LignePanier, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(100);
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btn = new Button("Remove");
            {
                btn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
                btn.setOnAction(e -> {
                    LignePanier item = getTableView().getItems().get(getIndex());
                    Response resp = clientTCP.sendRequest(new Request("REMOVE_FROM_CART", item.getProduct().getId()));
                    if (resp != null && resp.isSuccess()) {
                        showMessage("Removed " + item.getProduct().getName() + " from cart.", true);
                        loadData();
                    } else {
                        showMessage(resp != null ? resp.getMessage() : "Failed to remove item.", false);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });

        table.getColumns().addAll(nameCol, priceCol, qtyCol, subCol, actionCol);

        totalLabel = new Label("Total: $0.00");
        totalLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button checkoutBtn = new Button("Validate Order");
        checkoutBtn.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black; -fx-font-weight: bold;");

        messageLabel = new Label();

        checkoutBtn.setOnAction(e -> {
            Response resp = clientTCP.sendRequest(new Request("CREATE_ORDER", null));
            if (resp != null && resp.isSuccess()) {
                showMessage("Order validated successfully. Go to My Orders to pay.", true);
                loadData();
            } else {
                showMessage(resp != null ? resp.getMessage() : "Validation failed.", false);
            }
        });

        HBox bottomBox = new HBox(20);
        bottomBox.getChildren().addAll(totalLabel, checkoutBtn);

        view.getChildren().addAll(title, table, bottomBox, messageLabel);
    }

    private void loadData() {
        Response resp = clientTCP.sendRequest(new Request("VIEW_CART", null));
        if (resp != null && resp.isSuccess()) {
            Panier cart = (Panier) resp.getData();
            ObservableList<LignePanier> data = FXCollections.observableArrayList(cart.getItems());
            table.setItems(data);
            totalLabel.setText(String.format("Total: $%.2f", cart.getTotal()));
        } else {
            showMessage("Failed to load cart.", false);
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
