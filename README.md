
## 📚 Library Management System (JDBC)

### Project Overview
The **Library Management System (JDBC)** is a robust, console-based Java application designed to automate the core operations of a library. Built as an advanced-level internship project, it leverages **Java Database Connectivity (JDBC)** to interface with a **MySQL** relational database, ensuring persistent storage and data integrity.

The system provides a complete solution for managing library resources, user records, and borrowing transactions through an intuitive command-line interface. It is engineered to handle real-world scenarios, including concurrent access and complex business rules.

---

### Core Features & Functionality

#### 📖 Book Management
The system offers a full suite of CRUD (Create, Read, Update, Delete) operations for books. Users can add new books with unique ISBN validation, view the complete catalog with real-time availability status, update book details (title, author, ISBN), and delete books. Crucially, deletion is only permitted if no copies of the book are currently borrowed, preventing data loss and maintaining historical integrity.

#### 👤 User Management
Similarly, the system manages library patrons with complete CRUD capabilities. It supports adding new users with unique email validation, displaying all registered members with their membership dates, updating user information, and removing users. The system enforces a critical rule: a user cannot be deleted if they have any active loans, ensuring accountability.

#### 📋 Transaction & Borrowing Logic
The most sophisticated aspect of the system is its handling of borrow and return transactions. When a user borrows a book, the system automatically decrements the `available_copies`, creates a transaction record with a `BORROWED` status, and timestamps the event. Returning a book reverses this process: it increments the availability and updates the transaction status to `RETURNED` with a return date. The system also provides a complete transaction history, displaying all past activities with book titles, user names, and dates.

---

### Technical Architecture & Design

#### Database Schema (Relational Design)
The backend is structured around three interconnected tables, enforcing referential integrity through foreign keys:

- **Books**: Stores book metadata (`id`, `title`, `author`, `isbn`, `total_copies`, `available_copies`).
- **Users**: Stores patron details (`id`, `name`, `email`, `phone`, `membership_date`) with a unique constraint on email.
- **Transactions**: Acts as a junction table, linking books and users. It records (`borrow_date`, `return_date`, `status`) for every borrow/return cycle.

#### ACID Compliance & Concurrency Control
A key engineering highlight is the implementation of **ACID (Atomicity, Consistency, Isolation, Durability)** principles. The system uses manual transaction management (`setAutoCommit(false)`) combined with explicit `commit()` and `rollback()` calls. To prevent race conditions during high-concurrency scenarios—such as two users attempting to borrow the last available copy simultaneously—the system employs **row-level locking** via `SELECT ... FOR UPDATE`. This ensures that availability checks and stock updates happen atomically, guaranteeing data consistency.

#### Error Handling & Defensive Programming
The application is built with resilience in mind. It features comprehensive input validation (handling empty strings, negative numbers, malformed emails, and invalid data types) and user-friendly error messaging. Specific exceptions, such as `SQLIntegrityConstraintViolationException`, are caught and translated into clear feedback for the user, preventing raw stack traces from reaching the console.

#### Resource Management
The code adheres to best practices by utilizing **try-with-resources** statements, ensuring that all database connections, statements, and result sets are automatically closed after use. This prevents memory leaks and maintains optimal performance during extended usage.

---

### Project Context & Achievement
This project was developed as the **Level 3 (Advanced)** submission for the **Codveda Technology Internship**. It successfully demonstrates the ability to design a relational database schema, implement full CRUD operations via JDBC, and manage complex transactional workflows. The system goes beyond basic requirements by enforcing strict business rules and providing a stable, production-ready foundation suitable for further expansion.
