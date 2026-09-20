-- En production, VulnerableAppConfiguration creates the 'application' H2 user before
-- running schema.sql/data.sql (which GRANT rights to that user). This script replicates that
-- step for the embedded database used by AuthSeedDataRegressionTest.
CREATE USER IF NOT EXISTS application PASSWORD 'application';