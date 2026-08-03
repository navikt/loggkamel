UPDATE oversikt SET discard_logs = FALSE WHERE discard_logs IS NULL;

ALTER TABLE oversikt
    ALTER COLUMN discard_logs SET NOT NULL;