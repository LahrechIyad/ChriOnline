package server.service;

import server.dao.ProduitDAO;
import shared.model.Produit;
import shared.network.Response;

import java.util.List;
import java.util.Map;

/**
 * Service handling Product Business Logic.
 */
public class ProduitService {
    private ProduitDAO produitDAO;

    public ProduitService() {
        this.produitDAO = new ProduitDAO();
    }

    /**
     * Gets all products.
     */
    public Response getAllProducts() {
        List<Produit> products = produitDAO.findAll();
        if (products != null && !products.isEmpty()) {
            return new Response(true, "Products retrieved successfully.", products);
        } else {
            return new Response(false, "No products found.", null);
        }
    }

    /**
     * Gets product details by ID.
     */
    public Response getProductDetails(int productId) {
        Produit product = produitDAO.findById(productId);
        if (product != null) {
            return new Response(true, "Product found.", product);
        } else {
            return new Response(false, "Product not found.", null);
        }
    }

    /**
     * Checks stock and optionally updates it if an order is placed.
     */
    public boolean checkAndUpdateStock(int productId, int quantityToDeduct) {
        Produit product = produitDAO.findById(productId);
        if (product != null && product.getStock() >= quantityToDeduct) {
            int newStock = product.getStock() - quantityToDeduct;
            return produitDAO.updateStock(productId, newStock);
        }
        return false;
    }

    public Response createProduct(Produit produit) {
        if (produit == null || produit.getNomProduit() == null || produit.getNomProduit().isBlank()) {
            return new Response(false, "Product name is required.", null);
        }
        produit.setCategorieMetier("Electronique");
        if (produitDAO.createProduct(produit)) {
            return new Response(true, "Product created successfully.", null);
        }
        return new Response(false, "Failed to create product.", null);
    }

    public Response updateProduct(Produit produit) {
        if (produit == null || produit.getId() <= 0) {
            return new Response(false, "Valid product ID is required.", null);
        }
        produit.setCategorieMetier("Electronique");
        if (produitDAO.updateProduct(produit)) {
            return new Response(true, "Product updated successfully.", null);
        }
        return new Response(false, "Failed to update product.", null);
    }

    public Response deleteProduct(int id) {
        if (produitDAO.deleteProduct(id)) {
            return new Response(true, "Product deleted successfully.", null);
        }
        return new Response(false, "Failed to delete product.", null);
    }

    public Response getLowStockProducts(int threshold) {
        List<Produit> products = produitDAO.findLowStockProducts(threshold);
        return new Response(true, "Low stock products retrieved.", products);
    }

    public Response searchProducts(String keyword) {
        return new Response(true, "Product search completed.", produitDAO.searchProducts(keyword == null ? "" : keyword));
    }

    public Response filterProductsByCategory(String category) {
        if (category == null || category.isBlank() || "All".equalsIgnoreCase(category)) {
            return getAllProducts();
        }
        return new Response(true, "Products filtered by category.", produitDAO.findByCategory(category));
    }
}
