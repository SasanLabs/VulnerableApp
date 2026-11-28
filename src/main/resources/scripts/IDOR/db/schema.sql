DROP TABLE IF EXISTS idor_users;

-- type tells which levels a user belongs to: ID users are referenced by their numeric id in
-- levels 1 to 5, UUID users by their uuid in levels 6 to 8. Every query filters on it, so a
-- level can never read or update the users of the other group.
CREATE TABLE idor_users (
    id INT PRIMARY KEY,
    uuid VARCHAR(36),
    username VARCHAR(50),
    password VARCHAR(500),
    salary INT, 
    role VARCHAR(20),
    type VARCHAR(10)
);

GRANT SELECT, UPDATE ON idor_users TO application;
