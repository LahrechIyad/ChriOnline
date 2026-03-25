package shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a shopping cart.
 */
public class Panier implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private List<LignePanier> items;

    public Panier() {
        this.items = new ArrayList<>();
    }

    public Panier(int id, int userId) {
        this.id = id;
        this.userId = userId;
        this.items = new ArrayList<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public List<LignePanier> getItems() { return items; }
    public void setItems(List<LignePanier> items) { this.items = items; }

    public void addItem(Produit product, int quantity) {
        for (LignePanier item : items) {
            if (item.getProduct().getId() == product.getId()) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        items.add(new LignePanier(product, quantity));
    }

    public void removeItem(int productId) {
        items.removeIf(item -> item.getProduct().getId() == productId);
    }

    public void clear() {
        items.clear();
    }

    public double getTotal() {
        return items.stream().mapToDouble(LignePanier::getSubTotal).sum();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Cart Items:\n");
        for (LignePanier item : items) {
            sb.append("- ").append(item.toString()).append("\n");
        }
        sb.append(String.format("Total: %.2f $", getTotal()));
        return sb.toString();
    }
}
