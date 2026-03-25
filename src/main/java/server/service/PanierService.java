package server.service;

import shared.model.Panier;
import shared.model.Produit;
import shared.model.LignePanier;
import shared.network.Response;
import server.dao.ProduitDAO;

import java.util.HashMap;
import java.util.Map;

/**
 * Service handling Cart Business Logic.
 * Manages carts in memory per user session.
 */
public class PanierService {
    // Stores in-memory carts mapped by userId
    private Map<Integer, Panier> carts;
    private ProduitDAO produitDAO;

    public PanierService() {
        this.carts = new HashMap<>();
        this.produitDAO = new ProduitDAO();
    }

    private Panier getOrCreateCart(int userId) {
        if (!carts.containsKey(userId)) {
            carts.put(userId, new Panier(userId, userId));
        }
        return carts.get(userId);
    }

    public Response addProductToCart(int userId, int productId, int quantity) {
        Produit product = produitDAO.findById(productId);
        if (product == null) {
            return new Response(false, "Product not found.", null);
        }

        if (product.getStock() < quantity) {
            return new Response(false, "Not enough stock available. Remaining: " + product.getStock(), null);
        }

        Panier cart = getOrCreateCart(userId);
        
        // Also check if existing quantity + new quantity > stock
        int existingQuantity = 0;
        for (LignePanier item : cart.getItems()) {
            if (item.getProduct().getId() == productId) {
                existingQuantity = item.getQuantity();
            }
        }
        
        if (existingQuantity + quantity > product.getStock()) {
            return new Response(false, "Total requested quantity exceeds available stock.", null);
        }

        cart.addItem(product, quantity);
        return new Response(true, "Product added to cart.", cart);
    }

    public Response removeProductFromCart(int userId, int productId) {
        Panier cart = getOrCreateCart(userId);
        cart.removeItem(productId);
        return new Response(true, "Product removed from cart.", cart);
    }

    public Response getCart(int userId) {
        Panier cart = getOrCreateCart(userId);
        return new Response(true, "Cart retrieved.", cart);
    }

    public Panier clearCart(int userId) {
        Panier cart = getOrCreateCart(userId);
        cart.clear();
        return cart;
    }
}
