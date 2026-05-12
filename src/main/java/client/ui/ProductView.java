package client.ui;

import client.network.ClientTCP;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import shared.model.Produit;
import shared.model.User;
import shared.network.Request;
import shared.network.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductView {
    private final BorderPane view = new BorderPane();
    private final ClientTCP clientTCP;
    private final FlowPane grid = new FlowPane();
    private final Label feedback = new Label();
    private TextField searchField;
    private String activeCategory = "All";

    public ProductView(ClientTCP clientTCP, User user) {
        this.clientTCP = clientTCP;
        buildView();
        loadProducts();
    }

    private void buildView() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(18));

        VBox hero = new VBox(10);
        hero.getStyleClass().add("hero-pane");
        hero.setPadding(new Insets(26));
        Label title = new Label("Upgrade your digital experience");
        title.getStyleClass().add("hero-title");
        Label subtitle = new Label("Discover smartphones, laptops, tablets and accessories secured by AES/RSA communication.");
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);
        hero.getChildren().addAll(title, subtitle);

        HBox controls = new HBox(12);
        controls.setAlignment(Pos.CENTER_LEFT);
        searchField = new TextField();
        searchField.setPromptText("Search electronics, brands, categories...");
        searchField.setPrefWidth(320);
        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("primary-button");
        Button resetBtn = new Button("All products");
        resetBtn.getStyleClass().add("secondary-button");
        controls.getChildren().addAll(searchField, searchBtn, resetBtn);

        HBox categories = new HBox(10,
                categoryChip("All"),
                categoryChip("smartphones"),
                categoryChip("laptops"),
                categoryChip("tablets"),
                categoryChip("mobile-accessories")
        );

        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPrefWrapLength(980);

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        feedback.getStyleClass().add("muted-label");
        searchBtn.setOnAction(e -> searchProducts());
        resetBtn.setOnAction(e -> {
            searchField.clear();
            activeCategory = "All";
            loadProducts();
        });

        content.getChildren().addAll(hero, controls, categories, feedback, scrollPane);
        view.setCenter(content);
    }

    @SuppressWarnings("unchecked")
    private void loadProducts() {
        Response response = clientTCP.sendRequest(new Request("GET_PRODUCTS", null));
        if (response != null && response.isSuccess()) {
            renderProducts((List<Produit>) response.getData());
        } else {
            feedback.setText(response != null ? response.getMessage() : "Failed to load products.");
            feedback.getStyleClass().setAll("error-label");
        }
    }

    @SuppressWarnings("unchecked")
    private void searchProducts() {
        if (searchField.getText().isBlank()) {
            applyCategory(activeCategory);
            return;
        }
        Response response = clientTCP.sendRequest(new Request("SEARCH_PRODUCTS", searchField.getText()));
        if (response != null && response.isSuccess()) {
            renderProducts((List<Produit>) response.getData());
            feedback.setText("Search results for \"" + searchField.getText() + "\"");
            feedback.getStyleClass().setAll("success-label");
        } else {
            feedback.setText(response != null ? response.getMessage() : "Search failed.");
            feedback.getStyleClass().setAll("error-label");
        }
    }

    @SuppressWarnings("unchecked")
    private void applyCategory(String category) {
        activeCategory = category;
        if ("All".equalsIgnoreCase(category)) {
            loadProducts();
            return;
        }
        Response response = clientTCP.sendRequest(new Request("FILTER_PRODUCTS_BY_CATEGORY", category));
        if (response != null && response.isSuccess()) {
            renderProducts((List<Produit>) response.getData());
            feedback.setText("Category: " + prettyCategory(category));
            feedback.getStyleClass().setAll("muted-label");
        } else {
            feedback.setText(response != null ? response.getMessage() : "Category filter failed.");
            feedback.getStyleClass().setAll("error-label");
        }
    }

    private Button categoryChip(String category) {
        Button button = new Button(prettyCategory(category));
        button.getStyleClass().add("chip-button");
        button.setOnAction(e -> applyCategory(category));
        return button;
    }

    private void renderProducts(List<Produit> products) {
        grid.getChildren().clear();
        if (products == null || products.isEmpty()) {
            Label empty = new Label("No electronics products available.");
            empty.getStyleClass().add("muted-label");
            grid.getChildren().add(empty);
            return;
        }
        grid.getChildren().add(featuredCard(products.get(0)));
        for (Produit produit : products) {
            grid.getChildren().add(productCard(produit));
        }
    }

    private VBox featuredCard(Produit produit) {
        VBox card = new VBox(12);
        card.getStyleClass().add("featured-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(940);
        HBox row = new HBox(18, imageFor(produit, 250, 180), productSummary(produit, true));
        card.getChildren().addAll(new Label("Featured product"), row);
        return card;
    }

    private VBox productCard(Produit produit) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(300);
        card.getChildren().addAll(imageFor(produit, 268, 170), productSummary(produit, false), addToCartButton(produit));
        return card;
    }

    private VBox productSummary(Produit produit, boolean featured) {
        VBox box = new VBox(8);
        Label name = new Label(produit.getDisplayName());
        name.getStyleClass().add(featured ? "featured-title" : "product-title");
        name.setWrapText(true);
        Label brand = new Label((produit.getMarque() == null ? "Brand" : produit.getMarque()) + " • " + prettyCategory(produit.getCategorieSource()));
        brand.getStyleClass().add("muted-label");
        Label rating = new Label(String.format("Rating %.1f / 5", produit.getRating()));
        rating.getStyleClass().add("muted-label");
        Label stock = new Label(produit.getDisponibilite());
        stock.getStyleClass().add(produit.isInStock() ? "success-badge" : "danger-badge");
        Label oldPrice = new Label(produit.getRemisePct() > 0 ? String.format("$%.2f", produit.getPrixUsd()) : "");
        oldPrice.getStyleClass().add("old-price");
        Label newPrice = new Label(String.format("$%.2f", produit.getFinalPrice()));
        newPrice.getStyleClass().add("price-label");
        Label desc = new Label(produit.getDescription());
        desc.getStyleClass().add("muted-label");
        desc.setWrapText(true);
        box.getChildren().addAll(name, brand, rating, stock, new HBox(10, oldPrice, newPrice), desc);
        return box;
    }

    private Button addToCartButton(Produit produit) {
        Button button = new Button(produit.isInStock() ? "Add to cart" : "Unavailable");
        button.getStyleClass().add(produit.isInStock() ? "primary-button" : "secondary-button");
        button.setDisable(!produit.isInStock());
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> {
            Map<String, Integer> payload = new HashMap<>();
            payload.put("productId", produit.getId());
            payload.put("quantity", 1);
            Response response = clientTCP.sendRequest(new Request("ADD_TO_CART", payload));
            feedback.setText(response != null ? response.getMessage() : "Failed to add to cart.");
            feedback.getStyleClass().setAll(response != null && response.isSuccess() ? "success-label" : "error-label");
            if (response != null && response.isSuccess()) {
                loadProducts();
            }
        });
        return button;
    }

    private StackPane imageFor(Produit produit, double width, double height) {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("image-shell");
        pane.setPrefSize(width, height);
        if (produit.getImagePrincipale() != null && !produit.getImagePrincipale().isBlank()) {
            Image image = loadProductImage(produit.getImagePrincipale(), width, height, produit.getDisplayName());
            if (image != null) {
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(width);
                imageView.setFitHeight(height);
                imageView.setPreserveRatio(true);
                pane.getChildren().add(imageView);
                return pane;
            }
        }
        Label placeholder = new Label("Image unavailable");
        placeholder.getStyleClass().add("muted-label");
        pane.getChildren().add(placeholder);
        return pane;
    }

    private Image loadProductImage(String imageUrl, double width, double height, String productName) {
        for (String candidate : imageCandidates(imageUrl)) {
            try {
                Image image = new Image(candidate, width, height, true, true, false);
                if (!image.isError()) {
                    if (!candidate.equals(imageUrl)) {
                        System.out.println("Image fallback used for " + productName + ": " + candidate);
                    }
                    return image;
                }
                if (image.getException() != null) {
                    System.err.println("Image load failed for " + productName + ": " + candidate + " -> " + image.getException().getMessage());
                }
            } catch (Exception e) {
                System.err.println("Image load failed for " + productName + ": " + candidate + " -> " + e.getMessage());
            }
        }
        return null;
    }

    private String[] imageCandidates(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return new String[0];
        }
        if (imageUrl.toLowerCase().endsWith(".webp")) {
            String proxied = "https://images.weserv.nl/?url=" + stripProtocol(imageUrl) + "&output=jpg";
            return new String[]{proxied, imageUrl};
        }
        return new String[]{imageUrl};
    }

    private String stripProtocol(String imageUrl) {
        return imageUrl.replaceFirst("^https?://", "");
    }

    private String prettyCategory(String category) {
        if (category == null) {
            return "Electronics";
        }
        return switch (category.toLowerCase()) {
            case "mobile-accessories" -> "Accessories";
            case "smartphones" -> "Smartphones";
            case "laptops" -> "Laptops";
            case "tablets" -> "Tablets";
            default -> "All".equalsIgnoreCase(category) ? "All" : category;
        };
    }

    public BorderPane getView() {
        return view;
    }
}
