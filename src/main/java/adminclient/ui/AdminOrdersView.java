package adminclient.ui;

import adminclient.network.AdminClientHandler;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import shared.model.Commande;
import shared.model.OrderItemDetail;
import shared.network.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminOrdersView {
    private final VBox view = new VBox(14);
    private final AdminClientHandler clientHandler;
    private final TableView<Commande> table = new TableView<>();
    private final ListView<String> detailList = new ListView<>();
    private final Label feedback = new Label();
    private final ComboBox<String> filter = new ComboBox<>();

    public AdminOrdersView(AdminClientHandler clientHandler) {
        this.clientHandler = clientHandler;
        buildView();
        loadOrders("ALL");
    }

    private void buildView() {
        view.setPadding(new Insets(18));
        Label title = new Label("Order management");
        title.getStyleClass().add("section-title");
        feedback.getStyleClass().add("muted-label");

        filter.getItems().addAll("ALL", "PENDING", "VALIDATED", "SHIPPED", "CANCELLED");
        filter.setValue("ALL");
        ComboBox<String> statusUpdate = new ComboBox<>();
        statusUpdate.getItems().addAll("PENDING", "VALIDATED", "SHIPPED", "CANCELLED");
        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("secondary-button");
        Button details = new Button("Details");
        details.getStyleClass().add("secondary-button");
        Button update = new Button("Update status");
        update.getStyleClass().add("primary-button");
        HBox actions = new HBox(10, filter, statusUpdate, refresh, details, update);

        TableColumn<Commande, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Commande, String> userCol = new TableColumn<>("Customer");
        userCol.setCellValueFactory(new PropertyValueFactory<>("userEmail"));
        userCol.setPrefWidth(220);
        TableColumn<Commande, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        TableColumn<Commande, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        table.getColumns().addAll(idCol, userCol, totalCol, statusCol);

        refresh.setOnAction(e -> loadOrders(filter.getValue()));
        details.setOnAction(e -> showDetails());
        update.setOnAction(e -> updateStatus(statusUpdate.getValue()));

        view.getChildren().addAll(title, actions, table, new Label("Order details"), detailList, feedback);
    }

    @SuppressWarnings("unchecked")
    private void loadOrders(String status) {
        try {
            Response response = clientHandler.sendAdminRequest("ADMIN_GET_ORDERS", status);
            if (response != null && response.isSuccess()) {
                table.setItems(FXCollections.observableArrayList((List<Commande>) response.getData()));
            } else {
                feedback.setText(response != null ? response.getMessage() : "Failed to load orders.");
                feedback.getStyleClass().setAll("error-label");
            }
        } catch (Exception e) {
            feedback.setText(e.getMessage());
            feedback.getStyleClass().setAll("error-label");
        }
    }

    @SuppressWarnings("unchecked")
    private void showDetails() {
        Commande selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            feedback.setText("Select an order first.");
            feedback.getStyleClass().setAll("error-label");
            return;
        }
        try {
            Response response = clientHandler.sendAdminRequest("ADMIN_GET_ORDER_DETAILS", selected.getId());
            if (response != null && response.isSuccess()) {
                List<OrderItemDetail> details = (List<OrderItemDetail>) response.getData();
                detailList.getItems().clear();
                for (OrderItemDetail detail : details) {
                    detailList.getItems().add(detail.getProductName() + " • qty " + detail.getQuantity() + " • $" + detail.getUnitPrice());
                }
            }
        } catch (Exception e) {
            feedback.setText(e.getMessage());
            feedback.getStyleClass().setAll("error-label");
        }
    }

    private void updateStatus(String status) {
        Commande selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || status == null) {
            feedback.setText("Select an order and a target status.");
            feedback.getStyleClass().setAll("error-label");
            return;
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("orderId", selected.getId());
            payload.put("status", status);
            Response response = clientHandler.sendAdminRequest("ADMIN_UPDATE_ORDER_STATUS", payload);
            feedback.setText(response != null ? response.getMessage() : "Failed to update order status.");
            feedback.getStyleClass().setAll(response != null && response.isSuccess() ? "success-label" : "error-label");
            if (response != null && response.isSuccess()) {
                loadOrders(filter.getValue());
            }
        } catch (Exception e) {
            feedback.setText(e.getMessage());
            feedback.getStyleClass().setAll("error-label");
        }
    }

    public VBox getView() {
        return view;
    }
}
