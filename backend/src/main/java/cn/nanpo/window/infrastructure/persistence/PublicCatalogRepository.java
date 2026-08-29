package cn.nanpo.window.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.nanpo.window.api.publiccontent.PublicViews.AttractionView;
import cn.nanpo.window.api.publiccontent.PublicViews.ContactView;
import cn.nanpo.window.api.publiccontent.PublicViews.ExperienceView;
import cn.nanpo.window.api.publiccontent.PublicViews.FarmRecordView;
import cn.nanpo.window.api.publiccontent.PublicViews.FarmerDetailView;
import cn.nanpo.window.api.publiccontent.PublicViews.FarmerView;
import cn.nanpo.window.api.publiccontent.PublicViews.HomestayView;
import cn.nanpo.window.api.publiccontent.PublicViews.ProductDetailView;
import cn.nanpo.window.api.publiccontent.PublicViews.ProductSummaryView;
import cn.nanpo.window.api.publiccontent.PublicViews.SiteView;
import cn.nanpo.window.api.publiccontent.PublicViews.SkuView;
import cn.nanpo.window.api.publiccontent.PublicViews.TravelPlanView;
import cn.nanpo.window.api.publiccontent.PublicViews.TravelRouteView;
import cn.nanpo.window.api.publiccontent.PublicViews.TravelStopView;

@Repository
public class PublicCatalogRepository {

    private static final String PRODUCT_SELECT = """
            SELECT p.id, p.name, p.category, p.season_text, p.summary, p.cover_url,
                   f.id AS farmer_id, f.name AS farmer_name,
                   (SELECT MIN(s.unit_price) FROM product_sku s
                    WHERE s.product_id = p.id AND s.enabled = TRUE) AS starting_price
            FROM product p
            JOIN farmer_profile f ON f.id = p.farmer_id
            WHERE p.status = 'PUBLISHED'
              AND f.certification_status = 'APPROVED'
              AND f.status = 'ACTIVE'
            """;

    private final JdbcClient jdbc;
    private final ObjectMapper objectMapper;

