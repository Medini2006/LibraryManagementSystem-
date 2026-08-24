CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

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
