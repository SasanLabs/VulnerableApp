-- schema.sql of SQLInjection creates CARS without dropping it first, so drop it here to let every
-- test method start from the seed data again.
DROP TABLE IF EXISTS CARS;
