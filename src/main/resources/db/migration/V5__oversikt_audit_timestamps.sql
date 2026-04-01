ALTER TABLE oversikt
    ALTER COLUMN created SET DEFAULT NOW(),
    ALTER COLUMN created SET NOT NULL,
    ALTER COLUMN updated SET DEFAULT NOW(),
    ALTER COLUMN updated SET NOT NULL;

CREATE OR REPLACE FUNCTION set_oversikt_updated_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_oversikt_set_updated ON oversikt;

CREATE TRIGGER trg_oversikt_set_updated
BEFORE UPDATE ON oversikt
FOR EACH ROW
EXECUTE FUNCTION set_oversikt_updated_timestamp();

