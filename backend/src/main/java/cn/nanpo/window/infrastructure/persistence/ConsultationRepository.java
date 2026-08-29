package cn.nanpo.window.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import cn.nanpo.window.api.inquiry.InquiryViews.InquiryCommand;
import cn.nanpo.window.api.inquiry.InquiryViews.InquiryView;

@Repository
public class ConsultationRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;

    public ConsultationRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> findPublishedTarget(String sourceType, long sourceId) {
        String table = "HOMESTAY".equals(sourceType) ? "homestay" : "experience";
        return jdbc.sql("SELECT name FROM " + table + " WHERE id = :id AND status = 'PUBLISHED'")
                .param("id", sourceId).query(String.class).optional();
    }

    public long create(InquiryCommand command, String targetName) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO consultation_inquiry (
                        source_type, source_id, target_name, visit_at, party_size,
                        callback_phone, note, status
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'NEW')
                    """, new String[] { "id" });
            statement.setString(1, command.sourceType());
            statement.setLong(2, command.sourceId());
            statement.setString(3, targetName);
            statement.setTimestamp(4, Timestamp.valueOf(command.visitAt()));
            statement.setInt(5, command.partySize());
            statement.setString(6, command.callbackPhone());
            if (command.note() == null || command.note().isBlank()) statement.setNull(7, Types.VARCHAR);
            else statement.setString(7, command.note().trim());
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public Optional<InquiryView> find(long id) {
        return jdbc.sql(SELECT + " WHERE id = :id")
                .param("id", id).query(this::map).optional();
    }

    public List<InquiryView> findAdmin(String status, String sourceType) {
        return jdbc.sql(SELECT + """
                        WHERE (:status = 'ALL' OR status = :status)
                          AND (:sourceType = 'ALL' OR source_type = :sourceType)
                        ORDER BY created_at DESC, id DESC
                        LIMIT 200
                        """)
                .param("status", status).param("sourceType", sourceType)
                .query(this::map).list();
    }

    public boolean updateStatus(long id, String status) {
        return jdbc.sql("""
                        UPDATE consultation_inquiry
                        SET status = :status, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id
                        """)
                .param("status", status).param("id", id).update() == 1;
    }

    private InquiryView map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new InquiryView(
                rs.getLong("id"), rs.getString("source_type"), rs.getLong("source_id"),
                rs.getString("target_name"), rs.getTimestamp("visit_at").toLocalDateTime(),
                rs.getInt("party_size"), rs.getString("callback_phone"), rs.getString("note"),
                rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private static final String SELECT = """
            SELECT id, source_type, source_id, target_name, visit_at, party_size,
                   callback_phone, note, status, created_at, updated_at
            FROM consultation_inquiry
            """;
}
