-- Demo employee login: employee@bookstore.local / ChangeMe123!
INSERT INTO accounts (email, password_hash, first_name, last_name, role)
VALUES ('employee@bookstore.local', '$2b$12$c37hnZLmP3oRD2hMRZUBfu9zBKC1oEC1Hv6..MGM41o30e85jBeIe', 'Store', 'Employee', 'EMPLOYEE');

INSERT INTO books (isbn, title, author, description, price, stock_quantity) VALUES
('978-0140449136', 'The Odyssey', 'Homer', 'The epic journey of Odysseus, translated by Robert Fagles.', 12.99, 12),
('978-0743273565', 'The Great Gatsby', 'F. Scott Fitzgerald', 'A portrait of ambition and life in the Jazz Age.', 10.50, 8),
('978-0451524935', '1984', 'George Orwell', 'A dystopian novel about surveillance and authoritarianism.', 9.99, 15),
('978-0061120084', 'To Kill a Mockingbird', 'Harper Lee', 'A story of justice and childhood in the American South.', 11.25, 10),
('978-0486280615', 'Pride and Prejudice', 'Jane Austen', 'A classic novel of manners, family, and love.', 8.75, 7);
