package client.ui;

import client.network.ClientTCP;
import shared.model.Produit;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductView {
    private VBox view;
    private ClientTCP clientTCP;
    private User currentUser;
    private TableView<Produit> table;
    private Label messageLabel;

    public ProductView(ClientTCP clientTCP, User user) {
        this.clientTCP = clientTCP;
        this.currentUser = user;
        buildView();
        loadData();
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        view = new VBox(15);
        view.setPadding(new Insets(20));

        Label title = new Label("Product Catalog");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        table = new TableView<>();
        
        TableColumn<Produit, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<Produit, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);

        TableColumn<Produit, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(100);

        TableColumn<Produit, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        stockCol.setPrefWidth(100);

        table.getColumns().addAll(nameCol, descCol, priceCol, stockCol);

        HBox actionsBox = new HBox(10);
        TextField quantityField = new TextField("1");
        quantityField.setPrefWidth(50);
        Button addBtn = new Button("Add to Cart");
        addBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        
        messageLabel = new Label();
        
        addBtn.setOnAction(e -> {
            Produit selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showMessage("Please select a product.", false);
                return;
            }
            try {
                int qty = Integer.parseInt(quantityField.getText());
                if (qty <= 0) throw new NumberFormatException();

                Map<String, Integer> cartData = new HashMap<>();
                cartData.put("productId", selected.getId());
                cartData.put("quantity", qty);

                Response resp = clientTCP.sendRequest(new Request("ADD_TO_CART", cartData));
                showMessage(resp != null ? resp.getMessage() : "Error connecting to server", resp != null && resp.isSuccess());
                
                if(resp != null && resp.isSuccess()) {
                    loadData(); // refresh stock
                }
            } catch (NumberFormatException ex) {
                showMessage("Invalid quantity.", false);
            }
        });

        actionsBox.getChildren().addAll(new Label("Qty:"), quantityField, addBtn);
        
        view.getChildren().addAll(title, table, actionsBox, messageLabel);
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        Response resp = clientTCP.sendRequest(new Request("GET_PRODUCTS", null));
        if (resp != null && resp.isSuccess()) {
            List<Produit> products = (List<Produit>) resp.getData();
            ObservableList<Produit> data = FXCollections.observableArrayList(products);
            table.setItems(data);
        } else {
            showMessage("Failed to load products.", false);
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
