DROP TABLE IF EXISTS graphql_users;
DROP TABLE IF EXISTS graphql_orders;

CREATE TABLE graphql_users (
    id INT PRIMARY KEY,
    username VARCHAR(50),
    email VARCHAR(100),
    salary INT,
    role VARCHAR(20)
);

CREATE TABLE graphql_orders (
    id INT PRIMARY KEY,
    user_id INT,
    item VARCHAR(100),
    amount INT
);

GRANT SELECT, UPDATE, DELETE ON graphql_users TO application;
GRANT SELECT ON graphql_orders TO application;
