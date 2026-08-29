package cn.nanpo.window.api.order;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.order.OrderViews.CreateOrderCommand;
import cn.nanpo.window.api.order.OrderViews.OrderView;
import cn.nanpo.window.api.order.OrderViews.PaymentReportCommand;
import cn.nanpo.window.application.OrderService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {

    private final OrderService service;

    public CustomerOrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<OrderView> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderCommand command,
            @AuthenticationPrincipal UserPrincipal customer,
            HttpServletRequest request) {
        return ApiResponse.success(service.create(idempotencyKey, command, customer, clientIp(request)));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderView> get(
            @PathVariable String orderNo,
            @AuthenticationPrincipal UserPrincipal customer) {
        return ApiResponse.success(service.customerOrder(orderNo, customer));
    }

    @PostMapping("/{orderNo}/payment-report")
    public ApiResponse<OrderView> reportPayment(
            @PathVariable String orderNo,
            @Valid @RequestBody PaymentReportCommand command,
            @AuthenticationPrincipal UserPrincipal customer,
            HttpServletRequest request) {
        return ApiResponse.success(service.reportPayment(orderNo, command.note(), customer, clientIp(request)));
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<OrderView> cancel(
            @PathVariable String orderNo,
            @AuthenticationPrincipal UserPrincipal customer,
            HttpServletRequest request) {
        return ApiResponse.success(service.cancel(orderNo, customer, clientIp(request)));
    }

    @PostMapping("/{orderNo}/complete")
    public ApiResponse<OrderView> complete(
            @PathVariable String orderNo,
            @AuthenticationPrincipal UserPrincipal customer,
            HttpServletRequest request) {
        return ApiResponse.success(service.complete(orderNo, customer, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
    }
}
