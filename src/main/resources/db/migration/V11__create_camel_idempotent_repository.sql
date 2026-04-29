CREATE TABLE CAMEL_MESSAGEPROCESSED (
    processorName VARCHAR(255) NOT NULL,
    messageId     VARCHAR(1000) NOT NULL,
    createdAt     TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (processorName, messageId)
);

