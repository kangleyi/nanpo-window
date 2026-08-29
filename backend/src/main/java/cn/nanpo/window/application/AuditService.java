package cn.nanpo.window.application;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import cn.nanpo.window.common.api.RequestContext;

@Service
public class AuditService {

    private final JdbcClient jdbc;

    public AuditService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void record(long userId, String action, String objectType, String objectId, String ipAddress) {
        jdbc.sql("""
                        INSERT INTO operation_audit_log (
                            user_id, action, object_type, object_id, ip_address, request_id
                        ) VALUES (:userId, :action, :objectType, :objectId, :ipAddress, :requestId)
                        """)
                .param("userId", userId)
                .param("action", action)
                .param("objectType", objectType)
                .param("objectId", objectId)
                .param("ipAddress", ipAddress)
                .param("requestId", RequestContext.requestId())
                .update();
    }
}
