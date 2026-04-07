DROP TABLE IF EXISTS auth_users;

CREATE TABLE auth_users (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(500),
    salt VARCHAR(100),
    algorithm ENUM('PLAIN', 'MD5', 'SHA1', 'SHA256', 'BCRYPT'),
    level INT,
    email VARCHAR(100),
    role VARCHAR(20)
);

-- Application user has full access (for functional purposes)
GRANT ALL ON auth_users TO application;

-- A read-only user for exploration by the attacker/user
CREATE USER IF NOT EXISTS readonly_user PASSWORD 'readonly_password';
GRANT SELECT ON auth_users TO readonly_user;
