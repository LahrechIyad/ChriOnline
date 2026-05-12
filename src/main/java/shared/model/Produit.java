package shared.model;

import java.io.Serializable;

/**
 * Represents a Product in the catalog.
 */
public class Produit implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String sku;
    private String nomProduit;
    private String marque;
    private String categorieSource;
    private String categorieMetier;
    private double prixUsd;
    private double remisePct;
    private double prixNetUsd;
    private double rating;
    private String disponibilite;
    private String description;
    private int stock;
    private String imagePrincipale;
    private int nbImages;
    private String sourceCatalogue;

    public Produit() {}

    public Produit(int id, String sku, String nomProduit, String marque, String categorieSource,
                   String categorieMetier, double prixUsd, double remisePct, double prixNetUsd,
                   double rating, int stock, String disponibilite, String description,
                   String imagePrincipale, int nbImages, String sourceCatalogue) {
        this.id = id;
        this.sku = sku;
        this.nomProduit = nomProduit;
        this.marque = marque;
        this.categorieSource = categorieSource;
        this.categorieMetier = categorieMetier;
        this.prixUsd = prixUsd;
        this.remisePct = remisePct;
        this.prixNetUsd = prixNetUsd;
        this.rating = rating;
        this.stock = stock;
        this.disponibilite = disponibilite;
        this.description = description;
        this.imagePrincipale = imagePrincipale;
        this.nbImages = nbImages;
        this.sourceCatalogue = sourceCatalogue;
    }

    public Produit(String nomProduit, String description, double prixNetUsd, int stock) {
        this.nomProduit = nomProduit;
        this.description = description;
        this.prixNetUsd = prixNetUsd;
        this.prixUsd = prixNetUsd;
        this.stock = stock;
        this.disponibilite = stock > 0 ? "In Stock" : "Out of Stock";
        this.categorieMetier = "Electronique";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getNomProduit() { return nomProduit; }
    public void setNomProduit(String nomProduit) { this.nomProduit = nomProduit; }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    public String getCategorieSource() { return categorieSource; }
    public void setCategorieSource(String categorieSource) { this.categorieSource = categorieSource; }

    public String getCategorieMetier() { return categorieMetier; }
    public void setCategorieMetier(String categorieMetier) { this.categorieMetier = categorieMetier; }

    public double getPrixUsd() { return prixUsd; }
    public void setPrixUsd(double prixUsd) { this.prixUsd = prixUsd; }

    public double getRemisePct() { return remisePct; }
    public void setRemisePct(double remisePct) { this.remisePct = remisePct; }

    public double getPrixNetUsd() { return prixNetUsd; }
    public void setPrixNetUsd(double prixNetUsd) { this.prixNetUsd = prixNetUsd; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getDisponibilite() { return disponibilite; }
    public void setDisponibilite(String disponibilite) { this.disponibilite = disponibilite; }

    public String getImagePrincipale() { return imagePrincipale; }
    public void setImagePrincipale(String imagePrincipale) { this.imagePrincipale = imagePrincipale; }

    public int getNbImages() { return nbImages; }
    public void setNbImages(int nbImages) { this.nbImages = nbImages; }

    public String getSourceCatalogue() { return sourceCatalogue; }
    public void setSourceCatalogue(String sourceCatalogue) { this.sourceCatalogue = sourceCatalogue; }

    public String getDisplayName() {
        return nomProduit != null && !nomProduit.isBlank() ? nomProduit : getName();
    }

    public double getFinalPrice() {
        return prixNetUsd > 0 ? prixNetUsd : prixUsd;
    }

    public boolean isInStock() {
        return stock > 0 && !"Out of Stock".equalsIgnoreCase(disponibilite);
    }

    public String getName() {
        return getDisplayName();
    }

    public void setName(String name) {
        this.nomProduit = name;
    }

    public double getPrice() {
        return getFinalPrice();
    }

    public void setPrice(double price) {
        this.prixNetUsd = price;
        if (this.prixUsd == 0) {
            this.prixUsd = price;
        }
    }

    @Override
    public String toString() {
        return String.format("Produit #%d: %s | %.2f $ | Stock: %d", id, getDisplayName(), getFinalPrice(), stock);
    }
}
