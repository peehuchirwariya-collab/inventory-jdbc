import java.sql.*;
import java.util.Scanner;

public class InventoryApp {

    // IMPORTANT: Change "root" and "password" to your MySQL Workbench credentials!
    private static final String URL = "jdbc:mysql://localhost:3306/retail_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "Peehu@17";

    // Helper method to establish connection
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n==========================================");
            System.out.println("   INVENTORY MANAGEMENT SYSTEM (JDBC)    ");
            System.out.println("==========================================");
            System.out.println("1. Insert Product Records (at least 3)");
            System.out.println("2. Display by Category (Qty < 20, Price DESC)");
            System.out.println("3. Update Product Stock (Add Quantity)");
            System.out.println("4. Search and Modify Product Price");
            System.out.println("5. Inventory Total Value & Delete Zero-Stock");
            System.out.println("6. Exit");
            System.out.print("Enter your choice (1-6): ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // clear buffer

            switch (choice) {
                case 1 -> insertProducts(scanner);
                case 2 -> retrieveByCategory(scanner);
                case 3 -> updateStock(scanner);
                case 4 -> modifyProductPrice(scanner);
                case 5 -> aggregateAndDelete(scanner);
                case 6 -> {
                    System.out.println("Exiting application. Goodbye!");
                    scanner.close();
                    return;
                }
                default -> System.out.println("Invalid option! Please enter 1 to 6.");
            }
        }
    }

    // ------------------------------------------------------------------------
    // (a) Insert Product Records (2 Marks)
    // Inserts at least 3 records using PreparedStatement & handles duplicate IDs
    // ------------------------------------------------------------------------
    private static void insertProducts(Scanner sc) {
        String sql = "INSERT INTO Product (product_id, product_name, category, quantity, price) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            System.out.println("\nEnter 3 Product Records:");

            for (int i = 1; i <= 3; i++) {
                System.out.println("\n--- Product #" + i + " ---");
                System.out.print("Enter Product ID: ");
                int id = sc.nextInt();
                sc.nextLine();

                System.out.print("Enter Product Name: ");
                String name = sc.nextLine();

                System.out.print("Enter Category: ");
                String category = sc.nextLine();

                System.out.print("Enter Quantity: ");
                int qty = sc.nextInt();

                System.out.print("Enter Price: ");
                double price = sc.nextDouble();

                ps.setInt(1, id);
                ps.setString(2, name);
                ps.setString(3, category);
                ps.setInt(4, qty);
                ps.setDouble(5, price);

                try {
                    ps.executeUpdate();
                    System.out.println("✓ Product inserted successfully!");
                } catch (SQLIntegrityConstraintViolationException ex) {
                    System.out.println("✗ Error: Product ID " + id + " already exists! Duplicate IDs are not allowed.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // (b) Conditional Retrieval and Sorting (2 Marks)
    // Products of category where quantity < 20, sorted by price in descending order
    // ------------------------------------------------------------------------
    private static void retrieveByCategory(Scanner sc) {
        System.out.print("\nEnter Category to search: ");
        String category = sc.nextLine();

        String sql = "SELECT * FROM Product WHERE category = ? AND quantity < 20 ORDER BY price DESC";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n-------------------------------------------------------------");
                System.out.printf("%-10s %-20s %-15s %-10s %-10s\n", "ID", "Name", "Category", "Quantity", "Price");
                System.out.println("-------------------------------------------------------------");

                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%-10d %-20s %-15s %-10d $%-9.2f\n",
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getString("category"),
                            rs.getInt("quantity"),
                            rs.getDouble("price"));
                }

                if (!found) {
                    System.out.println("No products found in category '" + category + "' with quantity < 20.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // (c) Update Product Stock (2 Marks)
    // Accepts product ID & quantity to add; displays message if not found
    // ------------------------------------------------------------------------
    private static void updateStock(Scanner sc) {
        System.out.print("\nEnter Product ID to update stock: ");
        int id = sc.nextInt();
        System.out.print("Enter Quantity to add to existing stock: ");
        int addQty = sc.nextInt();

        String sql = "UPDATE Product SET quantity = quantity + ? WHERE product_id = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, addQty);
            ps.setInt(2, id);

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("✓ Stock updated successfully!");
            } else {
                System.out.println("✗ Product with ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // (d) Search and Modify Product Details (2 Marks)
    // Accepts ID and new price; updates only if exists and new price > 0
    // ------------------------------------------------------------------------
    private static void modifyProductPrice(Scanner sc) {
        System.out.print("\nEnter Product ID: ");
        int id = sc.nextInt();
        System.out.print("Enter New Price: ");
        double newPrice = sc.nextDouble();

        if (newPrice <= 0) {
            System.out.println("✗ Invalid price! Price must be greater than zero.");
            return;
        }

        String updateSql = "UPDATE Product SET price = ? WHERE product_id = ?";
        String selectSql = "SELECT * FROM Product WHERE product_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement updatePs = conn.prepareStatement(updateSql);
             PreparedStatement selectPs = conn.prepareStatement(selectSql)) {

            updatePs.setDouble(1, newPrice);
            updatePs.setInt(2, id);

            int rows = updatePs.executeUpdate();
            if (rows > 0) {
                System.out.println("✓ Price updated successfully!");

                // Retrieve and display updated product details
                selectPs.setInt(1, id);
                try (ResultSet rs = selectPs.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("\nUpdated Product Details:");
                        System.out.println("ID       : " + rs.getInt("product_id"));
                        System.out.println("Name     : " + rs.getString("product_name"));
                        System.out.println("Category : " + rs.getString("category"));
                        System.out.println("Quantity : " + rs.getInt("quantity"));
                        System.out.println("Price    : $" + rs.getDouble("price"));
                    }
                }
            } else {
                System.out.println("✗ Product with ID " + id + " does not exist.");
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    // (e) Aggregate Query and Conditional Deletion (2 Marks)
    // 1. Calculates SUM(quantity * price)
    // 2. Deletes record only if quantity is 0
    // ------------------------------------------------------------------------
    private static void aggregateAndDelete(Scanner sc) {
        String aggregateSql = "SELECT SUM(quantity * price) AS total_inventory_value FROM Product";
        String checkSql = "SELECT quantity FROM Product WHERE product_id = ?";
        String deleteSql = "DELETE FROM Product WHERE product_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement sumPs = conn.prepareStatement(aggregateSql);
             PreparedStatement checkPs = conn.prepareStatement(checkSql);
             PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {

            // 1. Calculate & Display total inventory value
            try (ResultSet rs = sumPs.executeQuery()) {
                if (rs.next()) {
                    double totalValue = rs.getDouble("total_inventory_value");
                    System.out.printf("\n>>> Total Inventory Value: $%.2f <<<\n", totalValue);
                }
            }

            // 2. Conditional deletion
            System.out.print("\nEnter Product ID to delete: ");
            int id = sc.nextInt();

            checkPs.setInt(1, id);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    int qty = rs.getInt("quantity");
                    if (qty == 0) {
                        deletePs.setInt(1, id);
                        deletePs.executeUpdate();
                        System.out.println("✓ Product ID " + id + " deleted successfully (Quantity was 0).");
                    } else {
                        System.out.println("✗ Cannot delete Product ID " + id + ": Quantity is " + qty + ". (Can only delete when quantity is 0).");
                    }
                } else {
                    System.out.println("✗ Product with ID " + id + " does not exist.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}