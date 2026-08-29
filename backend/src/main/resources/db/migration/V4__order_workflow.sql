ALTER TABLE customer_order ADD COLUMN payment_reported_at TIMESTAMP(6) NULL;
ALTER TABLE customer_order ADD COLUMN payment_report_note VARCHAR(500) NULL;
ALTER TABLE customer_order ADD COLUMN payment_confirmed_at TIMESTAMP(6) NULL;
ALTER TABLE customer_order ADD COLUMN shipping_company VARCHAR(100) NULL;
ALTER TABLE customer_order ADD COLUMN tracking_no VARCHAR(160) NULL;
ALTER TABLE customer_order ADD COLUMN shipped_at TIMESTAMP(6) NULL;
ALTER TABLE customer_order ADD COLUMN completed_at TIMESTAMP(6) NULL;
ALTER TABLE customer_order ADD COLUMN cancelled_at TIMESTAMP(6) NULL;
ALTER TABLE customer_order ADD COLUMN refund_amount DECIMAL(12,2) NULL;
ALTER TABLE customer_order ADD COLUMN refund_note VARCHAR(500) NULL;
ALTER TABLE customer_order ADD COLUMN refunded_at TIMESTAMP(6) NULL;

INSERT INTO media_asset (
    owner_user_id, media_type, storage_key, original_name,
    content_type, size_bytes, checksum_sha256, status
) VALUES (
    NULL, 'IMAGE', 'local/demo-payment-qr', 'demo-payment-qr',
    'image/svg+xml', 0, NULL, 'READY'
);

INSERT INTO payment_qr_config (
    version_no, payee_name, media_id, enabled_from, status
)
SELECT 1, '南坡村农产品服务中心（演示）', id, CURRENT_TIMESTAMP(6), 'PUBLISHED'
FROM media_asset WHERE storage_key = 'local/demo-payment-qr';
