USE mybank;

DROP TABLE IF EXISTS accounts;

CREATE TABLE accounts (
    account_number INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    mobile BIGINT,
    email VARCHAR(100),
    address VARCHAR(200),
    date VARCHAR(50),
    balance DOUBLE
);

