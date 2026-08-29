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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.nanpo.window.api.farmer.AiCopyViews.AiCopyView;
import cn.nanpo.window.infrastructure.ai.AiCopyProvider.GeneratedCopy;
import cn.nanpo.window.infrastructure.ai.AiCopyProvider.SourceFact;

@Repository
public class AiCopyRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AiCopyRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<ProductSource> findOwnedProduct(long ownerUserId, long productId) {
        return jdbc.sql("""
                        SELECT p.id, p.name, p.season_text, f.name AS farmer_name
                        FROM product p JOIN farmer_profile f ON f.id = p.farmer_id
                        WHERE p.id = :productId AND f.user_id = :ownerUserId AND f.status = 'ACTIVE'
                        """)
                .param("productId", productId).param("ownerUserId", ownerUserId)
                .query((rs, rowNum) -> new ProductSource(
                        rs.getLong("id"), rs.getString("name"), rs.getString("season_text"),
                        rs.getString("farmer_name")))
                .optional();
    }

    public List<SourceFact> publishedFacts(long productId) {
        return jdbc.sql("""
                        SELECT id, stage, occurred_at, confirmed_text
                        FROM farm_record
                        WHERE product_id = :productId AND status = 'PUBLISHED'
                          AND truth_confirmed = TRUE AND confirmed_text IS NOT NULL
                        ORDER BY occurred_at DESC, id DESC
                        """)
                .param("productId", productId)
                .query((rs, rowNum) -> new SourceFact(
                        rs.getLong("id"), rs.getString("stage"),
                        rs.getTimestamp("occurred_at").toLocalDateTime(), rs.getString("confirmed_text")))
                .list();
    }

    public long create(long ownerUserId, String scene, List<Long> sourceIds, GeneratedCopy generated) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ai_generation (
                        owner_user_id, scene, source_refs_json, model_name, model_version,
                        output_text, status
                    ) VALUES (?, ?, ?, ?, ?, ?, 'DRAFT')
                    """, new String[] { "id" });
            statement.setLong(1, ownerUserId);
            statement.setString(2, scene);
            statement.setString(3, writeIds(sourceIds));
            statement.setString(4, generated.modelName());
            nullableString(statement, 5, generated.modelVersion());
            statement.setString(6, generated.text());
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public Optional<AiCopyView> find(long id) {
        return jdbc.sql(SELECT + " WHERE id = :id")
                .param("id", id).query(this::map).optional();
    }

    public List<AiCopyView> findByOwner(long ownerUserId) {
        return jdbc.sql(SELECT + " WHERE owner_user_id = :ownerUserId ORDER BY created_at DESC, id DESC")
                .param("ownerUserId", ownerUserId).query(this::map).list();
    }

    public boolean confirm(long id, long ownerUserId, long version, String confirmedText) {
        return jdbc.sql("""
                        UPDATE ai_generation SET confirmed_text = :confirmedText, status = 'CONFIRMED',
                            confirmed_at = CURRENT_TIMESTAMP, version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND owner_user_id = :ownerUserId AND version = :version AND status = 'DRAFT'
                        """)
                .param("confirmedText", confirmedText).param("id", id)
                .param("ownerUserId", ownerUserId).param("version", version).update() == 1;
    }

    private AiCopyView map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AiCopyView(rs.getLong("id"), rs.getString("scene"), readIds(rs.getString("source_refs_json")),
                rs.getString("model_name"), rs.getString("model_version"), rs.getString("output_text"),
                rs.getString("confirmed_text"), rs.getString("status"),
                localDateTime(rs.getTimestamp("confirmed_at")), localDateTime(rs.getTimestamp("created_at")),
                rs.getLong("version"));
    }

    private String writeIds(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法保存 AI 来源", exception);
        }
    }

    private List<Long> readIds(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AI 来源数据损坏", exception);
        }
    }

    private void nullableString(PreparedStatement statement, int index, String value) throws java.sql.SQLException {
        if (value == null) statement.setNull(index, Types.VARCHAR);
        else statement.setString(index, value);
    }

    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static final String SELECT = """
            SELECT id, scene, source_refs_json, model_name, model_version, output_text,
                   confirmed_text, status, confirmed_at, created_at, version
            FROM ai_generation
            """;

    public record ProductSource(long id, String name, String season, String farmerName) {
    }
}
