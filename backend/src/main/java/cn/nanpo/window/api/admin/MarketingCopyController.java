package cn.nanpo.window.api.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.admin.MarketingCopyViews.AiStatus;
import cn.nanpo.window.api.admin.MarketingCopyViews.ImageCopyCommand;
import cn.nanpo.window.api.admin.MarketingCopyViews.MarketingCopyResult;
import cn.nanpo.window.api.admin.MarketingCopyViews.OptimizeCopyCommand;
import cn.nanpo.window.application.MarketingCopyService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/marketing-copy")
public class MarketingCopyController {

    private final MarketingCopyService service;

    public MarketingCopyController(MarketingCopyService service) {
        this.service = service;
    }

    @GetMapping("/status")
    public ApiResponse<AiStatus> status() {
        return ApiResponse.success(service.status());
    }

    @PostMapping("/optimize")
    public ApiResponse<MarketingCopyResult> optimize(
            @Valid @RequestBody OptimizeCopyCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.optimize(command, actor, clientIp(request)));
    }

    @PostMapping("/from-image")
    public ApiResponse<MarketingCopyResult> fromImage(
            @Valid @RequestBody ImageCopyCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.fromImage(command, actor, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
    }
}
