package cn.nanpo.window.api.farmer;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.order.OrderViews.FarmerOrderView;
import cn.nanpo.window.api.order.OrderViews.OrderView;
import cn.nanpo.window.application.OrderService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/farmer/orders")
public class FarmerOrderController {

    private final OrderService service;

    public FarmerOrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<FarmerOrderView>> orders(@AuthenticationPrincipal UserPrincipal farmer) {
        return ApiResponse.success(service.farmerOrders(farmer));
    }

    @PostMapping("/{id}/prepare")
    public ApiResponse<OrderView> prepare(
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal farmer,
            HttpServletRequest request) {
        return ApiResponse.success(service.markReady(id, farmer, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
    }
}
