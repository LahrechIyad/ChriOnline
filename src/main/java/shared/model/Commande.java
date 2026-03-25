package shared.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents an Order.
 */
public class Commande implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int userId;
    private Date orderDate;
    private double total;
    private String status; // PENDING, VALIDATED, CANCELLED

    public Commande() {}

    public Commande(int id, int userId, Date orderDate, double total, String status) {
        this.id = id;
        this.userId = userId;
        this.orderDate = orderDate;
        this.total = total;
        this.status = status;
    }

    public Commande(int userId, Date orderDate, double total, String status) {
        this.userId = userId;
        this.orderDate = orderDate;
        this.total = total;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Commande #%d | User: %d | Date: %s | Total: %.2f $ | Status: %s",
                id, userId, orderDate.toString(), total, status);
    }
}
