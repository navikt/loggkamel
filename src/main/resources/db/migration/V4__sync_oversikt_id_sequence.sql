-- Ensure the identity value for oversikt.id continues from the first free key.
-- This prevents duplicate key violations when existing rows already have low IDs.
DO $$
DECLARE
    next_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) + 1
    INTO next_id
    FROM oversikt;

    EXECUTE format('ALTER TABLE oversikt ALTER COLUMN id RESTART WITH %s', next_id);
END $$;

