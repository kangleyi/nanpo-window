UPDATE user_account
SET password_hash = '$2y$10$a4ZNBzLVs4fNLhQejXPnluu7.eTLcIxaG.xse2PTpT2giuOnylt3q',
    updated_at = CURRENT_TIMESTAMP;

UPDATE auth_session
SET revoked_at = CURRENT_TIMESTAMP
WHERE revoked_at IS NULL;
