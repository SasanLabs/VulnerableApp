DROP TABLE IF EXISTS csrf_accounts_l1;
DROP TABLE IF EXISTS csrf_accounts_l2;
DROP TABLE IF EXISTS csrf_accounts_l3;
DROP TABLE IF EXISTS csrf_accounts_l4;
DROP TABLE IF EXISTS csrf_accounts_l5;

CREATE TABLE csrf_accounts_l1 (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(500),
    email VARCHAR(100)
);

CREATE TABLE csrf_accounts_l2 (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(500),
    email VARCHAR(100)
);

CREATE TABLE csrf_accounts_l3 (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(500),
    email VARCHAR(100)
);

CREATE TABLE csrf_accounts_l4 (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(500),
    email VARCHAR(100)
);

CREATE TABLE csrf_accounts_l5 (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(500),
    email VARCHAR(100)
);

GRANT SELECT, UPDATE ON csrf_accounts_l1 TO application;
GRANT SELECT, UPDATE ON csrf_accounts_l2 TO application;
GRANT SELECT, UPDATE ON csrf_accounts_l3 TO application;
GRANT SELECT, UPDATE ON csrf_accounts_l4 TO application;
GRANT SELECT, UPDATE ON csrf_accounts_l5 TO application;