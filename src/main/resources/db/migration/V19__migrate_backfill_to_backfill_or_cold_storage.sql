-- Order matters: the old constraint must go before rows get new values,
-- and the column must be wide enough ('cold-storage-completed' is 22 characters) before any update.
-- Manual rollback (loses the requested/completed distinction for cold storage):
--   drop the new constraint, UPDATE backfill to 'requested' / 'finished' / 'false'
--   (cold-storage-requested and cold-storage-completed both become 'false'),
--   narrow the column to VARCHAR(9), set DEFAULT 'false', re-add the V18 constraint.

ALTER TABLE oversikt
    DROP CONSTRAINT oversikt_backfill_valid;

ALTER TABLE oversikt
    ALTER COLUMN backfill TYPE VARCHAR(22);

UPDATE oversikt
SET backfill = CASE backfill
    WHEN 'finished' THEN 'backfill-completed'
    WHEN 'requested' THEN 'backfill-requested'
    WHEN 'false' THEN 'cold-storage-completed'
END;

ALTER TABLE oversikt
    ALTER COLUMN backfill SET DEFAULT 'cold-storage-completed';

ALTER TABLE oversikt
    ADD CONSTRAINT oversikt_backfill_valid
        CHECK (backfill IN (
            'backfill-requested',
            'backfill-completed',
            'cold-storage-requested',
            'cold-storage-completed'
        ));
