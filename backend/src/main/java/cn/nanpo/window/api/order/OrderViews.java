package cn.nanpo.window.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OrderViews {

    private OrderViews() {
    }

    public record OrderLineCommand(@NotNull Long skuId, @Min(1) int quantity) {
    }

    public record CreateOrderCommand(
            @NotBlank @Size(max = 100) String recipientName,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请填写 11 位中国大陆手机号") String recipientPhone,
            @NotBlank @Size(max = 500) String recipientAddress,
            @Size(max = 500) String customerNote,
            @NotEmpty @Size(max = 50) List<@Valid OrderLineCommand> items) {
    }

    public record PaymentSnapshot(
            int version,
            String payeeName,
            String storageKey,
            boolean demo) {
    }

    public record OrderItemView(
            long id,
            long productId,
            long skuId,
            long farmerId,
            String productName,
            String specification,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount) {
    }

    public record OrderView(
            long id,
            String orderNo,
            long customerUserId,
            String recipientName,
            String recipientPhone,
            String recipientAddress,
            String customerNote,
            BigDecimal totalAmount,
            PaymentSnapshot payment,
            String status,
            String paymentReportNote,
            LocalDateTime paymentReportedAt,
            LocalDateTime paymentConfirmedAt,
            String shippingCompany,
            String trackingNo,
            LocalDateTime shippedAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt,
            BigDecimal refundAmount,
            String refundNote,
            LocalDateTime refundedAt,
            long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<OrderItemView> items) {
    }

    public record PaymentReportCommand(@Size(max = 500) String note) {
    }

    public record ReasonCommand(@NotBlank @Size(max = 500) String reason) {
    }

    public record ShipOrderCommand(
            @NotBlank @Size(max = 100) String shippingCompany,
            @NotBlank @Size(max = 160) String trackingNo) {
    }

    public record RefundOrderCommand(
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank @Size(max = 500) String reason) {
    }

    public record FarmerOrderView(
            long id,
            String orderNo,
            String status,
            LocalDateTime createdAt,
            List<OrderItemView> items) {
    }
}
