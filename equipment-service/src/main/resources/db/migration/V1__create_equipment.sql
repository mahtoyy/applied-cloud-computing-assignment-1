CREATE TABLE equipment (
    id            BIGINT IDENTITY(1,1) PRIMARY KEY,
    hostname      VARCHAR(100) NOT NULL,
    vendor        VARCHAR(50)  NOT NULL,
    model         VARCHAR(50)  NOT NULL,
    serial_number VARCHAR(50)  NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    ip_address    VARCHAR(45)  NULL,
    site_id       BIGINT       NOT NULL,
    CONSTRAINT uq_equipment_hostname UNIQUE (hostname),
    CONSTRAINT uq_equipment_serial UNIQUE (serial_number)
);

CREATE INDEX ix_equipment_site ON equipment(site_id);
