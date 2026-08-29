ALTER TABLE media_asset ADD COLUMN expires_at TIMESTAMP(6) NULL;
ALTER TABLE media_asset ADD COLUMN uploaded_at TIMESTAMP(6) NULL;
ALTER TABLE media_asset ADD COLUMN failure_reason VARCHAR(500) NULL;
ALTER TABLE media_asset ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_media_owner_status ON media_asset (owner_user_id, status, created_at);

ALTER TABLE ai_generation ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_ai_owner_scene ON ai_generation (owner_user_id, scene, created_at);
