package cn.nanpo.window.api.farmer;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordCommand;
import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordView;
import cn.nanpo.window.api.farmer.FarmerViews.FarmerDashboardView;
import cn.nanpo.window.api.farmer.FarmerViews.PlotCommand;
import cn.nanpo.window.api.farmer.FarmerViews.PlotView;
import cn.nanpo.window.api.farmer.FarmerViews.ProductCommand;
import cn.nanpo.window.api.farmer.FarmerViews.ProductManageView;
import cn.nanpo.window.application.FarmerWorkspaceService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/farmer")
public class FarmerWorkspaceController {

    private final FarmerWorkspaceService service;

    public FarmerWorkspaceController(FarmerWorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public ApiResponse<FarmerDashboardView> dashboard(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.success(service.dashboard(actor));
    }

    @GetMapping("/plots")
    public ApiResponse<List<PlotView>> plots(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.success(service.plots(actor));
    }

    @PostMapping("/plots")
    public ApiResponse<PlotView> createPlot(
            @Valid @RequestBody PlotCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.createPlot(command, actor, clientIp(request)));
    }

    @GetMapping("/products")
    public ApiResponse<List<ProductManageView>> products(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.success(service.products(actor));
    }

    @PostMapping("/products")
    public ApiResponse<ProductManageView> createProduct(
            @Valid @RequestBody ProductCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.createProduct(command, actor, clientIp(request)));
    }

    @GetMapping("/records")
    public ApiResponse<List<FarmRecordView>> records(
            @RequestParam(defaultValue = "ALL") String status,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.success(service.records(actor, status));
    }

    @PostMapping("/records")
    public ApiResponse<FarmRecordView> createRecord(
            @Valid @RequestBody FarmRecordCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.createRecord(command, actor, clientIp(request)));
    }

    @PostMapping("/records/{id}/submit")
    public ApiResponse<FarmRecordView> submitRecord(
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.submitRecord(id, actor, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
    }
}
