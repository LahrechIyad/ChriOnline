# ChriOnline E-Commerce Server-Client Project

## 1️⃣ Full Project Architecture
This project implements a complete client-server e-commerce application using a Clean Architecture approach with Java TCP Sockets. The client UI is built with **JavaFX** and the project uses **Maven** for dependency management.

```text
ChriOnline/
├── pom.xml
├── database/
│   └── schema.sql
└── src/
    └── main/
        └── java/
            ├── client/
            │   ├── main/ClientMain.java
            │   ├── network/ClientTCP.java
            │   └── ui/ (AuthView, ProductView, CartView, OrderView, MainDashboard)
            ├── server/
            │   ├── main/ServerMain.java
            │   ├── network/ServerTCP.java, ClientHandler.java
            │   ├── service/ (UserService, ProduitService, etc.)
            │   ├── dao/ (UserDAO, ProduitDAO, etc.)
            │   └── database/DBConnection.java
            └── shared/
                ├── model/ (User, Produit, Commande, etc.)
                └── network/ (Request, Response)
```

## 2️⃣ Each Folder Explained

- **`shared`**: Domain entities representing business objects (`model`) and protocol standard objects (`network`). Shared between Client and Server.
- **`server`**: 
  - `database/DBConnection`: Connects the Server to the SQLite database.
  - `dao`: Data Access Objects executing JDBC queries to manipulate DB state.
  - `service`: Business Logic Layer (validates data, enforces rules).
  - `network`: `ServerTCP` hosts the ServerSocket. `ClientHandler` processes TCP objects for each independent Client session.
  - `main/ServerMain`: Instantiates services, initiates DB schema, and opens TCP port 8081.
- **`client`**: 
  - `network/ClientTCP`: Abstracts networking logic.
  - `ui`: Graphical User Interfaces using modern JavaFX components (VBox, HBox, TableView).
  - `main/ClientMain`: Client Application Entry point extending `javafx.application.Application`.

## 5️⃣ Build and Execution Instructions (Maven required)

To compile and run this application seamlessly, you need **Apache Maven**. The SQLite driver and JavaFX UI libraries are automatically resolved by `pom.xml`.

### 1. Start the Server
Navigate to the root directory `ChriOnline` where the `pom.xml` is located, and execute the server using Maven:
```bash
mvn clean compile exec:java
```
*Note: The server will automatically connect to `chri_online.db` and execute `database/schema.sql` to initialize tables and insert fake products. It listens on port 8081.*

### 2. Start the Client UI (JavaFX)
Open a new terminal window in the `ChriOnline` directory, and launch the JavaFX Client Application:
```bash
mvn javafx:run
```

### Execution Flow Example:
1.  **Register/Login**: The JavaFX application opens a clean login screen. Click "Don't have an account? Register" to sign up, then switch back to login.
2.  **View Products**: Once logged in, you enter the Dashboard showing the Product Catalog in a table.
3.  **Add to Cart**: Select a product from the table, enter the Quantity, and click "Add to Cart".
4.  **Cart & Validation**: Navigate to "My Cart" on the sidebar. Review items and click "Validate Order".
5.  **Simulate Payment**: Navigate to "My Orders". Select a pending order, choose a payment method, and click "Pay Selected Order". The system registers the order status to `SUCCESS`.
