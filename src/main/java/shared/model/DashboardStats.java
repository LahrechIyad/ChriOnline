package shared.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DashboardStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private int totalProducts;
    private int lowStockProducts;
    private int totalOrders;
    private double totalRevenue;
    private List<Commande> recentOrders = new ArrayList<>();
    private List<Produit> bestRatedProducts = new ArrayList<>();

    public int getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(int totalProducts) {
        this.totalProducts = totalProducts;
    }

    public int getLowStockProducts() {
        return lowStockProducts;
    }

    public void setLowStockProducts(int lowStockProducts) {
        this.lowStockProducts = lowStockProducts;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public List<Commande> getRecentOrders() {
        return recentOrders;
    }

    public void setRecentOrders(List<Commande> recentOrders) {
        this.recentOrders = recentOrders;
    }

    public List<Produit> getBestRatedProducts() {
        return bestRatedProducts;
    }

    public void setBestRatedProducts(List<Produit> bestRatedProducts) {
        this.bestRatedProducts = bestRatedProducts;
    }
}
