ALTER TABLE camel_messageprocessed
    ALTER COLUMN createdat TYPE TIMESTAMPTZ
    USING createdat AT TIME ZONE 'Europe/Oslo';

ALTER TABLE camel_messageprocessed
    ALTER COLUMN createdat SET DEFAULT NOW();
