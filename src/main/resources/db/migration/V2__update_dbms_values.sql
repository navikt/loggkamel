-- ALTER TYPE dbms RENAME TO dbms_old;
--
-- CREATE TYPE dbms AS ENUM ('PostgreSQL', 'Oracle', 'DB2', 'IMS');
--
-- ALTER TABLE oversikt
--     ALTER COLUMN teknologi TYPE dbms
--         USING teknologi::text::dbms;
--
-- DROP TYPE dbms_old;

ALTER TYPE dbms RENAME VALUE 'PosgreSQL' TO 'PostgreSQL';