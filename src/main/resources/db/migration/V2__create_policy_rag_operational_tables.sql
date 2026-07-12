CREATE TABLE policy_raw_data (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source VARCHAR(50) NOT NULL,
  source_policy_id VARCHAR(150),
  request_url VARCHAR(1000) NOT NULL,
  request_parameters JSON,
  response_body LONGTEXT NOT NULL,
  response_format VARCHAR(20) NOT NULL,
  parse_status VARCHAR(30) NOT NULL,
  error_message VARCHAR(1000),
  collected_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE policy_collection_run (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source VARCHAR(50) NOT NULL,
  execution_type VARCHAR(30) NOT NULL,
  started_at DATETIME NOT NULL,
  completed_at DATETIME,
  status VARCHAR(30) NOT NULL,
  requested_page_count INT NOT NULL DEFAULT 0,
  api_request_count INT NOT NULL DEFAULT 0,
  received_count INT NOT NULL DEFAULT 0,
  inserted_count INT NOT NULL DEFAULT 0,
  updated_count INT NOT NULL DEFAULT 0,
  skipped_count INT NOT NULL DEFAULT 0,
  failed_count INT NOT NULL DEFAULT 0,
  representative_error VARCHAR(1000),
  failed_page INT,
  retry_count INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE policy_collection_error (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  collection_run_id BIGINT,
  raw_data_id BIGINT,
  source VARCHAR(50) NOT NULL,
  failed_page INT,
  source_policy_id VARCHAR(150),
  error_type VARCHAR(100) NOT NULL,
  error_message VARCHAR(1000) NOT NULL,
  occurred_at DATETIME NOT NULL,
  CONSTRAINT fk_collection_error_run FOREIGN KEY (collection_run_id) REFERENCES policy_collection_run(id) ON DELETE SET NULL,
  CONSTRAINT fk_collection_error_raw FOREIGN KEY (raw_data_id) REFERENCES policy_raw_data(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE policy_embedding_sync (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  policy_id INT NOT NULL,
  content_hash VARCHAR(64),
  sync_status VARCHAR(30) NOT NULL,
  last_error VARCHAR(1000),
  retry_count INT NOT NULL DEFAULT 0,
  requested_at DATETIME NOT NULL,
  synced_at DATETIME,
  CONSTRAINT fk_embedding_sync_policy FOREIGN KEY (policy_id) REFERENCES policy(id) ON DELETE CASCADE,
  CONSTRAINT uk_embedding_policy UNIQUE (policy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
