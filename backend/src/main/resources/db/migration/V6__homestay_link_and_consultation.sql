ALTER TABLE homestay ADD COLUMN external_url VARCHAR(1000) NULL;

CREATE TABLE consultation_inquiry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    target_name VARCHAR(160) NOT NULL,
    visit_at TIMESTAMP(6) NOT NULL,
    party_size INT NOT NULL,
    callback_phone VARCHAR(32) NOT NULL,
    note TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_consultation_inquiry_admin
    ON consultation_inquiry (status, source_type, created_at);
