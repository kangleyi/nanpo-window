package cn.nanpo.window.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.nanpo.window.api.admin.AdminContentViews.AttractionAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.AttractionCommand;
import cn.nanpo.window.api.admin.AdminContentViews.ContentStatusView;
import cn.nanpo.window.api.admin.AdminContentViews.ExperienceAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.ExperienceCommand;
import cn.nanpo.window.api.admin.AdminContentViews.HomestayAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.HomestayCommand;
import cn.nanpo.window.api.admin.AdminContentViews.GoodsSectionAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.GoodsSectionCommand;

@Repository
public class ContentAdminRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ContentAdminRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<HomestayAdminView> findHomestays(String status, int limit, int offset) {
        return jdbc.sql("""
                        SELECT id, name, lodging_type, summary, capacity_text, price_text,
                               cover_url, consultation_phone, external_url, sort_order, status, published_at, updated_at
                        FROM homestay
                        WHERE (:status = 'ALL' OR status = :status)
                        ORDER BY sort_order, id
                        LIMIT :limit OFFSET :offset
                        """)
                .param("status", status)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNum) -> new HomestayAdminView(
                        rs.getLong("id"), rs.getString("name"), rs.getString("lodging_type"),
                        rs.getString("summary"), rs.getString("capacity_text"), rs.getString("price_text"),
                        rs.getString("cover_url"), rs.getString("consultation_phone"), rs.getString("external_url"),
                        rs.getInt("sort_order"),
                        rs.getString("status"), localDateTime(rs.getTimestamp("published_at")),
                        localDateTime(rs.getTimestamp("updated_at"))))
                .list();
    }

    public Optional<GoodsSectionAdminView> findGoodsSection() {
        return jdbc.sql("""
                        SELECT id, goods_eyebrow, goods_title, goods_description,
                               goods_season_label, goods_season_note, goods_image_url,
                               goods_image_caption, updated_at
                        FROM site_profile
                        WHERE status = 'PUBLISHED'
                        ORDER BY published_at DESC, id DESC
                        LIMIT 1
                        """)
                .query((rs, rowNum) -> new GoodsSectionAdminView(
                        rs.getLong("id"), rs.getString("goods_eyebrow"), rs.getString("goods_title"),
                        rs.getString("goods_description"), rs.getString("goods_season_label"),
                        rs.getString("goods_season_note"), rs.getString("goods_image_url"),
                        rs.getString("goods_image_caption"), localDateTime(rs.getTimestamp("updated_at"))))
                .optional();
    }

    public int updateGoodsSection(long siteId, GoodsSectionCommand command) {
        return jdbc.sql("""
                        UPDATE site_profile
                        SET goods_eyebrow = :eyebrow, goods_title = :title,
                            goods_description = :description, goods_season_label = :seasonLabel,
                            goods_season_note = :seasonNote, goods_image_url = :imageUrl,
                            goods_image_caption = :imageCaption, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :siteId AND status = 'PUBLISHED'
                        """)
                .param("eyebrow", command.eyebrow())
                .param("title", command.title())
                .param("description", command.description())
                .param("seasonLabel", command.seasonLabel())
                .param("seasonNote", command.seasonNote())
                .param("imageUrl", command.imageUrl())
                .param("imageCaption", command.imageCaption())
                .param("siteId", siteId)
                .update();
    }

    public long countHomestays(String status) {
        return count("homestay", status);
    }

    public Optional<HomestayAdminView> findHomestay(long id) {
        return jdbc.sql("""
                        SELECT id, name, lodging_type, summary, capacity_text, price_text,
                               cover_url, consultation_phone, external_url, sort_order, status, published_at, updated_at
                        FROM homestay WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new HomestayAdminView(
                        rs.getLong("id"), rs.getString("name"), rs.getString("lodging_type"),
                        rs.getString("summary"), rs.getString("capacity_text"), rs.getString("price_text"),
                        rs.getString("cover_url"), rs.getString("consultation_phone"), rs.getString("external_url"),
                        rs.getInt("sort_order"),
                        rs.getString("status"), localDateTime(rs.getTimestamp("published_at")),
                        localDateTime(rs.getTimestamp("updated_at"))))
                .optional();
    }

    public long createHomestay(HomestayCommand command) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO homestay (
                        name, lodging_type, summary, capacity_text, price_text,
                        cover_url, consultation_phone, external_url, sort_order, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                    """, new String[] { "id" });
            statement.setString(1, command.name());
            statement.setString(2, command.type());
            statement.setString(3, command.summary());
            statement.setString(4, command.capacity());
            statement.setString(5, command.price());
            statement.setString(6, command.coverUrl());
            nullableString(statement, 7, command.consultationPhone());
            nullableString(statement, 8, command.externalUrl());
            statement.setInt(9, sortOrder(command.sortOrder()));
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public int updateHomestay(long id, HomestayCommand command) {
        return jdbc.sql("""
                        UPDATE homestay
                        SET name = :name, lodging_type = :type, summary = :summary,
                            capacity_text = :capacity, price_text = :price, cover_url = :coverUrl,
                            consultation_phone = :phone, external_url = :externalUrl, sort_order = :sortOrder,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id
                        """)
                .param("name", command.name())
                .param("type", command.type())
                .param("summary", command.summary())
                .param("capacity", command.capacity())
                .param("price", command.price())
                .param("coverUrl", command.coverUrl())
                .param("phone", command.consultationPhone(), Types.VARCHAR)
                .param("externalUrl", command.externalUrl(), Types.VARCHAR)
                .param("sortOrder", sortOrder(command.sortOrder()))
                .param("id", id)
                .update();
    }

    public List<ExperienceAdminView> findExperiences(String status, int limit, int offset) {
        return jdbc.sql("""
                        SELECT id, name, category, season_text, duration_text, summary, price_text,
                               cover_url, video_url, booking_notes, sort_order, status, published_at, updated_at
                        FROM experience
                        WHERE (:status = 'ALL' OR status = :status)
                        ORDER BY sort_order, id
                        LIMIT :limit OFFSET :offset
                        """)
                .param("status", status)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNum) -> new ExperienceAdminView(
                        rs.getLong("id"), rs.getString("name"), rs.getString("category"),
                        rs.getString("season_text"), rs.getString("duration_text"), rs.getString("summary"),
                        rs.getString("price_text"), rs.getString("cover_url"), rs.getString("video_url"),
                        rs.getString("booking_notes"), rs.getInt("sort_order"), rs.getString("status"),
                        localDateTime(rs.getTimestamp("published_at")), localDateTime(rs.getTimestamp("updated_at"))))
                .list();
    }

    public long countExperiences(String status) {
        return count("experience", status);
    }

    public Optional<ExperienceAdminView> findExperience(long id) {
        return jdbc.sql("""
                        SELECT id, name, category, season_text, duration_text, summary, price_text,
                               cover_url, video_url, booking_notes, sort_order, status, published_at, updated_at
                        FROM experience WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new ExperienceAdminView(
                        rs.getLong("id"), rs.getString("name"), rs.getString("category"),
                        rs.getString("season_text"), rs.getString("duration_text"), rs.getString("summary"),
                        rs.getString("price_text"), rs.getString("cover_url"), rs.getString("video_url"),
                        rs.getString("booking_notes"), rs.getInt("sort_order"), rs.getString("status"),
                        localDateTime(rs.getTimestamp("published_at")), localDateTime(rs.getTimestamp("updated_at"))))
                .optional();
    }

    public long createExperience(ExperienceCommand command) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO experience (
                        name, category, season_text, duration_text, summary, price_text,
                        cover_url, video_url, booking_notes, sort_order, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                    """, new String[] { "id" });
            statement.setString(1, command.name());
            statement.setString(2, command.type());
            statement.setString(3, command.season());
            statement.setString(4, command.duration());
            statement.setString(5, command.summary());
            statement.setString(6, command.price());
            statement.setString(7, command.coverUrl());
            nullableString(statement, 8, command.videoUrl());
            nullableString(statement, 9, command.bookingNotes());
            statement.setInt(10, sortOrder(command.sortOrder()));
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public int updateExperience(long id, ExperienceCommand command) {
        return jdbc.sql("""
                        UPDATE experience
                        SET name = :name, category = :type, season_text = :season,
                            duration_text = :duration, summary = :summary, price_text = :price,
                            cover_url = :coverUrl, video_url = :videoUrl, booking_notes = :notes,
                            sort_order = :sortOrder, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id
                        """)
                .param("name", command.name())
                .param("type", command.type())
                .param("season", command.season())
                .param("duration", command.duration())
                .param("summary", command.summary())
                .param("price", command.price())
                .param("coverUrl", command.coverUrl())
                .param("videoUrl", command.videoUrl(), Types.VARCHAR)
                .param("notes", command.bookingNotes(), Types.VARCHAR)
                .param("sortOrder", sortOrder(command.sortOrder()))
                .param("id", id)
                .update();
    }

    public List<AttractionAdminView> findAttractions(String status, int limit, int offset) {
        return jdbc.sql("""
                        SELECT id, name, category, distance_km, drive_minutes, summary,
                               cover_url, map_url, highlights_json, sort_order, status, published_at, updated_at
                        FROM attraction
                        WHERE (:status = 'ALL' OR status = :status)
                        ORDER BY sort_order, id
                        LIMIT :limit OFFSET :offset
                        """)
                .param("status", status)
                .param("limit", limit)
                .param("offset", offset)
                .query((rs, rowNum) -> new AttractionAdminView(
                        rs.getLong("id"), rs.getString("name"), rs.getString("category"),
                        rs.getBigDecimal("distance_km"), rs.getInt("drive_minutes"), rs.getString("summary"),
                        rs.getString("cover_url"), rs.getString("map_url"),
                        readStringList(rs.getString("highlights_json")), rs.getInt("sort_order"),
                        rs.getString("status"), localDateTime(rs.getTimestamp("published_at")),
                        localDateTime(rs.getTimestamp("updated_at"))))
                .list();
    }

    public long countAttractions(String status) {
        return count("attraction", status);
    }

    public Optional<AttractionAdminView> findAttraction(long id) {
        return jdbc.sql("""
                        SELECT id, name, category, distance_km, drive_minutes, summary,
                               cover_url, map_url, highlights_json, sort_order, status, published_at, updated_at
                        FROM attraction WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new AttractionAdminView(
                        rs.getLong("id"), rs.getString("name"), rs.getString("category"),
                        rs.getBigDecimal("distance_km"), rs.getInt("drive_minutes"), rs.getString("summary"),
                        rs.getString("cover_url"), rs.getString("map_url"),
                        readStringList(rs.getString("highlights_json")), rs.getInt("sort_order"),
                        rs.getString("status"), localDateTime(rs.getTimestamp("published_at")),
                        localDateTime(rs.getTimestamp("updated_at"))))
                .optional();
    }

    public long createAttraction(AttractionCommand command) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO attraction (
                        name, category, distance_km, drive_minutes, summary,
                        cover_url, map_url, highlights_json, sort_order, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT')
                    """, new String[] { "id" });
            statement.setString(1, command.name());
            statement.setString(2, command.category());
            statement.setBigDecimal(3, command.distanceKm());
            statement.setInt(4, command.driveMinutes());
            statement.setString(5, command.summary());
            statement.setString(6, command.coverUrl());
            statement.setString(7, command.mapUrl());
            statement.setString(8, writeStringList(command.highlights()));
            statement.setInt(9, sortOrder(command.sortOrder()));
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public int updateAttraction(long id, AttractionCommand command) {
        return jdbc.sql("""
                        UPDATE attraction
                        SET name = :name, category = :category, distance_km = :distanceKm,
                            drive_minutes = :driveMinutes, summary = :summary, cover_url = :coverUrl,
                            map_url = :mapUrl, highlights_json = :highlights, sort_order = :sortOrder,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id
                        """)
                .param("name", command.name())
                .param("category", command.category())
                .param("distanceKm", command.distanceKm())
                .param("driveMinutes", command.driveMinutes())
                .param("summary", command.summary())
                .param("coverUrl", command.coverUrl())
                .param("mapUrl", command.mapUrl())
                .param("highlights", writeStringList(command.highlights()))
                .param("sortOrder", sortOrder(command.sortOrder()))
                .param("id", id)
                .update();
    }

    public Optional<ContentStatusView> findStatus(ContentKind kind, long id) {
        return jdbc.sql("SELECT id, status, published_at FROM " + kind.tableName() + " WHERE id = :id")
                .param("id", id)
                .query((rs, rowNum) -> new ContentStatusView(
                        rs.getLong("id"), kind.path(), rs.getString("status"),
                        localDateTime(rs.getTimestamp("published_at"))))
                .optional();
    }

    public void setPublished(ContentKind kind, long id, boolean published) {
        String sql = published
                ? "UPDATE " + kind.tableName() + " SET status = 'PUBLISHED', published_at = COALESCE(published_at, CURRENT_TIMESTAMP), updated_at = CURRENT_TIMESTAMP WHERE id = :id"
                : "UPDATE " + kind.tableName() + " SET status = 'DRAFT', published_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = :id";
        jdbc.sql(sql).param("id", id).update();
    }

    private long count(String table, String status) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE (:status = 'ALL' OR status = :status)")
                .param("status", status)
                .query(Long.class)
                .single();
    }

    private int sortOrder(Integer value) {
        return value == null ? 0 : value;
    }

    private void nullableString(PreparedStatement statement, int index, String value) throws java.sql.SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize highlights", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid highlights JSON in database", exception);
        }
    }

    private LocalDateTime localDateTime(java.sql.Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public enum ContentKind {
        HOMESTAYS("homestays", "homestay"),
        EXPERIENCES("experiences", "experience"),
        ATTRACTIONS("attractions", "attraction");

        private final String path;
        private final String tableName;

        ContentKind(String path, String tableName) {
            this.path = path;
            this.tableName = tableName;
        }

        public String path() {
            return path;
        }

        public String tableName() {
            return tableName;
        }

        public static Optional<ContentKind> fromPath(String path) {
            for (ContentKind kind : values()) {
                if (kind.path.equals(path)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }
    }
}
