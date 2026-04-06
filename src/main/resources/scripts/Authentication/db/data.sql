-- Level 1: SQL Injection
INSERT INTO auth_users VALUES (1, 'admin_sqli', 'p@ssword_sqli', NULL, 'PLAIN', 1, 'admin_sqli@example.com', 'ADMIN');

-- Level 2: Sensitive Data Logging
INSERT INTO auth_users VALUES (2, 'admin_logs', 'p@ssword_logs', NULL, 'PLAIN', 2, 'admin_logs@example.com', 'ADMIN');

-- Level 3: Plaintext Storage
INSERT INTO auth_users VALUES (3, 'admin_plain', 'p@ssword_plain', NULL, 'PLAIN', 3, 'admin_plain@example.com', 'ADMIN');

-- Level 4: MD5 Hashing (p@ssword_md5)
INSERT INTO auth_users VALUES (4, 'admin_md5', '6796fac55d9d9361664bbd916ac177f1', NULL, 'MD5', 4, 'admin_md5@example.com', 'ADMIN');

-- Level 5: SHA1 Hashing (p@ssword_sha1)
INSERT INTO auth_users VALUES (5, 'admin_sha1', '6dbcc14392476d05988220bc0a86f9f38fca6583', NULL, 'SHA-1', 5, 'admin_sha1@example.com', 'ADMIN');

-- Level 6: SHA-256 (No Salt) (p@ssword_sha256)
INSERT INTO auth_users VALUES (6, 'admin_sha256', '8364e66d9dd41053c847d06692994775d79634d05f320b5220c4c23f39d5e3ef', NULL, 'SHA-256', 6, 'admin_sha256@example.com', 'ADMIN');

-- Level 7: Salted SHA-256 (p@ssword_enum)
INSERT INTO auth_users VALUES (7, 'admin_enum', '5e14930be6281734f46487e8348705ce883441968875560947de04e1e834417a', 'salt_enum', 'SHA-256', 7, 'admin_enum@example.com', 'ADMIN');

-- Level 8: Weak Password + Bcrypt (password123)
-- Bcrypt hash for 'password123'
INSERT INTO auth_users VALUES (8, 'admin_weak', '$2b$12$RibCfNMArIOXx7Bbu2zbfu9XgCaffDUgTFgonMVw4Cw/lW9GOUDcK', NULL, 'BCRYPT', 8, 'admin_weak@example.com', 'ADMIN');

-- Level 9: Secure (Bcrypt + Generic Error) (SasanLabs_Secure_2026!)
-- Bcrypt hash for 'SasanLabs_Secure_2026!'
INSERT INTO auth_users VALUES (9, 'admin_secure', '$2b$12$lM07jK4UaRBIMIT1k1DipOAbjS9/YaOfKrbfFPxFR15NDIfCPyRve', NULL, 'BCRYPT', 9, 'admin_secure@example.com', 'ADMIN');
