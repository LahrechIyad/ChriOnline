package server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the SQLite Database Connection.
 */
public class DBConnection {
    private static final String URL = "jdbc:sqlite:chri_online.db";
    private static Connection connection = null;

    private DBConnection() {}

    /**
     * Gets the singleton database connection.
     * @return Connection object
     */
    public static Connection getConnection() {
        if (connection == null) {
            try {
                // Ensure the JDBC driver is loaded
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(URL);
                System.out.println("Connected to SQLite database: chri_online.db");
            } catch (SQLException | ClassNotFoundException e) {
                System.err.println("Database Connection failed: " + e.getMessage());
            }
        }
        return connection;
    }

    /**
     * Closes the database connection.
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Failed to close connection: " + e.getMessage());
            }
        }
    }

    /**
     * Initializes the database with the schema.sql file if it hasn't been created.
     */
    public static void initializeDatabase() {
        Connection conn = getConnection();
        if (conn == null) return;
        
        File schemaFile = new File("database/schema.sql");
        if (!schemaFile.exists()) {
            System.err.println("Schema file not found at database/schema.sql");
            return;
        }

        try (Statement stmt = conn.createStatement();
             BufferedReader br = new BufferedReader(new FileReader(schemaFile))) {
            
            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sql.append(line).append("\n");
                if (line.trim().endsWith(";")) {
                    stmt.execute(sql.toString());
                    sql.setLength(0); // Clear string builder
                }
            }
            System.out.println("Database schema initialized successfully.");
            applyMigrations(conn);
            importOrSeedElectronicsProducts(conn);
        } catch (Exception e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
        }
    }

    private static void applyMigrations(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "users", "is_verified", "ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT 0");
        addColumnIfMissing(conn, "users", "verification_code", "ALTER TABLE users ADD COLUMN verification_code VARCHAR(10)");
        addColumnIfMissing(conn, "users", "encrypted_verification_code", "ALTER TABLE users ADD COLUMN encrypted_verification_code TEXT");
        addColumnIfMissing(conn, "users", "public_key", "ALTER TABLE users ADD COLUMN public_key TEXT");

        addColumnIfMissing(conn, "products", "sku", "ALTER TABLE products ADD COLUMN sku TEXT");
        addColumnIfMissing(conn, "products", "nom_produit", "ALTER TABLE products ADD COLUMN nom_produit TEXT");
        addColumnIfMissing(conn, "products", "marque", "ALTER TABLE products ADD COLUMN marque TEXT");
        addColumnIfMissing(conn, "products", "categorie_source", "ALTER TABLE products ADD COLUMN categorie_source TEXT");
        addColumnIfMissing(conn, "products", "categorie_metier", "ALTER TABLE products ADD COLUMN categorie_metier TEXT");
        addColumnIfMissing(conn, "products", "prix_usd", "ALTER TABLE products ADD COLUMN prix_usd REAL");
        addColumnIfMissing(conn, "products", "remise_pct", "ALTER TABLE products ADD COLUMN remise_pct REAL");
        addColumnIfMissing(conn, "products", "prix_net_usd", "ALTER TABLE products ADD COLUMN prix_net_usd REAL");
        addColumnIfMissing(conn, "products", "rating", "ALTER TABLE products ADD COLUMN rating REAL");
        addColumnIfMissing(conn, "products", "disponibilite", "ALTER TABLE products ADD COLUMN disponibilite TEXT");
        addColumnIfMissing(conn, "products", "image_principale", "ALTER TABLE products ADD COLUMN image_principale TEXT");
        addColumnIfMissing(conn, "products", "nb_images", "ALTER TABLE products ADD COLUMN nb_images INTEGER");
        addColumnIfMissing(conn, "products", "source_catalogue", "ALTER TABLE products ADD COLUMN source_catalogue TEXT");

        addColumnIfMissing(conn, "payments", "masked_card", "ALTER TABLE payments ADD COLUMN masked_card VARCHAR(30)");
        addColumnIfMissing(conn, "payments", "encrypted_billing_or_delivery_address",
                "ALTER TABLE payments ADD COLUMN encrypted_billing_or_delivery_address TEXT");

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE products SET nom_produit = COALESCE(nom_produit, name) WHERE nom_produit IS NULL");
        } catch (SQLException ignored) {
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE products SET prix_usd = COALESCE(prix_usd, price), prix_net_usd = COALESCE(prix_net_usd, price) " +
                    "WHERE prix_usd IS NULL OR prix_net_usd IS NULL");
        } catch (SQLException ignored) {
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE products SET disponibilite = CASE WHEN stock > 5 THEN 'In Stock' WHEN stock > 0 THEN 'Low Stock' ELSE 'Out of Stock' END " +
                    "WHERE disponibilite IS NULL");
        } catch (SQLException ignored) {
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE products SET categorie_metier = COALESCE(categorie_metier, 'Electronique'), " +
                    "categorie_source = COALESCE(categorie_source, 'legacy'), marque = COALESCE(marque, 'ChriOnline'), " +
                    "remise_pct = COALESCE(remise_pct, 0), rating = COALESCE(rating, 4.0), nb_images = COALESCE(nb_images, 1), " +
                    "source_catalogue = COALESCE(source_catalogue, 'legacy-seed') WHERE nom_produit IS NOT NULL");
        } catch (SQLException ignored) {
        }
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String ddl) throws SQLException {
        if (!columnExists(conn, table, column)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(ddl);
            }
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void importOrSeedElectronicsProducts(Connection conn) throws SQLException, IOException {
        File datasetFile = new File("database/products_seed_subset.sql");
        if (datasetFile.exists()) {
            int imported = importElectronicsFromSqlFile(conn, datasetFile);
            if (imported > 0) {
                System.out.println("Imported " + imported + " electronics products from products_seed_subset.sql");
                return;
            }
            System.err.println("Dataset file found but no electronics rows were imported. Falling back to demo seed.");
        }
        seedElectronicsProducts(conn);
    }

    private static int importElectronicsFromSqlFile(Connection conn, File datasetFile) throws SQLException, IOException {
        String content = Files.readString(datasetFile.toPath(), StandardCharsets.UTF_16LE)
                .replace("\uFEFF", "")
                .replace("\u0000", "")
                .trim();
        if (content.isBlank()) {
            return 0;
        }

        String importTable = "products_import_tmp";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + importTable);
            stmt.execute("""
                    CREATE TEMP TABLE products_import_tmp (
                        sku TEXT,
                        nom_produit TEXT,
                        marque TEXT,
                        categorie_source TEXT,
                        categorie_metier TEXT,
                        prix_usd REAL,
                        remise_pct REAL,
                        prix_net_usd REAL,
                        rating REAL,
                        stock INTEGER,
                        disponibilite TEXT,
                        description TEXT,
                        image_principale TEXT,
                        nb_images INTEGER,
                        source_catalogue TEXT
                    )
                    """);
        }

        String rewrittenSql = content.replaceFirst("(?i)INSERT\\s+INTO\\s+products", "INSERT INTO " + importTable);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(rewrittenSql);
        }

        int count;
        try (PreparedStatement countStmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM " + importTable + " WHERE categorie_metier = 'Electronique'");
             ResultSet rs = countStmt.executeQuery()) {
            count = rs.next() ? rs.getInt(1) : 0;
        }
        if (count <= 0) {
            return 0;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM products");
            stmt.executeUpdate(buildImportInsertSql(conn, importTable));
            stmt.execute("DROP TABLE IF EXISTS " + importTable);
        }
        return count;
    }

    private static String buildImportInsertSql(Connection conn, String importTable) throws SQLException {
        boolean hasLegacyName = columnExists(conn, "products", "name");
        boolean hasLegacyPrice = columnExists(conn, "products", "price");

        if (hasLegacyName && hasLegacyPrice) {
            return """
                    INSERT INTO products (
                        name, description, price, stock,
                        sku, nom_produit, marque, categorie_source, categorie_metier,
                        prix_usd, remise_pct, prix_net_usd, rating, disponibilite,
                        image_principale, nb_images, source_catalogue
                    )
                    SELECT
                        nom_produit, description, prix_net_usd, stock,
                        sku, nom_produit, marque, categorie_source, categorie_metier,
                        prix_usd, remise_pct * 100.0, prix_net_usd, rating, disponibilite,
                        image_principale, nb_images, source_catalogue
                    FROM products_import_tmp
                    WHERE categorie_metier = 'Electronique'
                    """;
        }

        return """
                INSERT INTO products (
                    sku, nom_produit, marque, categorie_source, categorie_metier,
                    prix_usd, remise_pct, prix_net_usd, rating, stock, disponibilite,
                    description, image_principale, nb_images, source_catalogue
                )
                SELECT
                    sku, nom_produit, marque, categorie_source, categorie_metier,
                    prix_usd, remise_pct * 100.0, prix_net_usd, rating, stock, disponibilite,
                    description, image_principale, nb_images, source_catalogue
                FROM products_import_tmp
                WHERE categorie_metier = 'Electronique'
                """;
    }

    private static void seedElectronicsProducts(Connection conn) throws SQLException {
        try (PreparedStatement countStmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM products WHERE categorie_metier = 'Electronique'");
             ResultSet rs = countStmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        boolean hasLegacyName = columnExists(conn, "products", "name");
        boolean hasLegacyPrice = columnExists(conn, "products", "price");
        String insert;
        if (hasLegacyName && hasLegacyPrice) {
            insert = "INSERT INTO products (name, description, price, stock, sku, nom_produit, marque, categorie_source, categorie_metier, prix_usd, remise_pct, prix_net_usd, rating, disponibilite, image_principale, nb_images, source_catalogue) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'Electronique', ?, ?, ?, ?, ?, ?, ?, ?)";
        } else {
            insert = "INSERT INTO products (sku, nom_produit, marque, categorie_source, categorie_metier, prix_usd, remise_pct, prix_net_usd, rating, stock, disponibilite, description, image_principale, nb_images, source_catalogue) " +
                    "VALUES (?, ?, ?, ?, 'Electronique', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        }
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"ELEC-REALME-XT", "Realme XT", "Realme", "smartphones", 349.00, 8.0, 321.08, 4.4, 18, "In Stock", "Smartphone Android with AMOLED display and quad camera.", "https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=900&q=80", 3, "demo-electronics"});
        rows.add(new Object[]{"ELEC-ECHO-PLUS", "Amazon Echo Plus", "Amazon", "mobile-accessories", 149.00, 10.0, 134.10, 4.3, 12, "In Stock", "Connected speaker with premium sound and voice assistant.", "https://images.unsplash.com/photo-1543512214-318c7553f230?auto=format&fit=crop&w=900&q=80", 2, "demo-electronics"});
        rows.add(new Object[]{"ELEC-MBP14", "Apple MacBook Pro 14 Inch Space Grey", "Apple", "laptops", 2199.00, 5.0, 2089.05, 4.9, 7, "In Stock", "Professional laptop for creators and engineers.", "https://images.unsplash.com/photo-1517336714739-489689fd1ca8?auto=format&fit=crop&w=900&q=80", 4, "demo-electronics"});
        rows.add(new Object[]{"ELEC-IPHONE-CHARGER", "Apple iPhone Charger", "Apple", "mobile-accessories", 39.00, 0.0, 39.00, 4.5, 30, "In Stock", "Fast charging adapter compatible with recent iPhone models.", "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=900&q=80", 1, "demo-electronics"});
        rows.add(new Object[]{"ELEC-IPHONE-5S", "iPhone 5s", "Apple", "smartphones", 199.00, 15.0, 169.15, 4.0, 5, "Low Stock", "Compact classic iPhone for lightweight everyday use.", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=900&q=80", 2, "demo-electronics"});
        rows.add(new Object[]{"ELEC-GALAXY-S7", "Samsung Galaxy S7", "Samsung", "smartphones", 259.00, 12.0, 227.92, 4.2, 8, "In Stock", "Reliable Samsung smartphone with vivid AMOLED display.", "https://images.unsplash.com/photo-1510557880182-3d4d3cba35a5?auto=format&fit=crop&w=900&q=80", 2, "demo-electronics"});
        rows.add(new Object[]{"ELEC-GALAXY-S10", "Samsung Galaxy S10", "Samsung", "smartphones", 499.00, 10.0, 449.10, 4.6, 9, "In Stock", "Flagship smartphone with premium camera system.", "https://images.unsplash.com/photo-1580910051074-3eb694886505?auto=format&fit=crop&w=900&q=80", 3, "demo-electronics"});
        rows.add(new Object[]{"ELEC-REALME-C35", "Realme C35", "Realme", "smartphones", 249.00, 7.0, 231.57, 4.1, 14, "In Stock", "Affordable smartphone with large battery and clean design.", "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=900&q=80", 2, "demo-electronics"});
        rows.add(new Object[]{"ELEC-IPAD-MINI-2021", "iPad Mini 2021 Starlight", "Apple", "tablets", 649.00, 6.0, 610.06, 4.8, 6, "In Stock", "Compact tablet ideal for reading, drawing and streaming.", "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=900&q=80", 3, "demo-electronics"});
        rows.add(new Object[]{"ELEC-HOMEPOD-MINI", "Apple HomePod Mini Cosmic Grey", "Apple", "mobile-accessories", 129.00, 4.0, 123.84, 4.4, 10, "In Stock", "Smart speaker for a connected Apple ecosystem.", "https://images.unsplash.com/photo-1512446816042-444d64126727?auto=format&fit=crop&w=900&q=80", 2, "demo-electronics"});
        rows.add(new Object[]{"ELEC-MAGSAFE-BATTERY", "Apple MagSafe Battery Pack", "Apple", "mobile-accessories", 109.00, 0.0, 109.00, 4.3, 11, "In Stock", "Portable magnetic battery pack for compatible iPhones.", "https://images.unsplash.com/photo-1609081219090-a6d81d3085bf?auto=format&fit=crop&w=900&q=80", 2, "demo-electronics"});
        rows.add(new Object[]{"ELEC-MATEBOOK-XPRO", "Huawei Matebook X Pro", "Huawei", "laptops", 1699.00, 9.0, 1546.09, 4.7, 4, "Low Stock", "Slim laptop with premium screen and long battery life.", "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=900&q=80", 3, "demo-electronics"});
        rows.add(new Object[]{"ELEC-TABS8PLUS", "Samsung Galaxy Tab S8 Plus Grey", "Samsung", "tablets", 999.00, 11.0, 889.11, 4.7, 5, "Low Stock", "Large premium tablet for productivity and entertainment.", "https://images.unsplash.com/photo-1585792180666-f7347c490ee2?auto=format&fit=crop&w=900&q=80", 3, "demo-electronics"});
        rows.add(new Object[]{"ELEC-BEATS-FLEX", "Beats Flex Wireless Earphones", "Beats", "mobile-accessories", 79.00, 5.0, 75.05, 4.2, 20, "In Stock", "Wireless earphones with flexible neckband and clear sound.", "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80", 2, "demo-electronics"});

        try (PreparedStatement stmt = conn.prepareStatement(insert)) {
            for (Object[] row : rows) {
                if (hasLegacyName && hasLegacyPrice) {
                    stmt.setString(1, (String) row[1]);
                    stmt.setString(2, (String) row[10]);
                    stmt.setDouble(3, (Double) row[6]);
                    stmt.setInt(4, (Integer) row[8]);
                    stmt.setString(5, (String) row[0]);
                    stmt.setString(6, (String) row[1]);
                    stmt.setString(7, (String) row[2]);
                    stmt.setString(8, (String) row[3]);
                    stmt.setDouble(9, (Double) row[4]);
                    stmt.setDouble(10, (Double) row[5]);
                    stmt.setDouble(11, (Double) row[6]);
                    stmt.setDouble(12, (Double) row[7]);
                    stmt.setString(13, (String) row[9]);
                    stmt.setString(14, (String) row[11]);
                    stmt.setInt(15, (Integer) row[12]);
                    stmt.setString(16, (String) row[13]);
                } else {
                    stmt.setString(1, (String) row[0]);
                    stmt.setString(2, (String) row[1]);
                    stmt.setString(3, (String) row[2]);
                    stmt.setString(4, (String) row[3]);
                    stmt.setDouble(5, (Double) row[4]);
                    stmt.setDouble(6, (Double) row[5]);
                    stmt.setDouble(7, (Double) row[6]);
                    stmt.setDouble(8, (Double) row[7]);
                    stmt.setInt(9, (Integer) row[8]);
                    stmt.setString(10, (String) row[9]);
                    stmt.setString(11, (String) row[10]);
                    stmt.setString(12, (String) row[11]);
                    stmt.setInt(13, (Integer) row[12]);
                    stmt.setString(14, (String) row[13]);
                }
                stmt.addBatch();
            }
            stmt.executeBatch();
            System.out.println("Seeded electronics store demo data.");
        }
    }
}
