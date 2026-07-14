CREATE TABLE policy_region_classification (
    policy_id INT PRIMARY KEY,
    region_scope VARCHAR(30) NOT NULL,
    confidence DECIMAL(5,4) NOT NULL,
    evidence_json LONGTEXT NOT NULL,
    classifier_version VARCHAR(50) NOT NULL,
    needs_review BOOLEAN NOT NULL,
    classified_at DATETIME NOT NULL,

    CONSTRAINT fk_policy_region_classification_policy
        FOREIGN KEY (policy_id)
        REFERENCES policy(id)
        ON DELETE CASCADE
);

CREATE INDEX ix_policy_region_classification_scope
    ON policy_region_classification(region_scope);

CREATE INDEX ix_policy_region_classification_version
    ON policy_region_classification(classifier_version);
