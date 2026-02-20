CREATE TABLE testlogger (
    id SERIAL PRIMARY KEY,
    logTime TIMESTAMP NOT NULL,
    logQuery VARCHAR(100),
    logUser VARCHAR(50) NOT NULL
);

INSERT INTO testlogger (logTime, logQuery, logUser)
VALUES (now(), 'UPDATE something', 'v-oidc-A123456-unixtime-'),
       (now(), 'INSERT something', 'B234567');