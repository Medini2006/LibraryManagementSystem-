import java.sql.*;
import java.util.*;

/**
 * Library Management System with JDBC.
 * Features: CRUD for Books & Users, Borrow/Return with transactional integrity.
 * Includes Sri Lankan names in sample data.
 */
public class LibraryManagementSystem {

    // Local XAMPP MySQL setup
    private static final String DB_NAME = "library_db";
    private static final String DB_BASE_URL = "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&allowMultiQueries=true&serverTimezone=UTC";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/" + DB_NAME + "?useSSL=false&allowPublicKeyRetrieval=true&allowMultiQueries=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private static Connection connection = null;
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            initializeDatabase();
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[OK] Connected to the Library Database.");

            // 2. Main menu loop
            boolean exit = false;
            while (!exit) {
                displayMenu();
                int choice = getIntInput("Enter your choice: ");
                switch (choice) {
                    case 1 -> addBook();
                    case 2 -> viewAllBooks();
                    case 3 -> updateBook();
                    case 4 -> deleteBook();
                    case 5 -> addUser();
                    case 6 -> viewAllUsers();
                    case 7 -> updateUser();
                    case 8 -> deleteUser();
                    case 9 -> borrowBook();
                    case 10 -> returnBook();
                    case 11 -> viewTransactions();
                    case 0 -> { exit = true; System.out.println("[BYE] Goodbye!"); }
                    default -> System.out.println("[ERROR] Invalid choice. Try again.");
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] MySQL JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[ERROR] Database connection error: " + e.getMessage());
        } finally {
            try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
        }
    }

    private static void initializeDatabase() throws SQLException {
        try (Connection initConn = DriverManager.getConnection(DB_BASE_URL, DB_USER, DB_PASSWORD);
             Statement stmt = initConn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
        }

        try (Connection initConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = initConn.createStatement()) {
            String schemaSql = """
                CREATE TABLE IF NOT EXISTS Books (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(200) NOT NULL,
                    author VARCHAR(200) NOT NULL,
                    isbn VARCHAR(50) UNIQUE NOT NULL,
                    total_copies INT NOT NULL DEFAULT 1,
                    available_copies INT NOT NULL DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );

                CREATE TABLE IF NOT EXISTS Users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(200) NOT NULL,
                    email VARCHAR(200) UNIQUE NOT NULL,
                    phone VARCHAR(50) NOT NULL,
                    membership_date DATE NOT NULL DEFAULT (CURRENT_DATE)
                );

                CREATE TABLE IF NOT EXISTS Transactions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    book_id INT NOT NULL,
                    user_id INT NOT NULL,
                    borrow_date DATE NOT NULL,
                    return_date DATE NULL,
                    status ENUM('BORROWED', 'RETURNED') NOT NULL DEFAULT 'BORROWED',
                    CONSTRAINT fk_transactions_book FOREIGN KEY (book_id) REFERENCES Books(id) ON DELETE CASCADE,
                    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES Users(id) ON DELETE CASCADE
                );

                INSERT INTO Books (title, author, isbn, total_copies, available_copies)
                VALUES
                    ('The Alchemist', 'Paulo Coelho', '9780061122415', 3, 3),
                    ('Clean Code', 'Robert C. Martin', '9780132350884', 2, 2),
                    ('Harry Potter', 'J.K. Rowling', '9780747532699', 4, 4)
                ON DUPLICATE KEY UPDATE
                    title = VALUES(title),
                    author = VALUES(author),
                    total_copies = VALUES(total_copies),
                    available_copies = VALUES(available_copies);

                INSERT INTO Users (name, email, phone)
                VALUES
                    ('Nimal Silva', 'nimal.silva@example.com', '0771234567'),
                    ('Kasun Perera', 'kasun.perera@example.com', '0712345678')
                ON DUPLICATE KEY UPDATE
                    name = VALUES(name),
                    phone = VALUES(phone);
                """;
            stmt.execute(schemaSql);
        }
    }

    private static void displayMenu() {
        System.out.println("\n[LIBRARY] === LIBRARY MANAGEMENT SYSTEM ===");
        System.out.println("1.  Add Book");
        System.out.println("2.  View All Books");
        System.out.println("3.  Update Book");
        System.out.println("4.  Delete Book");
        System.out.println("5.  Add User");
        System.out.println("6.  View All Users");
        System.out.println("7.  Update User");
        System.out.println("8.  Delete User");
        System.out.println("9.  Borrow Book");
        System.out.println("10. Return Book");
        System.out.println("11. View Transactions");
        System.out.println("0.  Exit");
    }

    // ---------- Helper Methods ----------
    private static int getIntInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("[ERROR] Please enter a number: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // consume newline
        return value;
    }

    private static String getStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static boolean bookExists(int bookId) {
        String sql = "SELECT 1 FROM Books WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Error checking book existence: " + e.getMessage());
            return false;
        }
    }

    private static boolean userExists(int userId) {
        String sql = "SELECT 1 FROM Users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Error checking user existence: " + e.getMessage());
            return false;
        }
    }

    private static int getActiveBorrowCount(int bookId, int userId) {
        String sql = "SELECT COUNT(*) AS active_count FROM Transactions WHERE book_id = ? AND user_id = ? AND status = 'BORROWED'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("active_count");
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Error checking borrow count: " + e.getMessage());
        }
        return 0;
    }

    private static boolean getActiveBorrowCountForBook(int bookId) {
        String sql = "SELECT COUNT(*) AS active_count FROM Transactions WHERE book_id = ? AND status = 'BORROWED'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("active_count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Error checking book borrow status: " + e.getMessage());
        }
        return false;
    }

    private static boolean userHasActiveBorrow(int userId) {
        String sql = "SELECT COUNT(*) AS active_count FROM Transactions WHERE user_id = ? AND status = 'BORROWED'";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("active_count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Error checking user borrow status: " + e.getMessage());
        }
        return false;
    }

    // ---------- Book CRUD ----------
    private static void addBook() {
        System.out.println("\n[BOOK] Add a New Book");
        String title = getStringInput("Title: ");
        String author = getStringInput("Author: ");
        String isbn = getStringInput("ISBN: ");
        int totalCopies;
        while (true) {
            totalCopies = getIntInput("Total Copies: ");
            if (totalCopies > 0) break;
            System.out.println("[ERROR] Total copies must be greater than 0.");
        }

        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            System.out.println("[ERROR] Title, author, and ISBN cannot be blank.");
            return;
        }

        String sql = "INSERT INTO Books (title, author, isbn, total_copies, available_copies) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, author);
            pstmt.setString(3, isbn);
            pstmt.setInt(4, totalCopies);
            pstmt.setInt(5, totalCopies); // initially available = total
            int affected = pstmt.executeUpdate();
            if (affected > 0) System.out.println("[OK] Book added successfully!");
        } catch (SQLException e) {
            System.err.println("[ERROR] Error adding book: " + e.getMessage());
        }
    }

    private static void viewAllBooks() {
        System.out.println("\n[BOOK] All Books");
        String sql = "SELECT id, title, author, isbn, total_copies, available_copies FROM Books";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("%-5s %-30s %-25s %-15s %-8s %-8s%n", "ID", "Title", "Author", "ISBN", "Total", "Avail.");
            while (rs.next()) {
                System.out.printf("%-5d %-30s %-25s %-15s %-8d %-8d%n",
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getInt("total_copies"),
                        rs.getInt("available_copies"));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Error retrieving books: " + e.getMessage());
        }
    }

    private static void updateBook() {
        System.out.println("\n[EDIT] Update Book");
        int id = getIntInput("Enter Book ID to update: ");
        String title = getStringInput("New Title (leave blank to keep): ");
        String author = getStringInput("New Author (leave blank to keep): ");
        String isbn = getStringInput("New ISBN (leave blank to keep): ");
        String totalCopiesStr = getStringInput("New Total Copies (negative to keep): ");

        if (!bookExists(id)) {
            System.out.println("[WARN] Book ID not found.");
            return;
        }

        StringBuilder sql = new StringBuilder("UPDATE Books SET ");
        List<Object> params = new ArrayList<>();
        if (!title.isEmpty()) { sql.append("title = ?, "); params.add(title); }
        if (!author.isEmpty()) { sql.append("author = ?, "); params.add(author); }
        if (!isbn.isEmpty()) { sql.append("isbn = ?, "); params.add(isbn); }

        if (!totalCopiesStr.isEmpty()) {
            try {
                int total = Integer.parseInt(totalCopiesStr);
                if (total >= 0) {
                    sql.append("total_copies = ?, available_copies = available_copies + (? - total_copies), ");
                    params.add(total);
                    params.add(total);
                } else {
                    System.out.println("[ERROR] Total copies must be zero or greater.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] Total copies must be a valid number.");
                return;
            }
        }
        if (params.isEmpty()) {
            System.out.println("[WARN] No fields to update.");
            return;
        }
        sql.delete(sql.length() - 2, sql.length());
        sql.append(" WHERE id = ?");
        params.add(id);

        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            int affected = pstmt.executeUpdate();
            if (affected > 0) System.out.println("[OK] Book updated successfully!");
            else System.out.println("[WARN] Book ID not found.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Error updating book: " + e.getMessage());
        }
    }

    private static void deleteBook() {
        System.out.println("\n[DELETE] Delete Book");
        int id = getIntInput("Enter Book ID to delete: ");

        if (getActiveBorrowCountForBook(id)) {
            System.out.println("[ERROR] Cannot delete a book that is currently borrowed.");
            return;
        }

        String sql = "DELETE FROM Books WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int affected = pstmt.executeUpdate();
            if (affected > 0) System.out.println("[OK] Book deleted successfully!");
            else System.out.println("[WARN] Book ID not found.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Error deleting book: " + e.getMessage());
        }
    }

    // ---------- User CRUD ----------
    private static void addUser() {
        System.out.println("\n[USER] Add a New User");
        String name = getStringInput("Full Name: ");
        String email = getStringInput("Email: ");
        String phone = getStringInput("Phone: ");

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            System.out.println("[ERROR] Name, email, and phone cannot be blank.");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            System.out.println("[ERROR] Please enter a valid email address.");
            return;
        }

        String sql = "INSERT INTO Users (name, email, phone) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, phone);
            int affected = pstmt.executeUpdate();
            if (affected > 0) System.out.println("[OK] User added successfully!");
        } catch (SQLException e) {
            System.err.println("[ERROR] Error adding user: " + e.getMessage());
        }
    }

    private static void viewAllUsers() {
        System.out.println("\n[USER] All Users");
        String sql = "SELECT id, name, email, phone, membership_date FROM Users";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("%-5s %-20s %-30s %-15s %-15s%n", "ID", "Name", "Email", "Phone", "Member Since");
            while (rs.next()) {
                System.out.printf("%-5d %-20s %-30s %-15s %-15s%n",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getDate("membership_date"));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Error retrieving users: " + e.getMessage());
        }
    }

    private static void updateUser() {
        System.out.println("\n[EDIT] Update User");
        int id = getIntInput("Enter User ID to update: ");
        String name = getStringInput("New Name (leave blank to keep): ");
        String email = getStringInput("New Email (leave blank to keep): ");
        String phone = getStringInput("New Phone (leave blank to keep): ");

        if (!userExists(id)) {
            System.out.println("[WARN] User ID not found.");
            return;
        }

        StringBuilder sql = new StringBuilder("UPDATE Users SET ");
        List<Object> params = new ArrayList<>();
        if (!name.isEmpty()) { sql.append("name = ?, "); params.add(name); }
        if (!email.isEmpty()) {
            if (!email.contains("@") || !email.contains(".")) {
                System.out.println("[ERROR] Please enter a valid email address.");
                return;
            }
            sql.append("email = ?, "); params.add(email);
        }
        if (!phone.isEmpty()) { sql.append("phone = ?, "); params.add(phone); }
        if (params.isEmpty()) {
            System.out.println("[WARN] No fields to update.");
            return;
        }
        sql.delete(sql.length() - 2, sql.length());
        sql.append(" WHERE id = ?");
        params.add(id);

        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            int affected = pstmt.executeUpdate();
            if (affected > 0) System.out.println("[OK] User updated successfully!");
            else System.out.println("[WARN] User ID not found.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Error updating user: " + e.getMessage());
        }
    }

    private static void deleteUser() {
        System.out.println("\n[DELETE] Delete User");
        int id = getIntInput("Enter User ID to delete: ");

        if (userHasActiveBorrow(id)) {
            System.out.println("[ERROR] Cannot delete a user with active book loans.");
            return;
        }

        String sql = "DELETE FROM Users WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int affected = pstmt.executeUpdate();
            if (affected > 0) System.out.println("[OK] User deleted successfully!");
            else System.out.println("[WARN] User ID not found.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Error deleting user: " + e.getMessage());
        }
    }

    // ---------- Borrow / Return ----------
    private static void borrowBook() {
        System.out.println("\n[BOOK] Borrow a Book");
        int bookId = getIntInput("Enter Book ID: ");
        int userId = getIntInput("Enter User ID: ");

        if (!bookExists(bookId)) {
            System.out.println("[WARN] Book not found.");
            return;
        }
        if (!userExists(userId)) {
            System.out.println("[WARN] User not found.");
            return;
        }
        if (getActiveBorrowCount(bookId, userId) > 0) {
            System.out.println("[ERROR] This user already has this book borrowed.");
            return;
        }

        try {
            connection.setAutoCommit(false);

            String checkSql = "SELECT available_copies FROM Books WHERE id = ? FOR UPDATE";
            int available = 0;
            try (PreparedStatement pstmt = connection.prepareStatement(checkSql)) {
                pstmt.setInt(1, bookId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    available = rs.getInt("available_copies");
                }
            }

            if (available <= 0) {
                System.out.println("[ERROR] No available copies for this book.");
                connection.rollback();
                return;
            }

            String updateBookSql = "UPDATE Books SET available_copies = available_copies - 1 WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateBookSql)) {
                pstmt.setInt(1, bookId);
                pstmt.executeUpdate();
            }

            String insertTransSql = "INSERT INTO Transactions (book_id, user_id, borrow_date, status) VALUES (?, ?, ?, 'BORROWED')";
            try (PreparedStatement pstmt = connection.prepareStatement(insertTransSql)) {
                pstmt.setInt(1, bookId);
                pstmt.setInt(2, userId);
                pstmt.setDate(3, new java.sql.Date(System.currentTimeMillis()));
                pstmt.executeUpdate();
            }

            connection.commit();
            System.out.println("[OK] Book borrowed successfully!");

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            System.err.println("[ERROR] Error borrowing book: " + e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private static void returnBook() {
        System.out.println("\n[BOOK] Return a Book");
        int bookId = getIntInput("Enter Book ID: ");
        int userId = getIntInput("Enter User ID: ");

        if (!bookExists(bookId)) {
            System.out.println("[WARN] Book not found.");
            return;
        }
        if (!userExists(userId)) {
            System.out.println("[WARN] User not found.");
            return;
        }

        try {
            connection.setAutoCommit(false);

            String findSql = "SELECT id FROM Transactions WHERE book_id = ? AND user_id = ? AND status = 'BORROWED' FOR UPDATE";
            int transId = -1;
            try (PreparedStatement pstmt = connection.prepareStatement(findSql)) {
                pstmt.setInt(1, bookId);
                pstmt.setInt(2, userId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    transId = rs.getInt("id");
                } else {
                    System.out.println("[WARN] No active borrow record found for this user and book.");
                    connection.rollback();
                    return;
                }
            }

            String updateTransSql = "UPDATE Transactions SET return_date = ?, status = 'RETURNED' WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateTransSql)) {
                pstmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
                pstmt.setInt(2, transId);
                pstmt.executeUpdate();
            }

            String updateBookSql = "UPDATE Books SET available_copies = available_copies + 1 WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(updateBookSql)) {
                pstmt.setInt(1, bookId);
                pstmt.executeUpdate();
            }

            connection.commit();
            System.out.println("[OK] Book returned successfully!");

        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            System.err.println("[ERROR] Error returning book: " + e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    private static void viewTransactions() {
        System.out.println("\n[TRANSACTIONS] Transaction History");
        String sql = "SELECT t.id, b.title AS book_title, u.name AS user_name, " +
                     "t.borrow_date, t.return_date, t.status " +
                     "FROM Transactions t " +
                     "JOIN Books b ON t.book_id = b.id " +
                     "JOIN Users u ON t.user_id = u.id " +
                     "ORDER BY t.borrow_date DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.printf("%-5s %-30s %-20s %-12s %-12s %-10s%n",
                    "ID", "Book", "User", "Borrow Date", "Return Date", "Status");
            while (rs.next()) {
                System.out.printf("%-5d %-30s %-20s %-12s %-12s %-10s%n",
                        rs.getInt("id"),
                        rs.getString("book_title"),
                        rs.getString("user_name"),
                        rs.getDate("borrow_date"),
                        rs.getDate("return_date"),
                        rs.getString("status"));
            }
        } catch (SQLException e) {
            System.err.println("[ERROR] Error retrieving transactions: " + e.getMessage());
        }
    }
}