CREATE TABLE product_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT uk_product_image_order UNIQUE (product_id, sort_order)
);

CREATE INDEX idx_product_image_product ON product_image (product_id, sort_order, id);

INSERT INTO product_image (product_id, image_url, sort_order)
SELECT id, cover_url, 0
FROM product;
