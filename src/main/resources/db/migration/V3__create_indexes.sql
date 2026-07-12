CREATE INDEX ix_policy_source ON policy (source_type, source_policy_id);
CREATE INDEX ix_policy_status ON policy (status, is_active, due_date);
CREATE INDEX ix_policy_title ON policy (title);
CREATE INDEX ix_policy_region_region ON policy_region (region_id);
CREATE INDEX ix_region_parent ON region_code (parent_id);
CREATE INDEX ix_policy_raw_source ON policy_raw_data (source, source_policy_id, collected_at);
CREATE INDEX ix_collection_run_source ON policy_collection_run (source, started_at);
CREATE INDEX ix_embedding_status ON policy_embedding_sync (sync_status, requested_at);
CREATE INDEX ix_policy_collection_error_run ON policy_collection_error (collection_run_id, occurred_at);
