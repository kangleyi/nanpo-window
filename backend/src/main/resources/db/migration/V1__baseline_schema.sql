CREATE TABLE user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(32) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_user_account_phone UNIQUE (phone)
);

CREATE TABLE role (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_id, role_code),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES user_account (id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_code) REFERENCES role (code)
);

CREATE TABLE auth_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    access_token_hash CHAR(64) NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL,
    access_expires_at TIMESTAMP(6) NOT NULL,
    refresh_expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_auth_session_access UNIQUE (access_token_hash),
    CONSTRAINT uk_auth_session_refresh UNIQUE (refresh_token_hash),
    CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE INDEX idx_auth_session_user ON auth_session (user_id, revoked_at);

CREATE TABLE operation_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    object_type VARCHAR(100) NOT NULL,
    object_id VARCHAR(100) NULL,
    before_value TEXT NULL,
    after_value TEXT NULL,
    ip_address VARCHAR(64) NULL,
    request_id VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE INDEX idx_audit_object ON operation_audit_log (object_type, object_id, created_at);

CREATE TABLE site_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    province VARCHAR(80) NOT NULL,
    city VARCHAR(80) NOT NULL,
    county VARCHAR(80) NOT NULL,
    address VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    map_keyword VARCHAR(255) NOT NULL,
    recommended_season VARCHAR(100) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE contact_channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    site_id BIGINT NOT NULL,
    scene VARCHAR(64) NOT NULL,
    contact_name VARCHAR(100) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    business_hours VARCHAR(120) NULL,
    enabled_from TIMESTAMP(6) NULL,
    disabled_at TIMESTAMP(6) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_contact_site FOREIGN KEY (site_id) REFERENCES site_profile (id)
);

CREATE TABLE travel_route (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    route_kind VARCHAR(32) NOT NULL,
    title VARCHAR(160) NOT NULL,
    duration_text VARCHAR(100) NOT NULL,
    note TEXT NOT NULL,
    steps_json TEXT NOT NULL,
    source_name VARCHAR(160) NULL,
    verified_at TIMESTAMP(6) NULL,
    expires_at TIMESTAMP(6) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_travel_route_kind UNIQUE (route_kind)
);

CREATE TABLE attraction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(100) NOT NULL,
    distance_km DECIMAL(8,2) NOT NULL,
    drive_minutes INT NOT NULL,
    summary TEXT NOT NULL,
    cover_url VARCHAR(500) NOT NULL,
    map_url VARCHAR(1000) NOT NULL,
    highlights_json TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_attraction_public ON attraction (status, sort_order, id);

CREATE TABLE travel_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    slug VARCHAR(100) NOT NULL,
    name VARCHAR(160) NOT NULL,
    duration_text VARCHAR(100) NOT NULL,
    suitable_for VARCHAR(255) NOT NULL,
    distance_text VARCHAR(160) NOT NULL,
    summary TEXT NOT NULL,
    stops_json TEXT NOT NULL,
    tips_json TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_travel_plan_slug UNIQUE (slug)
);

CREATE TABLE homestay (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    lodging_type VARCHAR(100) NOT NULL,
    summary TEXT NOT NULL,
    capacity_text VARCHAR(100) NOT NULL,
    price_text VARCHAR(100) NOT NULL,
    cover_url VARCHAR(500) NOT NULL,
    consultation_phone VARCHAR(32) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_homestay_public ON homestay (status, sort_order, id);

CREATE TABLE experience (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(100) NOT NULL,
    season_text VARCHAR(100) NOT NULL,
    duration_text VARCHAR(100) NOT NULL,
    summary TEXT NOT NULL,
    price_text VARCHAR(100) NOT NULL,
    cover_url VARCHAR(500) NOT NULL,
    video_url VARCHAR(500) NULL,
    booking_notes TEXT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE INDEX idx_experience_public ON experience (status, sort_order, id);

CREATE TABLE farmer_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    farmer_code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    village_group VARCHAR(100) NOT NULL,
    introduction TEXT NULL,
    certification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    certified_at TIMESTAMP(6) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_farmer_code UNIQUE (farmer_code),
    CONSTRAINT uk_farmer_user UNIQUE (user_id),
    CONSTRAINT fk_farmer_user FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE TABLE land_plot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    farmer_id BIGINT NOT NULL,
    plot_code VARCHAR(64) NOT NULL,
    location_text VARCHAR(255) NOT NULL,
    area_text VARCHAR(100) NULL,
    main_crop VARCHAR(100) NULL,
    cover_url VARCHAR(500) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_land_plot_code UNIQUE (plot_code),
    CONSTRAINT fk_plot_farmer FOREIGN KEY (farmer_id) REFERENCES farmer_profile (id)
);

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    farmer_id BIGINT NOT NULL,
    land_plot_id BIGINT NULL,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(100) NOT NULL,
    season_text VARCHAR(100) NOT NULL,
    summary TEXT NOT NULL,
    cover_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_farmer FOREIGN KEY (farmer_id) REFERENCES farmer_profile (id),
    CONSTRAINT fk_product_plot FOREIGN KEY (land_plot_id) REFERENCES land_plot (id)
);

