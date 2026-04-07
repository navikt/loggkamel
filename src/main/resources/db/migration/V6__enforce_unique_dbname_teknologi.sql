ALTER TABLE oversikt
    ALTER COLUMN teknologi SET NOT NULL;

ALTER TABLE oversikt
    ADD CONSTRAINT oversikt_dbname_teknologi_unique UNIQUE (dbname, teknologi);

