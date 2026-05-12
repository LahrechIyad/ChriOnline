package shared.model;

import java.io.Serializable;

public class OrderItemDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private int orderId;
    private int productId;
    private String productName;
    private String brand;
    private int quantity;
    private double unitPrice;

    public OrderItemDetail() {
    }

    public OrderItemDetail(int orderId, int productId, String productName, String brand, int quantity, double unitPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.productName = productName;
        this.brand = brand;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getBrand() {
        return brand;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getSubtotal() {
        return unitPrice * quantity;
    }
}
