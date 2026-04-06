DROP TABLE IF EXISTS auth_users;

CREATE TABLE auth_users (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(500),
    salt VARCHAR(100),
    algorithm VARCHAR(20),
    level INT,
    email VARCHAR(100),
    role VARCHAR(20)
);

GRANT SELECT ON auth_users TO application;
