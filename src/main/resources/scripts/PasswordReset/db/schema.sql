DROP TABLE IF EXISTS password_reset_tokens;
DROP TABLE IF EXISTS password_reset_users;

CREATE TABLE password_reset_users (
    id INT PRIMARY KEY,
    email VARCHAR(100),
    username VARCHAR(50),
    password VARCHAR(255),
    level INT
);

CREATE TABLE password_reset_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255),
    user_email VARCHAR(100),
    level INT,
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    used BOOLEAN
);

GRANT ALL ON password_reset_users TO application;
GRANT ALL ON password_reset_tokens TO application;