    public PublicCatalogRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public Optional<SiteView> findPublishedSite() {
        Optional<SiteRow> site = jdbc.sql("""
                        SELECT id, name, province, city, county, address, summary,
                               map_keyword, recommended_season
                        FROM site_profile
                        WHERE status = 'PUBLISHED'
                        ORDER BY published_at DESC, id DESC
                        LIMIT 1
                        """)
                .query((rs, rowNum) -> new SiteRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("province"),
                        rs.getString("city"),
                        rs.getString("county"),
                        rs.getString("address"),
                        rs.getString("summary"),
                        rs.getString("map_keyword"),
                        rs.getString("recommended_season")))
                .optional();
        if (site.isEmpty()) {
            return Optional.empty();
        }
        SiteRow value = site.get();
        ContactView contact = jdbc.sql("""
                        SELECT scene, contact_name, phone, business_hours
                        FROM contact_channel
                        WHERE site_id = :siteId
                          AND scene = 'VISITOR_SERVICE'
                          AND status = 'PUBLISHED'
                          AND (enabled_from IS NULL OR enabled_from <= CURRENT_TIMESTAMP)
                          AND (disabled_at IS NULL OR disabled_at > CURRENT_TIMESTAMP)
                        ORDER BY id DESC
                        LIMIT 1
                        """)
                .param("siteId", value.id())
                .query((rs, rowNum) -> new ContactView(
                        rs.getString("scene"),
                        rs.getString("contact_name"),
                        rs.getString("phone"),
                        rs.getString("business_hours")))
                .optional()
                .orElse(null);
        return Optional.of(new SiteView(
                value.id(), value.name(), value.province(), value.city(), value.county(),
                value.address(), value.summary(), value.mapKeyword(), value.recommendedSeason(), contact));
    }

    public List<TravelRouteView> findTravelRoutes() {
        return jdbc.sql("""
                        SELECT id, route_kind, title, duration_text, note, steps_json,
                               source_name, verified_at, expires_at
                        FROM travel_route
                        WHERE status = 'PUBLISHED'
                        ORDER BY sort_order, id
                        """)
                .query((rs, rowNum) -> new TravelRouteView(
                        rs.getLong("id"),
                        rs.getString("route_kind"),
                        rs.getString("title"),
                        rs.getString("duration_text"),
                        rs.getString("note"),
                        readStringList(rs.getString("steps_json")),
                        rs.getString("source_name"),
                        localDateTime(rs, "verified_at"),
                        localDateTime(rs, "expires_at")))
                .list();
    }

    public List<AttractionView> findAttractions(int limit, int offset) {
        return jdbc.sql("""
                        SELECT id, name, category, distance_km, drive_minutes, summary,
                               cover_url, map_url, highlights_json
                        FROM attraction
                        WHERE status = 'PUBLISHED'
                        ORDER BY sort_order, id
                        LIMIT :limit OFFSET :offset
                        """)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNum) -> new AttractionView(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getBigDecimal("distance_km"),
                        rs.getInt("drive_minutes"),
                        rs.getString("summary"),
                        rs.getString("cover_url"),
                        rs.getString("map_url"),
                        readStringList(rs.getString("highlights_json"))))
                .list();
    }

    public long countAttractions() {
        return count("SELECT COUNT(*) FROM attraction WHERE status = 'PUBLISHED'");
    }

    public List<TravelPlanView> findTravelPlans() {
        return jdbc.sql("""
                        SELECT id, slug, name, duration_text, suitable_for, distance_text,
                               summary, stops_json, tips_json
                        FROM travel_plan
                        WHERE status = 'PUBLISHED'
                        ORDER BY sort_order, id
                        """)
                .query((rs, rowNum) -> new TravelPlanView(
                        rs.getLong("id"),
                        rs.getString("slug"),
                        rs.getString("name"),
                        rs.getString("duration_text"),
                        rs.getString("suitable_for"),
                        rs.getString("distance_text"),
                        rs.getString("summary"),
                        readStops(rs.getString("stops_json")),
                        readStringList(rs.getString("tips_json"))))
                .list();
    }

    public List<HomestayView> findHomestays(int limit, int offset) {
        return jdbc.sql("""
                        SELECT id, name, lodging_type, summary, capacity_text, price_text,
                               cover_url, consultation_phone
                        FROM homestay
                        WHERE status = 'PUBLISHED'
                        ORDER BY sort_order, id
                        LIMIT :limit OFFSET :offset
                        """)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNum) -> new HomestayView(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("lodging_type"),
                        rs.getString("summary"),
                        rs.getString("capacity_text"),
                        rs.getString("price_text"),
                        rs.getString("cover_url"),
                        rs.getString("consultation_phone")))
                .list();
    }

    public long countHomestays() {
        return count("SELECT COUNT(*) FROM homestay WHERE status = 'PUBLISHED'");
    }

    public List<ExperienceView> findExperiences(int limit, int offset) {
        return jdbc.sql("""
                        SELECT id, name, category, season_text, duration_text, summary,
                               price_text, cover_url, video_url, booking_notes
                        FROM experience
                        WHERE status = 'PUBLISHED'
                        ORDER BY sort_order, id
                        LIMIT :limit OFFSET :offset
                        """)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNum) -> new ExperienceView(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getString("season_text"),
                        rs.getString("duration_text"),
                        rs.getString("summary"),
                        rs.getString("price_text"),
                        rs.getString("cover_url"),
                        rs.getString("video_url"),
                        rs.getString("booking_notes")))
                .list();
    }

    public long countExperiences() {
        return count("SELECT COUNT(*) FROM experience WHERE status = 'PUBLISHED'");
    }

    public List<ProductSummaryView> findProducts(int limit, int offset) {
        return jdbc.sql(PRODUCT_SELECT + " ORDER BY p.sort_order, p.id LIMIT :limit OFFSET :offset")
                .param("limit", limit)
                .param("offset", offset)
                .query(this::mapProduct)
                .list();
    }

    public long countProducts() {
        return count("""
                SELECT COUNT(*)
                FROM product p
                JOIN farmer_profile f ON f.id = p.farmer_id
                WHERE p.status = 'PUBLISHED'
                  AND f.certification_status = 'APPROVED'
                  AND f.status = 'ACTIVE'
                """);
    }

    public Optional<ProductDetailView> findProduct(long productId) {
        Optional<ProductSummaryView> product = jdbc.sql(PRODUCT_SELECT + " AND p.id = :productId")
                .param("productId", productId)
                .query(this::mapProduct)
                .optional();
        if (product.isEmpty()) {
            return Optional.empty();
        }
        ProductSummaryView summary = product.get();
        FarmerView farmer = findFarmer(summary.farmerId()).orElseThrow();
        List<SkuView> skus = jdbc.sql("""
                        SELECT id, sku_code, specification, unit_price, stock_note
                        FROM product_sku
                        WHERE product_id = :productId AND enabled = TRUE
                        ORDER BY unit_price, id
                        """)
                .param("productId", productId)
                .query((rs, rowNum) -> new SkuView(
                        rs.getLong("id"),
                        rs.getString("sku_code"),
                        rs.getString("specification"),
                        rs.getBigDecimal("unit_price"),
                        rs.getString("stock_note")))
                .list();
        List<FarmRecordView> records = jdbc.sql("""
                        SELECT id, stage, occurred_at, confirmed_text, original_text,
                               reviewed_at, published_at
                        FROM farm_record
                        WHERE product_id = :productId AND status = 'PUBLISHED'
                        ORDER BY occurred_at, id
                        """)
                .param("productId", productId)
                .query((rs, rowNum) -> new FarmRecordView(
                        rs.getLong("id"),
                        rs.getString("stage"),
                        rs.getTimestamp("occurred_at").toLocalDateTime(),
                        rs.getString("confirmed_text") == null
                                ? rs.getString("original_text")
                                : rs.getString("confirmed_text"),
                        localDateTime(rs, "reviewed_at"),
                        localDateTime(rs, "published_at")))
                .list();
        return Optional.of(new ProductDetailView(summary, farmer, skus, records));
    }

    public Optional<FarmerView> findFarmer(long farmerId) {
        return jdbc.sql("""
                        SELECT id, farmer_code, name, village_group, introduction, certification_status
                        FROM farmer_profile
                        WHERE id = :farmerId
                          AND certification_status = 'APPROVED'
                          AND status = 'ACTIVE'
                        """)
                .param("farmerId", farmerId)
                .query((rs, rowNum) -> new FarmerView(
                        rs.getLong("id"),
                        rs.getString("farmer_code"),
                        rs.getString("name"),
                        rs.getString("village_group"),
                        rs.getString("introduction"),
                        rs.getString("certification_status")))
                .optional();
    }

    public Optional<FarmerDetailView> findFarmerDetail(long farmerId) {
        Optional<FarmerView> farmer = findFarmer(farmerId);
        if (farmer.isEmpty()) {
            return Optional.empty();
        }
        List<ProductSummaryView> products = jdbc.sql(PRODUCT_SELECT
                        + " AND p.farmer_id = :farmerId ORDER BY p.sort_order, p.id")
                .param("farmerId", farmerId)
                .query(this::mapProduct)
                .list();
        return Optional.of(new FarmerDetailView(farmer.get(), products));
    }

    private ProductSummaryView mapProduct(ResultSet rs, int rowNum) throws SQLException {
        BigDecimal startingPrice = rs.getBigDecimal("starting_price");
        return new ProductSummaryView(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("season_text"),
                rs.getString("summary"),
                rs.getString("cover_url"),
                startingPrice,
                rs.getString("farmer_name"),
                rs.getLong("farmer_id"));
    }

    private long count(String sql) {
        return jdbc.sql(sql).query(Long.class).single();
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid catalog JSON", exception);
        }
    }

    private List<TravelStopView> readStops(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid travel stop JSON", exception);
        }
    }

    private LocalDateTime localDateTime(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record SiteRow(
            long id,
            String name,
            String province,
            String city,
            String county,
            String address,
            String summary,
            String mapKeyword,
            String recommendedSeason) {
    }
}

