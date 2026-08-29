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

import cn.nanpo.window.api.admin.FarmReviewViews.ApproveRecordCommand;
import cn.nanpo.window.api.admin.FarmReviewViews.RejectRecordCommand;
import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordView;
import cn.nanpo.window.application.FarmerWorkspaceService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/reviews/records")
public class FarmReviewController {

    private final FarmerWorkspaceService service;

    public FarmReviewController(FarmerWorkspaceService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<FarmRecordView>> queue(
            @RequestParam(defaultValue = "PENDING_REVIEW") String status) {
        return ApiResponse.success(service.reviewQueue(status));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<FarmRecordView> approve(
            @PathVariable long id,
            @Valid @RequestBody ApproveRecordCommand command,
            @AuthenticationPrincipal UserPrincipal reviewer,
            HttpServletRequest request) {
        return ApiResponse.success(service.approveRecord(id, command, reviewer, clientIp(request)));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<FarmRecordView> reject(
            @PathVariable long id,
            @Valid @RequestBody RejectRecordCommand command,
            @AuthenticationPrincipal UserPrincipal reviewer,
            HttpServletRequest request) {
        return ApiResponse.success(service.rejectRecord(id, command, reviewer, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
    }
}
