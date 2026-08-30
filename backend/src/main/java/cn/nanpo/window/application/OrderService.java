package cn.nanpo.window.application;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.nanpo.window.api.order.OrderViews.CreateOrderCommand;
import cn.nanpo.window.api.order.OrderViews.FarmerOrderView;
import cn.nanpo.window.api.order.OrderViews.OrderItemView;
import cn.nanpo.window.api.order.OrderViews.OrderView;
import cn.nanpo.window.api.order.OrderViews.PaymentSnapshot;
import cn.nanpo.window.api.order.OrderViews.RefundOrderCommand;
import cn.nanpo.window.api.order.OrderViews.ShipOrderCommand;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.persistence.FarmerWorkspaceRepository;
import cn.nanpo.window.infrastructure.persistence.OrderRepository;
import cn.nanpo.window.infrastructure.persistence.OrderRepository.NewOrder;
import cn.nanpo.window.infrastructure.persistence.OrderRepository.NewOrderItem;
import cn.nanpo.window.infrastructure.persistence.OrderRepository.PaymentConfig;
import cn.nanpo.window.infrastructure.persistence.OrderRepository.ProductSnapshot;
import cn.nanpo.window.infrastructure.persistence.OrderRepository.SaleSku;
import cn.nanpo.window.infrastructure.persistence.OrderRepository.SkuSnapshot;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class OrderService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ORDER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository repository;
    private final FarmerWorkspaceRepository farmerRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom;
    private final Clock clock;

    @Autowired
    public OrderService(
            OrderRepository repository,
            FarmerWorkspaceRepository farmerRepository,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this(repository, farmerRepository, auditService, objectMapper, new SecureRandom(), Clock.systemUTC());
    }

    OrderService(
            OrderRepository repository,
            FarmerWorkspaceRepository farmerRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            SecureRandom secureRandom,
            Clock clock) {
        this.repository = repository;
        this.farmerRepository = farmerRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    @Transactional
    public OrderView create(
            String idempotencyKey, CreateOrderCommand command, UserPrincipal customer, String ipAddress) {
        validateIdempotencyKey(idempotencyKey);
        OrderView existing = repository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (existing.customerUserId() != customer.id()) {
                throw new ApiException(ErrorCode.CONFLICT, "幂等键已被其他账号使用");
            }
            return existing;
        }

        Set<Long> skuIds = new HashSet<>();
        Set<Long> farmerIds = new HashSet<>();
        List<NewOrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var requested : command.items()) {
            if (!skuIds.add(requested.skuId())) {
                throw new ApiException(ErrorCode.INVALID_ARGUMENT, "同一 SKU 请合并数量后再提交");
            }
            SaleSku sku = repository.findSaleSku(requested.skuId())
                    .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "商品规格不存在或已下架"));
            farmerIds.add(sku.farmerId());
            BigDecimal lineAmount = sku.unitPrice().multiply(BigDecimal.valueOf(requested.quantity()));
            total = total.add(lineAmount);
            items.add(new NewOrderItem(
                    sku.productId(), sku.id(), sku.farmerId(),
                    json(new ProductSnapshot(sku.productName())),
                    json(new SkuSnapshot(sku.specification())),
                    requested.quantity(), sku.unitPrice(), lineAmount));
        }
        if (farmerIds.size() > 1) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "一张订单暂只支持同一农户的农品");
        }
        PaymentConfig payment = repository.findActivePaymentConfig()
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "当前没有可用的收款配置"));
        PaymentSnapshot paymentSnapshot = new PaymentSnapshot(
                payment.version(), payment.payeeName(), payment.storageKey(), payment.storageKey().startsWith("local/"));
        NewOrder order = new NewOrder(
                newOrderNo(), customer.id(), command.recipientName().trim(), command.recipientPhone(),
                command.recipientAddress().trim(), blankToNull(command.customerNote()), total,
                json(paymentSnapshot), idempotencyKey, items);
        try {
            long orderId = repository.createOrder(order);
            auditService.record(customer.id(), "ORDER_CREATE", "CUSTOMER_ORDER", String.valueOf(orderId), ipAddress);
            return repository.findById(orderId).orElseThrow();
        } catch (DataIntegrityViolationException exception) {
            return repository.findByIdempotencyKey(idempotencyKey)
                    .filter(value -> value.customerUserId() == customer.id())
                    .orElseThrow(() -> new ApiException(ErrorCode.CONFLICT, "订单创建冲突，请重试"));
        }
    }

    @Transactional(readOnly = true)
    public OrderView customerOrder(String orderNo, UserPrincipal customer) {
        return repository.findForCustomer(orderNo, customer.id())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    }

    @Transactional
    public OrderView reportPayment(
            String orderNo, String note, UserPrincipal customer, String ipAddress) {
        OrderView order = customerOrder(orderNo, customer);
        requireStatus(order, OrderStatus.CREATED);
        if (!repository.reportPayment(order.id(), customer.id(), order.version(), blankToNull(note))) {
            throw concurrentConflict();
        }
        logTransition(order, OrderStatus.PAYMENT_REPORTED, customer.id(), "客户报告已转账");
        auditService.record(customer.id(), "ORDER_PAYMENT_REPORT", "CUSTOMER_ORDER", String.valueOf(order.id()), ipAddress);
        return repository.findById(order.id()).orElseThrow();
    }

    @Transactional
    public OrderView cancel(String orderNo, UserPrincipal customer, String ipAddress) {
        OrderView order = customerOrder(orderNo, customer);
        if (!Set.of(OrderStatus.CREATED.name(), OrderStatus.PAYMENT_REPORTED.name()).contains(order.status())) {
            throw new ApiException(ErrorCode.CONFLICT, "当前订单状态不允许取消");
        }
        if (!repository.cancel(order.id(), customer.id(), order.version())) {
            throw concurrentConflict();
        }
        logTransition(order, OrderStatus.CANCELLED, customer.id(), "客户取消未付款订单");
        auditService.record(customer.id(), "ORDER_CANCEL", "CUSTOMER_ORDER", String.valueOf(order.id()), ipAddress);
        return repository.findById(order.id()).orElseThrow();
    }

    @Transactional
    public OrderView complete(String orderNo, UserPrincipal customer, String ipAddress) {
        OrderView order = customerOrder(orderNo, customer);
        requireStatus(order, OrderStatus.SHIPPED);
        if (!repository.complete(order.id(), customer.id(), order.version())) {
            throw concurrentConflict();
        }
        logTransition(order, OrderStatus.COMPLETED, customer.id(), "客户确认完成");
        auditService.record(customer.id(), "ORDER_COMPLETE", "CUSTOMER_ORDER", String.valueOf(order.id()), ipAddress);
        return repository.findById(order.id()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<OrderView> adminOrders(String status, Long farmerId) {
        String normalized = status == null || status.isBlank() ? "ALL" : status.toUpperCase();
        if (!"ALL".equals(normalized)) {
            try {
                OrderStatus.valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                throw new ApiException(ErrorCode.INVALID_ARGUMENT, "不支持的订单状态");
            }
        }
        if (farmerId != null && farmerId < 1) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "村民编号必须大于 0");
        }
        return repository.findAdminOrders(normalized, farmerId);
    }

    @Transactional
    public OrderView markReadyByAdmin(long id, UserPrincipal operator, String ipAddress) {
        OrderView order = order(id);
        requireStatus(order, OrderStatus.PAID);
        if (!repository.markReadyToShip(id, order.version())) {
            throw concurrentConflict();
        }
        logTransition(order, OrderStatus.READY_TO_SHIP, operator.id(), "运营确认备货完成");
        auditService.record(operator.id(), "ORDER_READY_TO_SHIP", "CUSTOMER_ORDER", String.valueOf(id), ipAddress);
        return order(id);
    }

    @Transactional
    public OrderView confirmPayment(long id, UserPrincipal operator, String ipAddress) {
        OrderView order = order(id);
        if (!Set.of(OrderStatus.CREATED.name(), OrderStatus.PAYMENT_REPORTED.name()).contains(order.status())) {
            throw new ApiException(ErrorCode.CONFLICT, "仅待付款或待核款订单可以确认收款");
        }
        if (!repository.confirmPayment(id, order.version())) {
            throw concurrentConflict();
        }
        String reason = OrderStatus.CREATED.name().equals(order.status())
                ? "运营核对到账后人工确认（客户未主动申报）"
                : "运营人工确认到账";
        logTransition(order, OrderStatus.PAID, operator.id(), reason);
        auditService.record(operator.id(), "ORDER_CONFIRM_PAYMENT", "CUSTOMER_ORDER", String.valueOf(id), ipAddress);
        return order(id);
    }

    @Transactional
    public OrderView rejectPayment(long id, String reason, UserPrincipal operator, String ipAddress) {
        OrderView order = order(id);
        requireStatus(order, OrderStatus.PAYMENT_REPORTED);
        if (!repository.rejectPayment(id, order.version(), reason)) {
            throw concurrentConflict();
        }
        logTransition(order, OrderStatus.CREATED, operator.id(), reason);
        auditService.record(operator.id(), "ORDER_REJECT_PAYMENT", "CUSTOMER_ORDER", String.valueOf(id), ipAddress);
        return order(id);
    }

    @Transactional(readOnly = true)
    public List<FarmerOrderView> farmerOrders(UserPrincipal actor) {
        long farmerId = farmerRepository.findFarmerByUserId(actor.id())
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "当前账号未绑定农户档案"))
                .id();
        return repository.findFarmerOrders(farmerId).stream()
                .map(order -> new FarmerOrderView(
                        order.id(), order.orderNo(), order.status(), order.createdAt(),
                        order.items().stream().filter(item -> item.farmerId() == farmerId).toList()))
                .toList();
    }

    @Transactional
    public OrderView markReady(long id, UserPrincipal farmerUser, String ipAddress) {
        long farmerId = farmerRepository.findFarmerByUserId(farmerUser.id())
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "当前账号未绑定农户档案"))
                .id();
        OrderView order = order(id);
        if (order.items().stream().noneMatch(item -> item.farmerId() == farmerId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "不能处理其他农户的订单");
        }
        requireStatus(order, OrderStatus.PAID);
        if (!repository.markReadyToShip(id, order.version())) {
            throw concurrentConflict();
        }
        logTransition(order, OrderStatus.READY_TO_SHIP, farmerUser.id(), "农户确认备货完成");
        auditService.record(farmerUser.id(), "ORDER_READY_TO_SHIP", "CUSTOMER_ORDER", String.valueOf(id), ipAddress);
        return order(id);
    }

    @Transactional
    public OrderView ship(long id, ShipOrderCommand command, UserPrincipal operator, String ipAddress) {
        OrderView order = order(id);
        requireStatus(order, OrderStatus.READY_TO_SHIP);
        if (!repository.ship(id, order.version(), command.shippingCompany().trim(), command.trackingNo().trim())) {
            throw concurrentConflict();
        }
        logTransition(order, OrderStatus.SHIPPED, operator.id(), "运营录入物流并发货");
        auditService.record(operator.id(), "ORDER_SHIP", "CUSTOMER_ORDER", String.valueOf(id), ipAddress);
        return order(id);
    }

    @Transactional
    public OrderView refund(long id, RefundOrderCommand command, UserPrincipal operator, String ipAddress) {
        OrderView order = order(id);
        if (command.amount().compareTo(order.totalAmount()) > 0) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "退款金额不能超过订单金额");
        }
        if (!repository.refund(id, order.version(), command.amount(), command.reason().trim())) {
            throw new ApiException(ErrorCode.CONFLICT, "当前订单状态不允许登记退款，或订单已更新");
        }
        logTransition(order, OrderStatus.REFUNDED, operator.id(), command.reason().trim());
        auditService.record(operator.id(), "ORDER_REFUND", "CUSTOMER_ORDER", String.valueOf(id), ipAddress);
        return order(id);
    }

    private OrderView order(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在"));
    }

    private void requireStatus(OrderView order, OrderStatus expected) {
        if (!expected.name().equals(order.status())) {
            throw new ApiException(ErrorCode.CONFLICT,
                    "订单状态已变更，期望 " + expected.name() + "，当前为 " + order.status());
        }
    }

    private void logTransition(OrderView order, OrderStatus target, Long operator, String reason) {
        repository.appendStatusLog(order.id(), order.status(), target.name(), operator, reason);
    }

    private ApiException concurrentConflict() {
        return new ApiException(ErrorCode.CONFLICT, "订单已被其他操作更新，请刷新后重试");
    }

    private void validateIdempotencyKey(String value) {
        if (value == null || !value.matches("[A-Za-z0-9._:-]{8,128}")) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "Idempotency-Key 需为 8—128 位安全字符");
        }
    }

    private String newOrderNo() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), BUSINESS_ZONE);
        return "NP" + ORDER_TIME.format(now) + "%06d".formatted(secureRandom.nextInt(1_000_000));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to create immutable order snapshot", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
