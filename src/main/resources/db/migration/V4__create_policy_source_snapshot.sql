CREATE TABLE policy_source_snapshot (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  policy_id INT NOT NULL,
  raw_data_id BIGINT,
  source VARCHAR(50) NOT NULL,
  source_policy_id VARCHAR(150) NOT NULL,
  raw_policy_json LONGTEXT NOT NULL,
  raw_content_hash VARCHAR(64) NOT NULL,
  collected_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,

  CONSTRAINT fk_policy_source_snapshot_policy
    FOREIGN KEY (policy_id)
    REFERENCES policy(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_policy_source_snapshot_raw_data
    FOREIGN KEY (raw_data_id)
    REFERENCES policy_raw_data(id)
    ON DELETE SET NULL,

  CONSTRAINT uk_policy_source_snapshot_policy
    UNIQUE (policy_id),

  CONSTRAINT uk_policy_source_snapshot_source
    UNIQUE (source, source_policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_policy_source_snapshot_source
ON policy_source_snapshot (source, source_policy_id);
