package server.service;

import server.dao.ProduitDAO;
import shared.model.Produit;
import shared.network.Response;

import java.util.List;

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
}