CREATE INDEX idx_product_public ON product (status, sort_order, id);
CREATE INDEX idx_product_farmer ON product (farmer_id, status);

CREATE TABLE product_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    specification VARCHAR(160) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    stock_note VARCHAR(255) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_product_sku_code UNIQUE (sku_code),
    CONSTRAINT fk_sku_product FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE TABLE farm_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    farmer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    land_plot_id BIGINT NULL,
    stage VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    original_text TEXT NOT NULL,
    confirmed_text TEXT NULL,
    truth_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    reviewed_at TIMESTAMP(6) NULL,
    published_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_record_farmer FOREIGN KEY (farmer_id) REFERENCES farmer_profile (id),
    CONSTRAINT fk_record_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_record_plot FOREIGN KEY (land_plot_id) REFERENCES land_plot (id)
);

CREATE INDEX idx_record_public ON farm_record (product_id, status, occurred_at);

CREATE TABLE media_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NULL,
    media_type VARCHAR(32) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    original_name VARCHAR(255) NULL,
    content_type VARCHAR(160) NOT NULL,
    size_bytes BIGINT NOT NULL,
    duration_seconds INT NULL,
    checksum_sha256 CHAR(64) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_media_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_media_owner FOREIGN KEY (owner_user_id) REFERENCES user_account (id)
);

CREATE TABLE record_media (
    record_id BIGINT NOT NULL,
    media_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (record_id, media_id),
    CONSTRAINT fk_record_media_record FOREIGN KEY (record_id) REFERENCES farm_record (id),
    CONSTRAINT fk_record_media_asset FOREIGN KEY (media_id) REFERENCES media_asset (id)
);

CREATE TABLE ai_generation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    scene VARCHAR(64) NOT NULL,
    source_refs_json TEXT NOT NULL,
    model_name VARCHAR(160) NOT NULL,
    model_version VARCHAR(100) NULL,
    output_text TEXT NOT NULL,
    confirmed_text TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    confirmed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_ai_owner FOREIGN KEY (owner_user_id) REFERENCES user_account (id)
);

CREATE TABLE payment_qr_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    version_no INT NOT NULL,
    payee_name VARCHAR(160) NOT NULL,
    media_id BIGINT NOT NULL,
    enabled_from TIMESTAMP(6) NOT NULL,
    disabled_at TIMESTAMP(6) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_payment_qr_version UNIQUE (version_no),
    CONSTRAINT fk_payment_qr_media FOREIGN KEY (media_id) REFERENCES media_asset (id)
);

CREATE TABLE customer_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL,
    customer_user_id BIGINT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone VARCHAR(32) NOT NULL,
    recipient_address VARCHAR(500) NOT NULL,
    customer_note VARCHAR(500) NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    payment_qr_snapshot TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_customer_order_no UNIQUE (order_no),
    CONSTRAINT uk_customer_order_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_user_id) REFERENCES user_account (id)
);

CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    farmer_id BIGINT NOT NULL,
    product_snapshot TEXT NOT NULL,
    sku_snapshot TEXT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    line_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES customer_order (id),
    CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_order_item_sku FOREIGN KEY (sku_id) REFERENCES product_sku (id),
    CONSTRAINT fk_order_item_farmer FOREIGN KEY (farmer_id) REFERENCES farmer_profile (id)
);

CREATE TABLE order_status_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    operator_user_id BIGINT NULL,
    reason VARCHAR(500) NULL,
    request_id VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_order_log_order FOREIGN KEY (order_id) REFERENCES customer_order (id),
    CONSTRAINT fk_order_log_operator FOREIGN KEY (operator_user_id) REFERENCES user_account (id)
);

CREATE INDEX idx_order_status ON customer_order (status, created_at);
CREATE INDEX idx_order_log_order ON order_status_log (order_id, created_at);

CREATE TABLE system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(160) NOT NULL,
    config_value TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_system_config_key UNIQUE (config_key)
);

