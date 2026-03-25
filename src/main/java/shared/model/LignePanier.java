package shared.model;

import java.io.Serializable;

/**
 * Represents a line item in a cart.
 */
public class LignePanier implements Serializable {
    private static final long serialVersionUID = 1L;

    private Produit product;
    private int quantity;

    public LignePanier() {}

    public LignePanier(Produit product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Produit getProduct() { return product; }
    public void setProduct(Produit product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getSubTotal() {
        return product.getPrice() * quantity;
    }

    @Override
    public String toString() {
        return String.format("%s (x%d) = %.2f $", product.getName(), quantity, getSubTotal());
    }
}
