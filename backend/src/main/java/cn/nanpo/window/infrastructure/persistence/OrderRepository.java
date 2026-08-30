package cn.nanpo.window.infrastructure.persistence;

import java.math.BigDecimal;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.nanpo.window.api.order.OrderViews.OrderItemView;
import cn.nanpo.window.api.order.OrderViews.OrderView;
import cn.nanpo.window.api.order.OrderViews.PaymentSnapshot;
import cn.nanpo.window.common.api.RequestContext;

@Repository
public class OrderRepository {

    private final JdbcClient jdbc;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OrderRepository(JdbcClient jdbc, JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<SaleSku> findSaleSku(long skuId) {
        return jdbc.sql("""
                        SELECT s.id, s.product_id, p.farmer_id, p.name AS product_name,
                               s.specification, s.unit_price, s.stock_note
                        FROM product_sku s
                        JOIN product p ON p.id = s.product_id
                        JOIN farmer_profile f ON f.id = p.farmer_id
                        WHERE s.id = :skuId AND s.enabled = TRUE
                          AND p.status = 'PUBLISHED'
                          AND f.status = 'ACTIVE' AND f.certification_status = 'APPROVED'
                        """)
                .param("skuId", skuId)
                .query((rs, rowNum) -> new SaleSku(
                        rs.getLong("id"), rs.getLong("product_id"), rs.getLong("farmer_id"),
                        rs.getString("product_name"), rs.getString("specification"),
                        rs.getBigDecimal("unit_price"), rs.getString("stock_note")))
                .optional();
    }

    public Optional<PaymentConfig> findActivePaymentConfig() {
        return jdbc.sql("""
                        SELECT q.version_no, q.payee_name, m.storage_key
                        FROM payment_qr_config q
                        JOIN media_asset m ON m.id = q.media_id
                        WHERE q.status = 'PUBLISHED'
                          AND q.enabled_from <= CURRENT_TIMESTAMP
                          AND (q.disabled_at IS NULL OR q.disabled_at > CURRENT_TIMESTAMP)
                          AND m.status = 'READY'
                        ORDER BY q.version_no DESC
                        LIMIT 1
                        """)
                .query((rs, rowNum) -> new PaymentConfig(
                        rs.getInt("version_no"), rs.getString("payee_name"), rs.getString("storage_key")))
                .optional();
    }

    public Optional<OrderView> findByIdempotencyKey(String idempotencyKey) {
        return findOrderId("o.idempotency_key = :value", idempotencyKey).flatMap(this::findById);
    }

    public Optional<OrderView> findForCustomer(String orderNo, long customerUserId) {
        Optional<Long> id = jdbc.sql("""
                        SELECT id FROM customer_order
                        WHERE order_no = :orderNo AND customer_user_id = :customerUserId
                        """)
                .param("orderNo", orderNo).param("customerUserId", customerUserId)
                .query(Long.class).optional();
        return id.flatMap(this::findById);
    }

    public List<OrderView> findCustomerOrders(long customerUserId) {
        List<OrderRow> rows = jdbc.sql(ORDER_SELECT + """
                        WHERE o.customer_user_id = :customerUserId
                        ORDER BY o.created_at DESC, o.id DESC
                        LIMIT 100
                        """)
                .param("customerUserId", customerUserId)
                .query(this::mapOrderRow)
                .list();
        return rows.stream().map(row -> toView(row, findItems(row.id()))).toList();
    }

    public Optional<OrderView> findById(long id) {
        Optional<OrderRow> row = jdbc.sql(ORDER_SELECT + " WHERE o.id = :id")
                .param("id", id)
                .query(this::mapOrderRow)
                .optional();
        return row.map(value -> toView(value, findItems(value.id())));
    }

    public List<OrderView> findAdminOrders(String status, Long farmerId) {
        List<OrderRow> rows = jdbc.sql(ORDER_SELECT + """
                        WHERE (:status = 'ALL' OR o.status = :status)
                          AND (:farmerId IS NULL OR EXISTS (
                              SELECT 1 FROM order_item farmer_item
                              WHERE farmer_item.order_id = o.id AND farmer_item.farmer_id = :farmerId
                          ))
                        ORDER BY o.created_at DESC, o.id DESC LIMIT 100
                        """)
                .param("status", status)
                .param("farmerId", farmerId, Types.BIGINT)
                .query(this::mapOrderRow)
                .list();
        return rows.stream().map(row -> toView(row, findItems(row.id()))).toList();
    }

    public List<OrderView> findFarmerOrders(long farmerId) {
        List<Long> ids = jdbc.sql("""
                        SELECT DISTINCT o.id
                        FROM customer_order o
                        JOIN order_item i ON i.order_id = o.id
                        WHERE i.farmer_id = :farmerId
                          AND o.status IN ('PAID', 'READY_TO_SHIP', 'SHIPPED', 'COMPLETED')
                        ORDER BY o.id DESC
                        """)
                .param("farmerId", farmerId)
                .query(Long.class)
                .list();
        return ids.stream().map(this::findById).flatMap(Optional::stream).toList();
    }

    public long createOrder(NewOrder order) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO customer_order (
                        order_no, customer_user_id, recipient_name, recipient_phone,
                        recipient_address, customer_note, total_amount, payment_qr_snapshot,
                        status, idempotency_key
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'CREATED', ?)
                    """, new String[] { "id" });
            statement.setString(1, order.orderNo());
            statement.setLong(2, order.customerUserId());
            statement.setString(3, order.recipientName());
            statement.setString(4, order.recipientPhone());
            statement.setString(5, order.recipientAddress());
            nullableString(statement, 6, order.customerNote());
            statement.setBigDecimal(7, order.totalAmount());
            statement.setString(8, order.paymentSnapshotJson());
            statement.setString(9, order.idempotencyKey());
            return statement;
        }, keys);
        long orderId = keys.getKey().longValue();
        for (NewOrderItem item : order.items()) {
            jdbc.sql("""
                            INSERT INTO order_item (
                                order_id, product_id, sku_id, farmer_id, product_snapshot,
                                sku_snapshot, quantity, unit_price, line_amount
                            ) VALUES (
                                :orderId, :productId, :skuId, :farmerId, :productSnapshot,
                                :skuSnapshot, :quantity, :unitPrice, :lineAmount
                            )
                            """)
                    .param("orderId", orderId)
                    .param("productId", item.productId())
                    .param("skuId", item.skuId())
                    .param("farmerId", item.farmerId())
                    .param("productSnapshot", item.productSnapshotJson())
                    .param("skuSnapshot", item.skuSnapshotJson())
                    .param("quantity", item.quantity())
                    .param("unitPrice", item.unitPrice())
                    .param("lineAmount", item.lineAmount())
                    .update();
        }
        appendStatusLog(orderId, null, "CREATED", order.customerUserId(), "客户创建订单");
        return orderId;
    }

    public boolean reportPayment(long id, long customerUserId, long version, String note) {
        return jdbc.sql("""
                        UPDATE customer_order
                        SET status = 'PAYMENT_REPORTED', payment_report_note = :note,
                            payment_reported_at = CURRENT_TIMESTAMP,
                            version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND customer_user_id = :customerUserId
                          AND version = :version AND status = 'CREATED'
                        """)
                .param("note", note, Types.VARCHAR)
                .param("id", id).param("customerUserId", customerUserId).param("version", version)
                .update() == 1;
    }

    public boolean cancel(long id, long customerUserId, long version) {
        return jdbc.sql("""
                        UPDATE customer_order
                        SET status = 'CANCELLED', cancelled_at = CURRENT_TIMESTAMP,
                            version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND customer_user_id = :customerUserId
                          AND version = :version AND status IN ('CREATED', 'PAYMENT_REPORTED')
                        """)
                .param("id", id).param("customerUserId", customerUserId).param("version", version)
                .update() == 1;
    }

    public boolean confirmPayment(long id, long version) {
        return jdbc.sql("""
                        UPDATE customer_order
                        SET status = 'PAID', payment_confirmed_at = CURRENT_TIMESTAMP,
                            version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version
                          AND status IN ('CREATED', 'PAYMENT_REPORTED')
                        """)
                .param("id", id).param("version", version).update() == 1;
    }

    public boolean rejectPayment(long id, long version, String reason) {
        return jdbc.sql("""
                        UPDATE customer_order
                        SET status = 'CREATED', payment_report_note = :reason,
                            payment_reported_at = NULL, version = version + 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version AND status = 'PAYMENT_REPORTED'
                        """)
                .param("reason", reason).param("id", id).param("version", version).update() == 1;
    }

    public boolean markReadyToShip(long id, long version) {
        return jdbc.sql("""
                        UPDATE customer_order
                        SET status = 'READY_TO_SHIP', version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version AND status = 'PAID'
                        """)
                .param("id", id).param("version", version).update() == 1;
    }

    public boolean ship(long id, long version, String company, String trackingNo) {
        return jdbc.sql("""
                        UPDATE customer_order
                        SET status = 'SHIPPED', shipping_company = :company, tracking_no = :trackingNo,
                            shipped_at = CURRENT_TIMESTAMP, version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version AND status = 'READY_TO_SHIP'
                        """)
                .param("company", company).param("trackingNo", trackingNo)
                .param("id", id).param("version", version).update() == 1;
    }

    public boolean complete(long id, long customerUserId, long version) {
        return jdbc.sql("""
                        UPDATE customer_order
                        SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP,
                            version = version + 1, updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND customer_user_id = :customerUserId
                          AND version = :version AND status = 'SHIPPED'
                        """)
                .param("id", id).param("customerUserId", customerUserId).param("version", version)
                .update() == 1;
    }

    public boolean refund(long id, long version, BigDecimal amount, String reason) {
        return jdbc.sql("""
                        UPDATE customer_order
                        SET status = 'REFUNDED', refund_amount = :amount, refund_note = :reason,
                            refunded_at = CURRENT_TIMESTAMP, version = version + 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :id AND version = :version
                          AND status IN ('PAID', 'READY_TO_SHIP', 'SHIPPED', 'COMPLETED')
                          AND :amount <= total_amount
                        """)
                .param("amount", amount).param("reason", reason)
                .param("id", id).param("version", version).update() == 1;
    }

    public void appendStatusLog(
            long orderId, String fromStatus, String toStatus, Long operatorUserId, String reason) {
        jdbc.sql("""
                        INSERT INTO order_status_log (
                            order_id, from_status, to_status, operator_user_id, reason, request_id
                        ) VALUES (:orderId, :fromStatus, :toStatus, :operatorUserId, :reason, :requestId)
                        """)
                .param("orderId", orderId)
                .param("fromStatus", fromStatus, Types.VARCHAR)
                .param("toStatus", toStatus)
                .param("operatorUserId", operatorUserId, Types.BIGINT)
                .param("reason", reason, Types.VARCHAR)
                .param("requestId", RequestContext.requestId(), Types.VARCHAR)
                .update();
    }

    private Optional<Long> findOrderId(String predicate, Object value) {
        return jdbc.sql("SELECT o.id FROM customer_order o WHERE " + predicate)
                .param("value", value).query(Long.class).optional();
    }

    private List<OrderItemView> findItems(long orderId) {
        return jdbc.sql("""
                        SELECT id, product_id, sku_id, farmer_id, product_snapshot, sku_snapshot,
                               quantity, unit_price, line_amount
                        FROM order_item WHERE order_id = :orderId ORDER BY id
                        """)
                .param("orderId", orderId)
                .query((rs, rowNum) -> {
                    ProductSnapshot product = readJson(rs.getString("product_snapshot"), ProductSnapshot.class);
                    SkuSnapshot sku = readJson(rs.getString("sku_snapshot"), SkuSnapshot.class);
                    return new OrderItemView(
                            rs.getLong("id"), rs.getLong("product_id"), rs.getLong("sku_id"),
                            rs.getLong("farmer_id"), product.name(), sku.specification(),
                            rs.getInt("quantity"), rs.getBigDecimal("unit_price"), rs.getBigDecimal("line_amount"));
                })
                .list();
    }

    private OrderRow mapOrderRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new OrderRow(
                rs.getLong("id"), rs.getString("order_no"), rs.getLong("customer_user_id"),
                rs.getString("recipient_name"), rs.getString("recipient_phone"),
                rs.getString("recipient_address"), rs.getString("customer_note"),
                rs.getBigDecimal("total_amount"), rs.getString("payment_qr_snapshot"), rs.getString("status"),
                rs.getString("payment_report_note"), localDateTime(rs.getTimestamp("payment_reported_at")),
                localDateTime(rs.getTimestamp("payment_confirmed_at")), rs.getString("shipping_company"),
                rs.getString("tracking_no"), localDateTime(rs.getTimestamp("shipped_at")),
                localDateTime(rs.getTimestamp("completed_at")), localDateTime(rs.getTimestamp("cancelled_at")),
                rs.getBigDecimal("refund_amount"), rs.getString("refund_note"),
                localDateTime(rs.getTimestamp("refunded_at")), rs.getLong("version"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private OrderView toView(OrderRow row, List<OrderItemView> items) {
        return new OrderView(
                row.id(), row.orderNo(), row.customerUserId(), row.recipientName(), row.recipientPhone(),
                row.recipientAddress(), row.customerNote(), row.totalAmount(),
                readJson(row.paymentSnapshotJson(), PaymentSnapshot.class), row.status(),
                row.paymentReportNote(), row.paymentReportedAt(), row.paymentConfirmedAt(),
                row.shippingCompany(), row.trackingNo(), row.shippedAt(), row.completedAt(), row.cancelledAt(),
                row.refundAmount(), row.refundNote(), row.refundedAt(), row.version(), row.createdAt(), row.updatedAt(), items);
    }

    private <T> T readJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Invalid order snapshot in database", exception);
        }
    }

    private void nullableString(PreparedStatement statement, int index, String value) throws java.sql.SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private static final String ORDER_SELECT = """
            SELECT o.id, o.order_no, o.customer_user_id, o.recipient_name, o.recipient_phone,
                   o.recipient_address, o.customer_note, o.total_amount, o.payment_qr_snapshot,
                   o.status, o.payment_report_note, o.payment_reported_at, o.payment_confirmed_at,
                   o.shipping_company, o.tracking_no, o.shipped_at, o.completed_at, o.cancelled_at,
                   o.refund_amount, o.refund_note, o.refunded_at, o.version, o.created_at, o.updated_at
            FROM customer_order o
            """;

    public record SaleSku(
            long id, long productId, long farmerId, String productName,
            String specification, BigDecimal unitPrice, String stockNote) {
    }

    public record PaymentConfig(int version, String payeeName, String storageKey) {
    }

    public record ProductSnapshot(String name) {
    }

    public record SkuSnapshot(String specification) {
    }

    public record NewOrder(
            String orderNo,
            long customerUserId,
            String recipientName,
            String recipientPhone,
            String recipientAddress,
            String customerNote,
            BigDecimal totalAmount,
            String paymentSnapshotJson,
            String idempotencyKey,
            List<NewOrderItem> items) {
    }

    public record NewOrderItem(
            long productId,
            long skuId,
            long farmerId,
            String productSnapshotJson,
            String skuSnapshotJson,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount) {
    }

    private record OrderRow(
            long id, String orderNo, long customerUserId, String recipientName, String recipientPhone,
            String recipientAddress, String customerNote, BigDecimal totalAmount, String paymentSnapshotJson,
            String status, String paymentReportNote, LocalDateTime paymentReportedAt,
            LocalDateTime paymentConfirmedAt, String shippingCompany, String trackingNo,
            LocalDateTime shippedAt, LocalDateTime completedAt, LocalDateTime cancelledAt,
            BigDecimal refundAmount, String refundNote, LocalDateTime refundedAt,
            long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
