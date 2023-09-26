-- init DataBase

DROP DATABASE IF EXISTS exchange;

CREATE DATABASE exchange;

USE exchange;

CREATE TABLE events (
    id INTEGER NOT NULL,
    createAt BIGINT NOT NULL,
    data VARCHAR(10000) NOT NULL,
    previousId BIGINT NOT NULL,
    sequencerId BIGINT NOT NULL,
    CONSTRAINT UNI_PREV_ID UNIQUE (previousId),
    PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE match_details (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account BIGINT NOT NULL,
    counterAccount BIGINT NOT NULL,
    counterOrderId BIGINT NOT NULL,
    createdAt BIGINT NOT NULL,
    direction VARCHAR(32) NOT NULL,
    orderId BIGINT NOT NULL,
    price DECIMAL(0,0) NOT NULL,
    quantity DECIMAL(0,0) NOT NULL,
    sequenceId BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    CONSTRAINT UNI_OID_COID UNIQUE (orderId, counterOrderId),
    INDEX IDX_OID_CT (orderId, createdAt),
    PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE orders (
    id BIGINT NOT NULL,
    createdAt BIGINT NOT NULL,
    direction VARCHAR(32) NOT NULL,
    price DECIMAL(36,18) NOT NULL,
    quantity DECIMAL(36,18) NOT NULL,
    sequenceId BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    unfilledQuantity DECIMAL(36,18) NOT NULL,
    updateAt BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;


CREATE TABLE ticks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    createdAt BIGINT NOT NULL,
    makerOrderId BIGINT NOT NULL,
    price DECIMAL(0,0) NOT NULL,
    quantity DECIMAL(0,0) NOT NULL,
    sequenceId BIGINT NOT NULL,
    takerDirection BIT NOT NULL,
    takerOrderId BIGINT NOT NULL,
    CONSTRAINT UNI_T_M UNIQUE (takerOrderId, makerOrderId),
    INDEX IDX_CT (createdAt),
    PRIMARY KEY(id)
) CHARACTER SET utf8 COLLATE utf8_general_ci AUTO_INCREMENT = 1000;