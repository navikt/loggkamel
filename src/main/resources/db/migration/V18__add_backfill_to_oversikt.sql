ALTER TABLE oversikt
    ADD COLUMN backfill VARCHAR(9) NOT NULL DEFAULT 'false';

UPDATE oversikt
SET backfill = CASE
    WHEN teknologi = 'POSTGRESQL' THEN 'finished'
    ELSE 'requested'
END;

ALTER TABLE oversikt
    ADD CONSTRAINT oversikt_backfill_valid
        CHECK (backfill IN ('false', 'requested', 'finished'));
