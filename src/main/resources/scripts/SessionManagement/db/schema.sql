DROP TABLE IF EXISTS session_users;

CREATE TABLE session_users (
    id INT PRIMARY KEY,
    firstname VARCHAR(25) NOT NULL,
    lastname VARCHAR(25),
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_attacker BOOLEAN DEFAULT FALSE
);

GRANT SELECT ON session_users TO application;
