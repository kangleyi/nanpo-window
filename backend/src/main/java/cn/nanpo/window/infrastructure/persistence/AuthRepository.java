package cn.nanpo.window.infrastructure.persistence;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import cn.nanpo.window.security.UserPrincipal;

@Repository
public class AuthRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;

    public AuthRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserPrincipal> findActiveUserByPhone(String phone) {
        return findAccount("phone = :credential", phone);
    }

    public Optional<UserPrincipal> findActiveUserByAccessHash(String accessHash) {
        Optional<Long> userId = jdbc.sql("""
                        SELECT s.user_id
                        FROM auth_session s
                        JOIN user_account u ON u.id = s.user_id
                        WHERE s.access_token_hash = :accessHash
                          AND s.revoked_at IS NULL
                          AND s.access_expires_at > CURRENT_TIMESTAMP
                          AND u.status = 'ACTIVE'
                        """)
                .param("accessHash", accessHash)
                .query(Long.class)
                .optional();
        return userId.flatMap(id -> findActiveUserById(id));
    }

    public Optional<UserPrincipal> findActiveUserById(long userId) {
        return findAccount("id = :credential", userId);
    }

    public long createCustomer(String phone) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO user_account (phone, display_name, status) VALUES (?, ?, 'ACTIVE')",
                    new String[] { "id" });
            statement.setString(1, phone);
            statement.setString(2, "客户 " + phone.substring(phone.length() - 4));
            return statement;
        }, keyHolder);
        long userId = keyHolder.getKey().longValue();
        jdbc.sql("INSERT INTO user_role (user_id, role_code) VALUES (:userId, 'CUSTOMER')")
                .param("userId", userId)
                .update();
        return userId;
    }

    public long createSession(
            long userId,
            String accessHash,
            String refreshHash,
            Instant accessExpiresAt,
            Instant refreshExpiresAt) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO auth_session (
                        user_id, access_token_hash, refresh_token_hash,
                        access_expires_at, refresh_expires_at
                    ) VALUES (?, ?, ?, ?, ?)
                    """, new String[] { "id" });
            statement.setLong(1, userId);
            statement.setString(2, accessHash);
            statement.setString(3, refreshHash);
            statement.setTimestamp(4, Timestamp.from(accessExpiresAt));
            statement.setTimestamp(5, Timestamp.from(refreshExpiresAt));
            return statement;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<RefreshSession> findRefreshSession(String refreshHash) {
        return jdbc.sql("""
                        SELECT id, user_id, refresh_expires_at
                        FROM auth_session
                        WHERE refresh_token_hash = :refreshHash
                          AND revoked_at IS NULL
                          AND refresh_expires_at > CURRENT_TIMESTAMP
                        """)
                .param("refreshHash", refreshHash)
                .query((rs, rowNum) -> new RefreshSession(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getTimestamp("refresh_expires_at").toInstant()))
                .optional();
    }

    public void revokeSession(long sessionId) {
        jdbc.sql("""
                        UPDATE auth_session
                        SET revoked_at = CURRENT_TIMESTAMP
                        WHERE id = :sessionId AND revoked_at IS NULL
                        """)
                .param("sessionId", sessionId)
                .update();
    }

    public void revokeByAccessHash(String accessHash) {
        jdbc.sql("""
                        UPDATE auth_session
                        SET revoked_at = CURRENT_TIMESTAMP
                        WHERE access_token_hash = :accessHash AND revoked_at IS NULL
                        """)
                .param("accessHash", accessHash)
                .update();
    }

    public void updateLastLogin(long userId) {
        jdbc.sql("UPDATE user_account SET last_login_at = CURRENT_TIMESTAMP WHERE id = :userId")
                .param("userId", userId)
                .update();
    }

    private Optional<UserPrincipal> findAccount(String predicate, Object credential) {
        Optional<AccountRow> account = jdbc.sql("""
                        SELECT id, phone, display_name
                        FROM user_account
                        WHERE %s AND status = 'ACTIVE'
                        """.formatted(predicate))
                .param("credential", credential)
                .query((rs, rowNum) -> new AccountRow(
                        rs.getLong("id"),
                        rs.getString("phone"),
                        rs.getString("display_name")))
                .optional();
        if (account.isEmpty()) {
            return Optional.empty();
        }
        AccountRow value = account.get();
        Set<String> roles = new LinkedHashSet<>(jdbc.sql("""
                        SELECT role_code
                        FROM user_role
                        WHERE user_id = :userId
                        ORDER BY role_code
                        """)
                .param("userId", value.id())
                .query(String.class)
                .list());
        return Optional.of(new UserPrincipal(value.id(), value.phone(), value.displayName(), roles));
    }

    public record RefreshSession(long id, long userId, Instant expiresAt) {
    }

    private record AccountRow(long id, String phone, String displayName) {
    }
}
