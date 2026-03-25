package shared.model;

import java.io.Serializable;

/**
 * Represents a payment transaction.
 */
public class Paiement implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int orderId;
    private double amount;
    private String method; // e.g., CREDIT_CARD, PAYPAL
    private String status; // SUCCESS, FAILED

    public Paiement() {}

    public Paiement(int id, int orderId, double amount, String method, String status) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = status;
    }

    public Paiement(int orderId, double amount, String method, String status) {
        this.orderId = orderId;
        this.amount = amount;
        this.method = method;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Paiement #%d | Order: %d | %.2f $ | %s | %s",
                id, orderId, amount, method, status);
    }
}
