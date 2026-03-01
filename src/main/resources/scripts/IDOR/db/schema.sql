DROP TABLE IF EXISTS idor_users;

CREATE TABLE idor_users (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(50),
    salary INT, 
    role VARCHAR(20)
);

GRANT SELECT ON idor_users TO application;