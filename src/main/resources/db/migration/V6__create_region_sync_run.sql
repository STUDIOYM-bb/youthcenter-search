CREATE TABLE region_sync_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(40) NOT NULL,
    started_at DATETIME NOT NULL,
    completed_at DATETIME,

    api_province_count INT NOT NULL DEFAULT 0,
    api_municipality_count INT NOT NULL DEFAULT 0,

    inserted_count INT NOT NULL DEFAULT 0,
    updated_count INT NOT NULL DEFAULT 0,
    unchanged_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,

    progress_percent INT NOT NULL DEFAULT 0,
    current_province_code VARCHAR(50),
    current_province_name VARCHAR(100),

    retry_count INT NOT NULL DEFAULT 0,
    error_summary VARCHAR(1000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE region_sync_error (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sync_run_id BIGINT NOT NULL,
    province_code VARCHAR(50),
    province_name VARCHAR(100),
    error_type VARCHAR(100) NOT NULL,
    error_message VARCHAR(1000) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    occurred_at DATETIME NOT NULL,

    CONSTRAINT fk_region_sync_error_run
        FOREIGN KEY (sync_run_id)
        REFERENCES region_sync_run(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
