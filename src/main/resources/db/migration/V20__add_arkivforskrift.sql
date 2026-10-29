-- Manual rollback: drop oversikt_endringer_uten_krav_valid, restore the previous
-- no_forwarding_types_on_discard constraint without arkivforskrift, then drop the column.
-- Restore the backfill default to 'cold-storage-completed'.
-- The correction to endringer_uten_krav cannot be reversed without a backup.
ALTER TABLE oversikt
    ADD COLUMN arkivforskrift BOOLEAN NOT NULL DEFAULT false,
    ALTER COLUMN backfill SET DEFAULT 'backfill-completed';

UPDATE oversikt
SET endringer_uten_krav = false
WHERE okonomi IS true AND endringer_uten_krav IS true;

ALTER TABLE oversikt
    ADD CONSTRAINT oversikt_endringer_uten_krav_valid
        CHECK (NOT endringer_uten_krav OR (NOT okonomi AND NOT arkivforskrift)),
    DROP CONSTRAINT no_forwarding_types_on_discard,
    ADD CONSTRAINT no_forwarding_types_on_discard
        CHECK (NOT discard_logs OR (
            NOT okonomi AND NOT arkivforskrift
            AND NOT endringer_uten_krav AND NOT logging_leseoperasjoner
        ));
