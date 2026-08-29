package cn.nanpo.window.api.auth;

import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.application.AuthService;
import cn.nanpo.window.application.AuthService.AuthTokens;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@RestController
public class AuthController {

    private static final String PHONE_PATTERN = "^1\\d{10}$";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/sms/send")
    public ApiResponse<SmsSentView> sendSms(@Valid @RequestBody SendSmsRequest request) {
        Instant expiresAt = authService.sendCode(request.phone());
        return ApiResponse.success(new SmsSentView(expiresAt));
    }

    @PostMapping("/api/auth/sms/login")
    public ApiResponse<AuthTokens> login(
            @Valid @RequestBody SmsLoginRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.login(request.phone(), request.code(), clientIp(httpRequest)));
    }

    @PostMapping("/api/auth/refresh")
    public ApiResponse<AuthTokens> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.refresh(request.refreshToken(), clientIp(httpRequest)));
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            HttpServletRequest httpRequest) {
        authService.logout(principal, bearerToken(authorization), clientIp(httpRequest));
        return ApiResponse.success(null);
    }

    @GetMapping("/api/me")
    public ApiResponse<UserPrincipal> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(principal);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID, "缺少访问令牌");
        }
        return authorization.substring(7);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record SendSmsRequest(
            @NotBlank @Pattern(regexp = PHONE_PATTERN, message = "请填写 11 位中国大陆手机号") String phone) {
    }

    public record SmsLoginRequest(
            @NotBlank @Pattern(regexp = PHONE_PATTERN, message = "请填写 11 位中国大陆手机号") String phone,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "请填写 6 位验证码") String code) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record SmsSentView(Instant expiresAt) {
    }
}

