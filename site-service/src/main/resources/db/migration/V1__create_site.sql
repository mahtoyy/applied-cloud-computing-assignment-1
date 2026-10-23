CREATE TABLE site (
    id      BIGINT IDENTITY(1,1) PRIMARY KEY,
    code    VARCHAR(20)  NOT NULL,
    name    VARCHAR(100) NOT NULL,
    address VARCHAR(200) NULL,
    city    VARCHAR(100) NOT NULL,
    type    VARCHAR(20)  NOT NULL,
    status  VARCHAR(20)  NOT NULL,
    CONSTRAINT uq_site_code UNIQUE (code)
);
