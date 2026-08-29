package cn.nanpo.window.api.farmer;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.farmer.AiCopyViews.AiCopyView;
import cn.nanpo.window.api.farmer.AiCopyViews.ConfirmCopyCommand;
import cn.nanpo.window.api.farmer.AiCopyViews.GenerateCopyCommand;
import cn.nanpo.window.application.AiCopyService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/farmer")
public class AiCopyController {

    private final AiCopyService service;

    public AiCopyController(AiCopyService service) {
        this.service = service;
    }

    @GetMapping("/ai-copy")
    public ApiResponse<List<AiCopyView>> list(@AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.success(service.list(actor));
    }

    @PostMapping("/products/{productId}/ai-copy")
    public ApiResponse<AiCopyView> generate(
            @PathVariable long productId,
            @Valid @RequestBody GenerateCopyCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.generate(productId, command, actor, clientIp(request)));
    }

    @PostMapping("/ai-copy/{id}/confirm")
    public ApiResponse<AiCopyView> confirm(
            @PathVariable long id,
            @Valid @RequestBody ConfirmCopyCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.confirm(id, command, actor, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr() : forwardedFor.split(",", 2)[0].trim();
    }
}
