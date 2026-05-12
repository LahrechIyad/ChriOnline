package adminclient.ui;

import adminclient.network.AdminClientHandler;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import shared.model.Produit;
import shared.network.Response;

import java.util.List;

public class AdminProductsView {
    private final VBox view = new VBox(14);
    private final AdminClientHandler clientHandler;
    private final TableView<Produit> table = new TableView<>();
    private final Label feedback = new Label();
    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryFilter = new ComboBox<>();

    public AdminProductsView(AdminClientHandler clientHandler) {
        this.clientHandler = clientHandler;
        buildView();
        loadProducts();
    }

    private void buildView() {
        view.setPadding(new Insets(18));
        Label title = new Label("Product management");
        title.getStyleClass().add("section-title");
        feedback.getStyleClass().add("muted-label");

        searchField.setPromptText("Search by name, brand or category");
        categoryFilter.getItems().addAll("All", "smartphones", "laptops", "tablets", "mobile-accessories");
        categoryFilter.setValue("All");
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("secondary-button");
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("secondary-button");
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("secondary-button");
        HBox actions = new HBox(10, searchField, categoryFilter, searchBtn, addBtn, editBtn, deleteBtn);

        TableColumn<Produit, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        TableColumn<Produit, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("nomProduit"));
        nameCol.setPrefWidth(260);
        TableColumn<Produit, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("marque"));
        TableColumn<Produit, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("categorieSource"));
        TableColumn<Produit, Double> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("prixNetUsd"));
        TableColumn<Produit, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stock"));
        TableColumn<Produit, String> availCol = new TableColumn<>("Availability");
        availCol.setCellValueFactory(new PropertyValueFactory<>("disponibilite"));
        table.getColumns().addAll(idCol, nameCol, brandCol, catCol, priceCol, stockCol, availCol);

        searchBtn.setOnAction(e -> searchOrFilter());
        addBtn.setOnAction(e -> createOrUpdate(null));
        editBtn.setOnAction(e -> createOrUpdate(table.getSelectionModel().getSelectedItem()));
        deleteBtn.setOnAction(e -> deleteSelected());

        view.getChildren().addAll(title, actions, table, feedback);
    }

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        try {
            Response response = clientHandler.sendAdminRequest("ADMIN_GET_PRODUCTS", null);
            if (response != null && response.isSuccess()) {
                table.setItems(FXCollections.observableArrayList((List<Produit>) response.getData()));
            } else {
                feedback.setText(response != null ? response.getMessage() : "Failed to load products.");
                feedback.getStyleClass().setAll("error-label");
            }
        } catch (Exception e) {
            feedback.setText(e.getMessage());
            feedback.getStyleClass().setAll("error-label");
        }
    }

    @SuppressWarnings("unchecked")
    private void searchOrFilter() {
        try {
            Response response;
            if (!searchField.getText().isBlank()) {
                response = clientHandler.sendAdminRequest("SEARCH_PRODUCTS", searchField.getText());
            } else if (!"All".equalsIgnoreCase(categoryFilter.getValue())) {
                response = clientHandler.sendAdminRequest("FILTER_PRODUCTS_BY_CATEGORY", categoryFilter.getValue());
            } else {
                response = clientHandler.sendAdminRequest("ADMIN_GET_PRODUCTS", null);
            }
            if (response != null && response.isSuccess()) {
                table.setItems(FXCollections.observableArrayList((List<Produit>) response.getData()));
            }
        } catch (Exception e) {
            feedback.setText(e.getMessage());
            feedback.getStyleClass().setAll("error-label");
        }
    }

    private void createOrUpdate(Produit selected) {
        Dialog<Produit> dialog = new Dialog<>();
        dialog.setTitle(selected == null ? "Create product" : "Edit product");
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        TextField sku = new TextField(selected == null ? "" : selected.getSku());
        TextField name = new TextField(selected == null ? "" : selected.getNomProduit());
        TextField brand = new TextField(selected == null ? "" : selected.getMarque());
        ComboBox<String> category = new ComboBox<>();
        category.getItems().addAll("smartphones", "laptops", "tablets", "mobile-accessories");
        category.setValue(selected == null ? "smartphones" : selected.getCategorieSource());
        TextField price = new TextField(selected == null ? "" : String.valueOf(selected.getFinalPrice()));
        TextField listPrice = new TextField(selected == null ? "" : String.valueOf(selected.getPrixUsd()));
        TextField discount = new TextField(selected == null ? "0" : String.valueOf(selected.getRemisePct()));
        TextField rating = new TextField(selected == null ? "4.0" : String.valueOf(selected.getRating()));
        TextField stock = new TextField(selected == null ? "0" : String.valueOf(selected.getStock()));
        TextField availability = new TextField(selected == null ? "In Stock" : selected.getDisponibilite());
        TextField image = new TextField(selected == null ? "" : selected.getImagePrincipale());
        TextField description = new TextField(selected == null ? "" : selected.getDescription());

        VBox form = new VBox(10,
                labeled("SKU", sku), labeled("Name", name), labeled("Brand", brand), labeled("Category", category),
                labeled("List price", listPrice), labeled("Net price", price), labeled("Discount %", discount),
                labeled("Rating", rating), labeled("Stock", stock), labeled("Availability", availability),
                labeled("Image URL", image), labeled("Description", description)
        );
        form.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> {
            if (button == save) {
                Produit produit = selected == null ? new Produit() : selected;
                produit.setSku(sku.getText().trim());
                produit.setNomProduit(name.getText().trim());
                produit.setMarque(brand.getText().trim());
                produit.setCategorieSource(category.getValue());
                produit.setCategorieMetier("Electronique");
                produit.setPrixUsd(Double.parseDouble(listPrice.getText().trim()));
                produit.setPrixNetUsd(Double.parseDouble(price.getText().trim()));
                produit.setRemisePct(Double.parseDouble(discount.getText().trim()));
                produit.setRating(Double.parseDouble(rating.getText().trim()));
                produit.setStock(Integer.parseInt(stock.getText().trim()));
                produit.setDisponibilite(availability.getText().trim());
                produit.setImagePrincipale(image.getText().trim());
                produit.setDescription(description.getText().trim());
                produit.setNbImages(1);
                produit.setSourceCatalogue("admin-panel");
                return produit;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(produit -> {
            try {
                Response response = clientHandler.sendAdminRequest(selected == null ? "ADMIN_CREATE_PRODUCT" : "ADMIN_UPDATE_PRODUCT", produit);
                feedback.setText(response != null ? response.getMessage() : "Operation failed.");
                feedback.getStyleClass().setAll(response != null && response.isSuccess() ? "success-label" : "error-label");
                if (response != null && response.isSuccess()) {
                    loadProducts();
                }
            } catch (Exception e) {
                feedback.setText(e.getMessage());
                feedback.getStyleClass().setAll("error-label");
            }
        });
    }

    private VBox labeled(String labelText, javafx.scene.Node node) {
        Label label = new Label(labelText);
        label.getStyleClass().add("muted-label");
        return new VBox(4, label, node);
    }

    private void deleteSelected() {
        Produit selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            feedback.setText("Select a product first.");
            feedback.getStyleClass().setAll("error-label");
            return;
        }
        try {
            Response response = clientHandler.sendAdminRequest("ADMIN_DELETE_PRODUCT", selected.getId());
            feedback.setText(response != null ? response.getMessage() : "Delete failed.");
            feedback.getStyleClass().setAll(response != null && response.isSuccess() ? "success-label" : "error-label");
            if (response != null && response.isSuccess()) {
                loadProducts();
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
