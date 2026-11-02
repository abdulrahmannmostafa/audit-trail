ALTER TABLE audit_log
    ADD COLUMN occurred_at TIMESTAMP(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6) AFTER event_id,
    ADD COLUMN actor_id VARCHAR(100) NOT NULL
        DEFAULT 'system' AFTER occurred_at,
    ADD COLUMN actor_username VARCHAR(255) NULL AFTER actor_id,
    ADD COLUMN outcome VARCHAR(20) NOT NULL
        DEFAULT 'SUCCESS' AFTER entity_id,
    ADD COLUMN consumed_at TIMESTAMP(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6) AFTER metadata;

CREATE INDEX idx_audit_actor_time
    ON audit_log (actor_id, occurred_at);

CREATE INDEX idx_audit_action_time
    ON audit_log (action, occurred_at);
