UPDATE oversikt
SET okonomi = COALESCE(okonomi, false),
    arkivlov = COALESCE(arkivlov, false),
    logging_leseoperasjoner = COALESCE(logging_leseoperasjoner, false),
    fiksa = COALESCE(fiksa, false),
    funnet_logger = COALESCE(funnet_logger, false);

ALTER TABLE oversikt
    ALTER COLUMN okonomi SET DEFAULT false,
    ALTER COLUMN arkivlov SET DEFAULT false,
    ALTER COLUMN logging_leseoperasjoner SET DEFAULT false,
    ALTER COLUMN fiksa SET DEFAULT false,
    ALTER COLUMN funnet_logger SET DEFAULT false,
    ALTER COLUMN okonomi SET NOT NULL,
    ALTER COLUMN arkivlov SET NOT NULL,
    ALTER COLUMN logging_leseoperasjoner SET NOT NULL,
    ALTER COLUMN fiksa SET NOT NULL,
    ALTER COLUMN funnet_logger SET NOT NULL;

