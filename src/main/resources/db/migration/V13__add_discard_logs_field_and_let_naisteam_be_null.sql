ALTER TABLE oversikt
    ADD COLUMN IF NOT EXISTS discard_logs BOOLEAN DEFAULT false,
    ALTER COLUMN naisteam DROP NOT NULL,
    ADD CONSTRAINT naisteam_or_discard CHECK (naisteam IS NOT NULL or discard_logs IS true),
    ADD CONSTRAINT no_forwarding_types_on_discard CHECK (discard_logs is false or (okonomi is false and arkivlov is false and logging_leseoperasjoner is false))
;
