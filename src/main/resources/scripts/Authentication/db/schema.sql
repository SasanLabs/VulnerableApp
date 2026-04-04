DROP TABLE IF EXISTS auth_users;

CREATE TABLE auth_users (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    password_plain VARCHAR(500),
    password_md5 VARCHAR(500),
    password_sha1 VARCHAR(500),
    password_salted VARCHAR(500),
    email VARCHAR(100),
    role VARCHAR(20)
);

GRANT SELECT ON auth_users TO application;
