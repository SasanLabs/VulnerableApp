DROP TABLE IF EXISTS idor_users;

CREATE TABLE idor_users (
    id INT PRIMARY KEY,
    uuid VARCHAR(36),
    username VARCHAR(50),
    password VARCHAR(500),
    salary INT, 
    role VARCHAR(20),
    level INT
);

GRANT SELECT, UPDATE ON idor_users TO application;