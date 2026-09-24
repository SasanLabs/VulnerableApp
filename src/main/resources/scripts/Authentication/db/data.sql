-- Level 1: SQL Injection
-- Real password: 'not_needed_for_sqli'
INSERT INTO auth_users VALUES (1, 'admin_sqli', 'not_needed_for_sqli', NULL, 'PLAIN', 1, 'admin_sqli@example.com', 'ADMIN');

-- Level 2: Sensitive Data Logging
-- Real password: 'v9K#2mLp!8zQ'
INSERT INTO auth_users VALUES (2, 'admin_logs', 'v9K#2mLp!8zQ', NULL, 'PLAIN', 2, 'admin_logs@example.com', 'ADMIN');

-- Level 3: Plaintext Storage
-- Real password: 'b7X$4nRj-6mW'
INSERT INTO auth_users VALUES (3, 'admin_plain', 'b7X$4nRj-6mW', NULL, 'PLAIN', 3, 'admin_plain@example.com', 'ADMIN');

-- Level 4: MD5 Hashing (SaKyb21.20#)
INSERT INTO auth_users VALUES (4, 'admin_md5', '84b2db8c58e7b24eeeab1ced704aed8a', NULL, 'MD5', 4, 'admin_md5@example.com', 'ADMIN');

-- Level 5: SHA1 Hashing (WyXxb@i34)
INSERT INTO auth_users VALUES (5, 'admin_sha1', 'cc9f65e23e5531b877559f24efa19ee0f54168f3', NULL, 'SHA1', 5, 'admin_sha1@example.com', 'ADMIN');

-- Level 6: SHA-256 (No Salt) (QoTz8!bM52)
INSERT INTO auth_users VALUES (6, 'admin_sha256', '06991471fe08cd4cac889046b89d9da3643260cade37c4c923c20289efc498e1', NULL, 'SHA256', 6, 'admin_sha256@example.com', 'ADMIN');

-- Level 7: Salted SHA-256 (RuPq3#kW76 with Salt s9A#2zLk)
INSERT INTO auth_users VALUES (7, 'admin_enum', '0a81e6daca6e13eb4f3ecba40a7fe9d234768d11a60ea855fabdac493221cfcb', 's9A#2zLk', 'SHA256', 7, 'admin_enum@example.com', 'ADMIN');

-- Level 8: Weak Password + Bcrypt (password123)
-- Bcrypt hash for 'password123'
INSERT INTO auth_users VALUES (8, 'admin_weak', '$2a$10$gV2vZ5fxhZlwOP.GIqOI1.z7q5jws8VDmgIcKqY/uzvhzSUDio2sW', NULL, 'BCRYPT', 8, 'admin_weak@example.com', 'ADMIN');

-- Level 9: Secure (Bcrypt + Generic Error) (9fG#2hJk*LmN!8qR)
-- Bcrypt hash for '9fG#2hJk*LmN!8qR'
INSERT INTO auth_users VALUES (9, 'admin_secure', '$2a$10$1WiFUNqUY/vHTzR2QtuMQuzCLK3aZEdjEUpqS4msXOevaCz7Wobe.', NULL, 'BCRYPT', 9, 'admin_secure@example.com', 'ADMIN');

-- Level 10: Low-iteration BCrypt (cost factor 4)
-- Bcrypt hash (cost 4) for the common password 'sunshine'
INSERT INTO auth_users VALUES (10, 'admin_lowcost', '$2a$04$rK/CT/Bz7GjjGLnB3WWjTOpMpNcGJzmoh.bdc7gQJ4DBQnKj9xnHC', NULL, 'BCRYPT_LOW_ITERATION', 10, 'admin_lowcost@example.com', 'ADMIN');
