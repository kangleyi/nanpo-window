package cn.nanpo.window.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordCommand;
import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordView;
import cn.nanpo.window.api.farmer.FarmerViews.FarmerCommand;
import cn.nanpo.window.api.farmer.FarmerViews.FarmerDashboardView;
import cn.nanpo.window.api.farmer.FarmerViews.FarmerProfileView;
import cn.nanpo.window.api.farmer.FarmerViews.PlotCommand;
import cn.nanpo.window.api.farmer.FarmerViews.PlotView;
import cn.nanpo.window.api.farmer.FarmerViews.ProductCommand;
import cn.nanpo.window.api.farmer.FarmerViews.ProductManageView;
import cn.nanpo.window.api.farmer.FarmerViews.RecordMediaView;
import cn.nanpo.window.api.farmer.FarmerViews.SkuCommand;
import cn.nanpo.window.api.farmer.FarmerViews.SkuManageView;

@Repository
public class FarmerWorkspaceRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;

    public FarmerWorkspaceRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<FarmerProfileView> findFarmerByUserId(long userId) {
        return jdbc.sql("""
                        SELECT id, farmer_code, name, village_group, introduction, certification_status
                        FROM farmer_profile
                        WHERE user_id = :userId AND status = 'ACTIVE'
                        """)
                .param("userId", userId)
                .query((rs, rowNum) -> new FarmerProfileView(
                        rs.getLong("id"), rs.getString("farmer_code"), rs.getString("name"),
                        rs.getString("village_group"), rs.getString("introduction"),
                        rs.getString("certification_status")))
                .optional();
    }

    public Optional<FarmerProfileView> findFarmer(long farmerId) {
        return jdbc.sql("""
                        SELECT id, farmer_code, name, village_group, introduction, certification_status
                        FROM farmer_profile
                        WHERE id = :farmerId AND status = 'ACTIVE'
                        """)
                .param("farmerId", farmerId)
                .query((rs, rowNum) -> new FarmerProfileView(
                        rs.getLong("id"), rs.getString("farmer_code"), rs.getString("name"),
                        rs.getString("village_group"), rs.getString("introduction"),
                        rs.getString("certification_status")))
                .optional();
    }

    public List<FarmerProfileView> findActiveFarmers() {
        return jdbc.sql("""
                        SELECT id, farmer_code, name, village_group, introduction, certification_status
                        FROM farmer_profile
                        WHERE status = 'ACTIVE'
                        ORDER BY village_group, name, id
                        """)
                .query((rs, rowNum) -> new FarmerProfileView(
                        rs.getLong("id"), rs.getString("farmer_code"), rs.getString("name"),
                        rs.getString("village_group"), rs.getString("introduction"),
                        rs.getString("certification_status")))
                .list();
    }

    public long createFarmer(FarmerCommand command, String passwordHash) {
        GeneratedKeyHolder accountKeys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO user_account (phone, password_hash, display_name, status)
                    VALUES (?, ?, ?, 'ACTIVE')
                    """, new String[] { "id" });
            statement.setString(1, command.phone().trim());
            statement.setString(2, passwordHash);
            statement.setString(3, command.name().trim());
            return statement;
        }, accountKeys);
        long userId = accountKeys.getKey().longValue();
        jdbc.sql("INSERT INTO user_role (user_id, role_code) VALUES (:userId, 'FARMER')")
                .param("userId", userId)
                .update();

        GeneratedKeyHolder farmerKeys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO farmer_profile (
                        user_id, farmer_code, name, village_group, introduction,
                        certification_status, certified_at, status
                    ) VALUES (?, ?, ?, ?, ?, 'APPROVED', CURRENT_TIMESTAMP, 'ACTIVE')
                    """, new String[] { "id" });
            statement.setLong(1, userId);
            statement.setString(2, "NP-F-%03d".formatted(userId));
            statement.setString(3, command.name().trim());
            statement.setString(4, command.villageGroup().trim());
            nullableString(statement, 5, command.introduction());
            return statement;
        }, farmerKeys);
        return farmerKeys.getKey().longValue();
    }

    public FarmerDashboardView dashboard(FarmerProfileView farmer) {
        long plotCount = farmerCount("land_plot", farmer.id(), null);
        long productCount = farmerCount("product", farmer.id(), null);
        long recordCount = farmerCount("farm_record", farmer.id(), null);
        long pending = farmerCount("farm_record", farmer.id(), "PENDING_REVIEW");
        long published = farmerCount("farm_record", farmer.id(), "PUBLISHED");
        return new FarmerDashboardView(farmer, plotCount, productCount, recordCount, pending, published);
    }

    public List<PlotView> findPlots(long farmerId) {
        return jdbc.sql("""
                        SELECT id, plot_code, location_text, area_text, main_crop, cover_url, status, updated_at
                        FROM land_plot WHERE farmer_id = :farmerId ORDER BY id
                        """)
                .param("farmerId", farmerId)
                .query((rs, rowNum) -> new PlotView(
                        rs.getLong("id"), rs.getString("plot_code"), rs.getString("location_text"),
                        rs.getString("area_text"), rs.getString("main_crop"), rs.getString("cover_url"),
                        rs.getString("status"), rs.getTimestamp("updated_at").toLocalDateTime()))
                .list();
    }

    public boolean ownsPlot(long farmerId, long plotId) {
        return jdbc.sql("SELECT COUNT(*) FROM land_plot WHERE id = :plotId AND farmer_id = :farmerId")
                .param("plotId", plotId).param("farmerId", farmerId)
                .query(Long.class).single() == 1;
    }

    public long createPlot(long farmerId, PlotCommand command) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO land_plot (
                        farmer_id, plot_code, location_text, area_text, main_crop, cover_url, status
                    ) VALUES (?, ?, ?, ?, ?, ?, 'DRAFT')
                    """, new String[] { "id" });
            statement.setLong(1, farmerId);
            statement.setString(2, command.code());
            statement.setString(3, command.location());
            nullableString(statement, 4, command.area());
            nullableString(statement, 5, command.mainCrop());
            nullableString(statement, 6, command.coverUrl());
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public Optional<PlotView> findPlot(long farmerId, long id) {
        return findPlots(farmerId).stream().filter(plot -> plot.id() == id).findFirst();
    }

    public List<ProductManageView> findProducts(long farmerId) {
        List<ProductRow> products = jdbc.sql("""
                        SELECT p.id, p.land_plot_id, p.name, p.category, p.season_text, p.summary,
                               p.cover_url, p.status, p.updated_at,
                               (SELECT COUNT(*) FROM farm_record r WHERE r.product_id = p.id) AS record_count
                        FROM product p
                        WHERE p.farmer_id = :farmerId
                        ORDER BY p.updated_at DESC, p.id DESC
                        """)
                .param("farmerId", farmerId)
                .query((rs, rowNum) -> new ProductRow(
                        rs.getLong("id"), nullableLong(rs, "land_plot_id"), rs.getString("name"),
                        rs.getString("category"), rs.getString("season_text"), rs.getString("summary"),
                        rs.getString("cover_url"), rs.getString("status"), rs.getLong("record_count"),
                        rs.getTimestamp("updated_at").toLocalDateTime()))
                .list();
        return products.stream().map(product -> new ProductManageView(
                product.id(), product.plotId(), product.name(), product.category(), product.season(),
                product.summary(), product.coverUrl(), findProductImages(product.id()), product.status(), findSkus(product.id()),
                product.recordCount(), product.updatedAt())).toList();
    }

    public boolean ownsProduct(long farmerId, long productId) {
        return jdbc.sql("SELECT COUNT(*) FROM product WHERE id = :productId AND farmer_id = :farmerId")
                .param("productId", productId).param("farmerId", farmerId)
                .query(Long.class).single() == 1;
    }

    public boolean ownsSku(long productId, long skuId) {
        return jdbc.sql("SELECT COUNT(*) FROM product_sku WHERE id = :skuId AND product_id = :productId")
                .param("skuId", skuId).param("productId", productId)
                .query(Long.class).single() == 1;
    }

    public long createProduct(long farmerId, ProductCommand command) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO product (
                        farmer_id, land_plot_id, name, category, season_text,
                        summary, cover_url, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                    """, new String[] { "id" });
            statement.setLong(1, farmerId);
            nullableLong(statement, 2, command.plotId());
            statement.setString(3, command.name());
            statement.setString(4, command.category());
            statement.setString(5, command.season());
            statement.setString(6, command.summary());
            statement.setString(7, productImages(command).getFirst());
            return statement;
        }, keys);
        long productId = keys.getKey().longValue();
        replaceProductImages(productId, productImages(command));
        for (SkuCommand sku : command.skus()) {
            String code = nextSkuCode(productId);
            jdbc.sql("""
                            INSERT INTO product_sku (
                                product_id, sku_code, specification, unit_price, stock_note, enabled
                            ) VALUES (:productId, :code, :specification, :unitPrice, :stockNote, TRUE)
                            """)
                    .param("productId", productId)
                    .param("code", code)
                    .param("specification", sku.specification())
                    .param("unitPrice", sku.unitPrice())
                    .param("stockNote", sku.stockNote(), Types.VARCHAR)
                    .update();
        }
        return productId;
    }

    public boolean updateProduct(long farmerId, long productId, ProductCommand command) {
        int updated = jdbc.sql("""
                        UPDATE product
                        SET land_plot_id = :plotId, name = :name, category = :category,
                            season_text = :season, summary = :summary, cover_url = :coverUrl,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :productId AND farmer_id = :farmerId
                        """)
                .param("plotId", command.plotId(), Types.BIGINT)
                .param("name", command.name())
                .param("category", command.category())
                .param("season", command.season())
                .param("summary", command.summary())
                .param("coverUrl", productImages(command).getFirst())
                .param("productId", productId)
                .param("farmerId", farmerId)
                .update();
        if (updated == 0) {
            return false;
        }
        replaceProductImages(productId, productImages(command));
        jdbc.sql("UPDATE product_sku SET enabled = FALSE, updated_at = CURRENT_TIMESTAMP WHERE product_id = :productId")
                .param("productId", productId)
                .update();
        for (SkuCommand sku : command.skus()) {
            int skuUpdated = sku.id() == null ? 0 : jdbc.sql("""
                            UPDATE product_sku
                            SET specification = :specification, unit_price = :unitPrice,
                                stock_note = :stockNote, enabled = TRUE, updated_at = CURRENT_TIMESTAMP
                            WHERE product_id = :productId AND id = :skuId
                            """)
                    .param("specification", sku.specification())
                    .param("unitPrice", sku.unitPrice())
                    .param("stockNote", sku.stockNote(), Types.VARCHAR)
                    .param("productId", productId)
                    .param("skuId", sku.id())
                    .update();
            if (skuUpdated == 0) {
                String code = nextSkuCode(productId);
                jdbc.sql("""
                                INSERT INTO product_sku (
                                    product_id, sku_code, specification, unit_price, stock_note, enabled
                                ) VALUES (:productId, :code, :specification, :unitPrice, :stockNote, TRUE)
                                """)
                        .param("productId", productId)
                        .param("code", code)
                        .param("specification", sku.specification())
                        .param("unitPrice", sku.unitPrice())
                        .param("stockNote", sku.stockNote(), Types.VARCHAR)
                        .update();
            }
        }
        return true;
    }

    private List<String> findProductImages(long productId) {
        return jdbc.sql("""
                        SELECT image_url
                        FROM product_image
                        WHERE product_id = :productId
                        ORDER BY sort_order, id
                        """)
                .param("productId", productId)
                .query(String.class)
                .list();
    }

    private List<String> productImages(ProductCommand command) {
        return command.imageUrls() == null || command.imageUrls().isEmpty()
                ? List.of(command.coverUrl())
                : command.imageUrls().stream().distinct().toList();
    }

    private void replaceProductImages(long productId, List<String> imageUrls) {
        jdbc.sql("DELETE FROM product_image WHERE product_id = :productId")
                .param("productId", productId)
                .update();
        for (int index = 0; index < imageUrls.size(); index++) {
            jdbc.sql("""
                            INSERT INTO product_image (product_id, image_url, sort_order)
                            VALUES (:productId, :imageUrl, :sortOrder)
                            """)
                    .param("productId", productId)
                    .param("imageUrl", imageUrls.get(index))
                    .param("sortOrder", index)
                    .update();
        }
    }

    private String nextSkuCode(long productId) {
        for (int sequence = 1; sequence <= 9999; sequence++) {
            String code = "SKU-%06d-%02d".formatted(productId, sequence);
            long count = jdbc.sql("SELECT COUNT(*) FROM product_sku WHERE sku_code = :code")
                    .param("code", code).query(Long.class).single();
            if (count == 0) return code;
        }
        throw new IllegalStateException("无法为农产品生成新的规格编码");
    }

    public boolean setProductPublished(long farmerId, long productId, boolean published) {
        String status = published ? "PUBLISHED" : "DRAFT";
        return jdbc.sql("""
                        UPDATE product
                        SET status = :status,
                            published_at = CASE WHEN :status = 'PUBLISHED' THEN CURRENT_TIMESTAMP ELSE NULL END,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :productId AND farmer_id = :farmerId
                        """)
                .param("status", status)
                .param("productId", productId)
                .param("farmerId", farmerId)
                .update() == 1;
    }

    public Optional<ProductManageView> findProduct(long farmerId, long id) {
        return findProducts(farmerId).stream().filter(product -> product.id() == id).findFirst();
    }

    public List<FarmRecordView> findRecords(long farmerId, String status) {
        List<FarmRecordView> records = jdbc.sql(RECORD_SELECT + """
                        WHERE r.farmer_id = :farmerId
                          AND (:status = 'ALL' OR r.status = :status)
                        ORDER BY r.occurred_at DESC, r.id DESC
                        """)
                .param("farmerId", farmerId)
                .param("status", status)
                .query(this::mapRecord)
                .list();
        return records.stream().map(this::withMedia).toList();
    }

    public List<FarmRecordView> findReviewQueue(String status) {
        List<FarmRecordView> records = jdbc.sql(RECORD_SELECT + """
                        WHERE (:status = 'ALL' OR r.status = :status)
                        ORDER BY r.created_at, r.id
                        """)
                .param("status", status)
                .query(this::mapRecord)
                .list();
        return records.stream().map(this::withMedia).toList();
    }

    public Optional<FarmRecordView> findRecord(long farmerId, long id) {
        return jdbc.sql(RECORD_SELECT + " WHERE r.id = :id AND r.farmer_id = :farmerId")
                .param("id", id).param("farmerId", farmerId)
                .query(this::mapRecord).optional().map(this::withMedia);
    }

    public Optional<FarmRecordView> findRecord(long id) {
        return jdbc.sql(RECORD_SELECT + " WHERE r.id = :id")
                .param("id", id).query(this::mapRecord).optional().map(this::withMedia);
    }

    private List<RecordMediaView> findRecordMedia(long recordId) {
        return jdbc.sql("""
                        SELECT m.id, m.media_type, m.original_name, m.content_type, m.status
                        FROM record_media rm JOIN media_asset m ON m.id = rm.media_id
                        WHERE rm.record_id = :recordId
                        ORDER BY rm.sort_order, m.id
                        """)
                .param("recordId", recordId)
                .query((rs, rowNum) -> new RecordMediaView(
                        rs.getLong("id"), rs.getString("media_type"), rs.getString("original_name"),
                        rs.getString("content_type"), rs.getString("status"),
                        "READY".equals(rs.getString("status")) ? "/api/media/" + rs.getLong("id") + "/content" : null))
                .list();
    }

    public long createRecord(long farmerId, FarmRecordCommand command) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO farm_record (
                        farmer_id, product_id, land_plot_id, stage, occurred_at,
                        original_text, truth_confirmed, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                    """, new String[] { "id" });
            statement.setLong(1, farmerId);
            statement.setLong(2, command.productId());
            nullableLong(statement, 3, command.plotId());
            statement.setString(4, command.stage());
            statement.setTimestamp(5, Timestamp.valueOf(command.occurredAt()));
            statement.setString(6, command.originalText());
            statement.setBoolean(7, command.truthConfirmed());
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public boolean submitRecord(long farmerId, long id, long version) {
        return jdbc.sql("""
                        UPDATE farm_record
                        SET status = 'PENDING_REVIEW', review_note = NULL,
                            reviewer_user_id = NULL, reviewed_at = NULL,
                            version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND farmer_id = :farmerId AND version = :version
                          AND status IN ('DRAFT', 'REJECTED') AND truth_confirmed = TRUE
                        """)
                .param("id", id).param("farmerId", farmerId).param("version", version)
                .update() == 1;
    }

    public boolean approveRecord(
            long id, long version, long reviewerUserId, String confirmedText, String reviewNote) {
        return jdbc.sql("""
                        UPDATE farm_record
                        SET status = 'PUBLISHED', confirmed_text = :confirmedText,
                            review_note = :reviewNote, reviewer_user_id = :reviewerUserId,
                            reviewed_at = CURRENT_TIMESTAMP, published_at = CURRENT_TIMESTAMP,
                            version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version AND status = 'PENDING_REVIEW'
                        """)
                .param("confirmedText", confirmedText)
                .param("reviewNote", reviewNote, Types.VARCHAR)
                .param("reviewerUserId", reviewerUserId)
                .param("id", id).param("version", version)
                .update() == 1;
    }

    public boolean rejectRecord(long id, long version, long reviewerUserId, String reviewNote) {
        return jdbc.sql("""
                        UPDATE farm_record
                        SET status = 'REJECTED', review_note = :reviewNote,
                            reviewer_user_id = :reviewerUserId, reviewed_at = CURRENT_TIMESTAMP,
                            published_at = NULL, version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version AND status = 'PENDING_REVIEW'
                        """)
                .param("reviewNote", reviewNote)
                .param("reviewerUserId", reviewerUserId)
                .param("id", id).param("version", version)
                .update() == 1;
    }

    private List<SkuManageView> findSkus(long productId) {
        return jdbc.sql("""
                        SELECT id, sku_code, specification, unit_price, stock_note, enabled
                        FROM product_sku WHERE product_id = :productId ORDER BY id
                        """)
                .param("productId", productId)
                .query((rs, rowNum) -> new SkuManageView(
                        rs.getLong("id"), rs.getString("sku_code"), rs.getString("specification"),
                        rs.getBigDecimal("unit_price"), rs.getString("stock_note"), rs.getBoolean("enabled")))
                .list();
    }

    private FarmRecordView mapRecord(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new FarmRecordView(
                rs.getLong("id"), rs.getLong("product_id"), rs.getString("product_name"),
                nullableLong(rs, "land_plot_id"), rs.getString("plot_code"), rs.getString("stage"),
                rs.getTimestamp("occurred_at").toLocalDateTime(), rs.getString("original_text"),
                rs.getString("confirmed_text"), rs.getBoolean("truth_confirmed"), rs.getString("status"),
                rs.getString("review_note"), nullableLong(rs, "reviewer_user_id"),
                localDateTime(rs.getTimestamp("reviewed_at")), localDateTime(rs.getTimestamp("published_at")),
                rs.getLong("version"), rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(), List.of());
    }

    private FarmRecordView withMedia(FarmRecordView record) {
        return new FarmRecordView(
                record.id(), record.productId(), record.productName(), record.plotId(), record.plotCode(),
                record.stage(), record.occurredAt(), record.originalText(), record.confirmedText(),
                record.truthConfirmed(), record.status(), record.reviewNote(), record.reviewerUserId(),
                record.reviewedAt(), record.publishedAt(), record.version(), record.createdAt(), record.updatedAt(),
                findRecordMedia(record.id()));
    }

    private long farmerCount(String table, long farmerId, String status) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE farmer_id = :farmerId"
                + (status == null ? "" : " AND status = :status");
        JdbcClient.StatementSpec statement = jdbc.sql(sql).param("farmerId", farmerId);
        if (status != null) {
            statement.param("status", status);
        }
        return statement.query(Long.class).single();
    }

    private void nullableString(PreparedStatement statement, int index, String value) throws java.sql.SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private void nullableLong(PreparedStatement statement, int index, Long value) throws java.sql.SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static final String RECORD_SELECT = """
            SELECT r.id, r.product_id, p.name AS product_name, r.land_plot_id, lp.plot_code,
                   r.stage, r.occurred_at, r.original_text, r.confirmed_text, r.truth_confirmed,
                   r.status, r.review_note, r.reviewer_user_id, r.reviewed_at, r.published_at,
                   r.version, r.created_at, r.updated_at
            FROM farm_record r
            JOIN product p ON p.id = r.product_id
            LEFT JOIN land_plot lp ON lp.id = r.land_plot_id
            """;

    private record ProductRow(
            long id, Long plotId, String name, String category, String season,
            String summary, String coverUrl, String status, long recordCount, LocalDateTime updatedAt) {
    }
}
