package cn.nanpo.window.api.admin;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.farmer.FarmerViews.FarmerProfileView;
import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordCommand;
import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordView;
import cn.nanpo.window.api.farmer.FarmerViews.PlotView;
import cn.nanpo.window.api.farmer.FarmerViews.ProductCommand;
import cn.nanpo.window.api.farmer.FarmerViews.ProductManageView;
import cn.nanpo.window.application.FarmerWorkspaceService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/farmers")
public class AdminFarmerController {

    private final FarmerWorkspaceService service;

    public AdminFarmerController(FarmerWorkspaceService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<FarmerProfileView>> farmers() {
        return ApiResponse.success(service.farmers());
    }

    @GetMapping("/{farmerId}/plots")
    public ApiResponse<List<PlotView>> plots(@PathVariable long farmerId) {
        return ApiResponse.success(service.adminPlots(farmerId));
    }

    @GetMapping("/{farmerId}/products")
    public ApiResponse<List<ProductManageView>> products(@PathVariable long farmerId) {
        return ApiResponse.success(service.adminProducts(farmerId));
    }

    @PostMapping("/{farmerId}/products")
    public ApiResponse<ProductManageView> createProduct(
            @PathVariable long farmerId,
            @Valid @RequestBody ProductCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.createProductForFarmer(farmerId, command, actor, clientIp(request)));
    }

    @PutMapping("/{farmerId}/products/{productId}")
    public ApiResponse<ProductManageView> updateProduct(
            @PathVariable long farmerId,
            @PathVariable long productId,
            @Valid @RequestBody ProductCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(
                service.updateProductForFarmer(farmerId, productId, command, actor, clientIp(request)));
    }

    @PostMapping("/{farmerId}/products/{productId}/{action:publish|unpublish}")
    public ApiResponse<ProductManageView> setPublished(
            @PathVariable long farmerId,
            @PathVariable long productId,
            @PathVariable String action,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.setProductPublished(
                farmerId, productId, "publish".equals(action), actor, clientIp(request)));
    }

    @PostMapping("/{farmerId}/records")
    public ApiResponse<FarmRecordView> createRecord(
            @PathVariable long farmerId,
            @Valid @RequestBody FarmRecordCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.createRecordForFarmer(farmerId, command, actor, clientIp(request)));
    }

    @PostMapping("/{farmerId}/records/{recordId}/submit")
    public ApiResponse<FarmRecordView> submitRecord(
            @PathVariable long farmerId,
            @PathVariable long recordId,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.submitRecordForFarmer(farmerId, recordId, actor, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
    }
}
