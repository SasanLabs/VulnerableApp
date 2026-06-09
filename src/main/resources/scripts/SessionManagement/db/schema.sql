DROP TABLE IF EXISTS session_users;

CREATE TABLE session_users (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(100),
    role VARCHAR(20)
);

GRANT SELECT ON session_users TO application;
