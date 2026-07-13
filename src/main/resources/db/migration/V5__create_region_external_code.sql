CREATE TABLE region_external_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    region_id INT NOT NULL,
    code_system VARCHAR(50) NOT NULL,
    external_code VARCHAR(50) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_region_external_code_region
        FOREIGN KEY (region_id)
        REFERENCES region_code(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_region_external_code
        UNIQUE (code_system, external_code),

    CONSTRAINT uk_region_external_region
        UNIQUE (region_id, code_system, external_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_region_external_region
    ON region_external_code(region_id);
