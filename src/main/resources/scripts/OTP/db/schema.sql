DROP TABLE IF EXISTS otp_tokens;
DROP TABLE IF EXISTS otp_users;

CREATE TABLE otp_users (
    id INT PRIMARY KEY,
    email VARCHAR(100),
    level VARCHAR(50)
);

CREATE TABLE otp_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100),
    otp_hash VARCHAR(255),
    level VARCHAR(50),
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    attempts INT,
    used BOOLEAN
);

GRANT ALL ON otp_users TO application;
GRANT ALL ON otp_tokens TO application;