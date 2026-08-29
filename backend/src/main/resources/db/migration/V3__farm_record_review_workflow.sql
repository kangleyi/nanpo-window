ALTER TABLE farm_record ADD COLUMN review_note TEXT NULL;
ALTER TABLE farm_record ADD COLUMN reviewer_user_id BIGINT NULL;
ALTER TABLE farm_record ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE farm_record
    ADD CONSTRAINT fk_record_reviewer
    FOREIGN KEY (reviewer_user_id) REFERENCES user_account (id);

CREATE INDEX idx_record_review_queue ON farm_record (status, created_at);
