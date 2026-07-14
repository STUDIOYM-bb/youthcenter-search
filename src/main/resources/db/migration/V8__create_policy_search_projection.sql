CREATE TABLE policy_search_projection (
    policy_id INT PRIMARY KEY,
    source_policy_id VARCHAR(100) NOT NULL,
    normalized_title VARCHAR(500) NOT NULL,
    title_text TEXT NOT NULL,
    keyword_text TEXT,
    category_text TEXT,
    description_text LONGTEXT,
    support_text LONGTEXT,
    target_text LONGTEXT,
    qualification_text LONGTEXT,
    application_text LONGTEXT,
    institution_text TEXT,
    full_search_text LONGTEXT NOT NULL,
    projection_version VARCHAR(50) NOT NULL,
    missing_snapshot BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_policy_search_projection_policy
        FOREIGN KEY (policy_id)
        REFERENCES policy(id)
        ON DELETE CASCADE
);

CREATE INDEX ix_policy_search_projection_source_policy_id
    ON policy_search_projection(source_policy_id);

CREATE INDEX ix_policy_search_projection_version
    ON policy_search_projection(projection_version);
