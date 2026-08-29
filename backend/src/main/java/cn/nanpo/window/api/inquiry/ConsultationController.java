package cn.nanpo.window.api.inquiry;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.inquiry.InquiryViews.InquiryCommand;
import cn.nanpo.window.api.inquiry.InquiryViews.InquiryView;
import cn.nanpo.window.application.ConsultationService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class ConsultationController {

    private final ConsultationService service;

    public ConsultationController(ConsultationService service) {
        this.service = service;
    }

    @PostMapping("/api/public/inquiries")
    public ApiResponse<InquiryView> create(@Valid @RequestBody InquiryCommand command) {
        return ApiResponse.success(service.create(command));
    }

    @GetMapping("/api/admin/inquiries")
    public ApiResponse<List<InquiryView>> list(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "ALL") String sourceType) {
        return ApiResponse.success(service.adminList(status, sourceType));
    }

    @PostMapping("/api/admin/inquiries/{id}/{action:contacted|closed}")
    public ApiResponse<InquiryView> updateStatus(
            @PathVariable long id,
            @PathVariable String action,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateStatus(id, action, actor, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr() : forwardedFor.split(",", 2)[0].trim();
    }
}
