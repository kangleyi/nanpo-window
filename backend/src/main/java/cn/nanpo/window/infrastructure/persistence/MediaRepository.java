package cn.nanpo.window.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import cn.nanpo.window.api.media.MediaViews.MediaView;
import cn.nanpo.window.api.media.MediaViews.UploadTicketCommand;

@Repository
public class MediaRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;

    public MediaRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
    }

    public long create(long ownerUserId, UploadTicketCommand command, String storageKey, Instant expiresAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO media_asset (
                        owner_user_id, media_type, storage_key, original_name, content_type,
                        size_bytes, checksum_sha256, status, expires_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 'CREATED', ?)
                    """, new String[] { "id" });
            statement.setLong(1, ownerUserId);
            statement.setString(2, command.mediaType());
            statement.setString(3, storageKey);
            statement.setString(4, command.originalName());
            statement.setString(5, command.contentType());
            statement.setLong(6, command.sizeBytes());
            nullableString(statement, 7, command.checksumSha256());
            statement.setTimestamp(8, Timestamp.from(expiresAt));
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    public void attachToRecord(long recordId, long mediaId) {
        jdbc.sql("INSERT INTO record_media (record_id, media_id, sort_order) VALUES (:recordId, :mediaId, 0)")
                .param("recordId", recordId).param("mediaId", mediaId).update();
    }

    public boolean ownsRecord(long ownerUserId, long recordId) {
        return jdbc.sql("""
                        SELECT COUNT(*) FROM farm_record r
                        JOIN farmer_profile f ON f.id = r.farmer_id
                        WHERE r.id = :recordId AND f.user_id = :ownerUserId
                        """)
                .param("recordId", recordId).param("ownerUserId", ownerUserId)
                .query(Long.class).single() == 1;
    }

    public boolean isRecordMedia(long mediaId) {
        return jdbc.sql("SELECT COUNT(*) FROM record_media WHERE media_id = :mediaId")
                .param("mediaId", mediaId).query(Long.class).single() > 0;
    }

    public Optional<MediaRow> find(long id) {
        return jdbc.sql("""
                        SELECT id, owner_user_id, media_type, storage_key, original_name, content_type,
                               size_bytes, checksum_sha256, status, failure_reason, expires_at,
                               uploaded_at, created_at, version
                        FROM media_asset WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, rowNum) -> new MediaRow(
                        rs.getLong("id"), rs.getLong("owner_user_id"), rs.getString("media_type"),
                        rs.getString("storage_key"), rs.getString("original_name"), rs.getString("content_type"),
                        rs.getLong("size_bytes"), rs.getString("checksum_sha256"), rs.getString("status"),
                        rs.getString("failure_reason"), instant(rs.getTimestamp("expires_at")),
                        localDateTime(rs.getTimestamp("uploaded_at")), localDateTime(rs.getTimestamp("created_at")),
                        rs.getLong("version")))
                .optional();
    }

    public boolean markUploaded(long id, long version) {
        return jdbc.sql("""
                        UPDATE media_asset SET status = 'UPLOADED', failure_reason = NULL,
                            uploaded_at = CURRENT_TIMESTAMP, version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version AND status IN ('CREATED', 'FAILED', 'UPLOADED')
                        """)
                .param("id", id).param("version", version).update() == 1;
    }

    public boolean markReady(long id, long version, String checksum) {
        return jdbc.sql("""
                        UPDATE media_asset SET status = 'READY', checksum_sha256 = :checksum,
                            failure_reason = NULL, version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version AND status = 'UPLOADED'
                        """)
                .param("checksum", checksum).param("id", id).param("version", version).update() == 1;
    }

    public boolean markFailed(long id, long version, String reason) {
        return jdbc.sql("""
                        UPDATE media_asset SET status = 'FAILED', failure_reason = :reason,
                            version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version AND status = 'UPLOADED'
                        """)
                .param("reason", reason).param("id", id).param("version", version).update() == 1;
    }

    public static MediaView view(MediaRow row) {
        return new MediaView(row.id(), row.mediaType(), row.originalName(), row.contentType(), row.sizeBytes(),
                row.checksumSha256(), row.status(), row.failureReason(), row.expiresAt(), row.uploadedAt(),
                row.createdAt(), row.version());
    }

    private void nullableString(PreparedStatement statement, int index, String value) throws java.sql.SQLException {
        if (value == null || value.isBlank()) statement.setNull(index, Types.VARCHAR);
        else statement.setString(index, value.toLowerCase());
    }

    private Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    public record MediaRow(
            long id, long ownerUserId, String mediaType, String storageKey, String originalName,
            String contentType, long sizeBytes, String checksumSha256, String status, String failureReason,
            Instant expiresAt, LocalDateTime uploadedAt, LocalDateTime createdAt, long version) {
    }
}
