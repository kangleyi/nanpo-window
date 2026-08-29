package cn.nanpo.window.api.admin;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.order.OrderViews.OrderView;
import cn.nanpo.window.api.order.OrderViews.ReasonCommand;
import cn.nanpo.window.api.order.OrderViews.RefundOrderCommand;
import cn.nanpo.window.api.order.OrderViews.ShipOrderCommand;
import cn.nanpo.window.application.OrderService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService service;

    public AdminOrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<OrderView>> orders(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) Long farmerId) {
        return ApiResponse.success(service.adminOrders(status, farmerId));
    }

    @PostMapping("/{id}/prepare")
    public ApiResponse<OrderView> prepare(
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal operator,
            HttpServletRequest request) {
        return ApiResponse.success(service.markReadyByAdmin(id, operator, clientIp(request)));
    }

    @PostMapping("/{id}/confirm-payment")
    public ApiResponse<OrderView> confirmPayment(
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal operator,
            HttpServletRequest request) {
        return ApiResponse.success(service.confirmPayment(id, operator, clientIp(request)));
    }

    @PostMapping("/{id}/reject-payment")
    public ApiResponse<OrderView> rejectPayment(
            @PathVariable long id,
            @Valid @RequestBody ReasonCommand command,
            @AuthenticationPrincipal UserPrincipal operator,
            HttpServletRequest request) {
        return ApiResponse.success(service.rejectPayment(id, command.reason(), operator, clientIp(request)));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<OrderView> ship(
            @PathVariable long id,
            @Valid @RequestBody ShipOrderCommand command,
            @AuthenticationPrincipal UserPrincipal operator,
            HttpServletRequest request) {
        return ApiResponse.success(service.ship(id, command, operator, clientIp(request)));
    }

    @PostMapping("/{id}/refund")
    public ApiResponse<OrderView> refund(
            @PathVariable long id,
            @Valid @RequestBody RefundOrderCommand command,
            @AuthenticationPrincipal UserPrincipal operator,
            HttpServletRequest request) {
        return ApiResponse.success(service.refund(id, command, operator, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
    }
}
