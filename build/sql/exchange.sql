
-- events --
CREATE TABLE exchange.events (
    sequenceId BIGINT NOT NULL,
    createdAt TIMESTAMP NOT NULL,
    `data` varchar(10000) NOT NULL,
    previousId BIGINT NOT NULL,
    CONSTRAINT PRI_SEQ_ID PRIMARY KEY (sequenceId),
    CONSTRAINT UNI_PREV_ID UNIQUE KEY (previousId)
)CHARACTER SET utf8 COLLATE=utf8_general_ci AUTO_INCREMENT=1000;