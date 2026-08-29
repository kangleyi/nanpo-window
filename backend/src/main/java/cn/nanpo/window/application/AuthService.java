package cn.nanpo.window.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.persistence.AuthRepository;
import cn.nanpo.window.infrastructure.persistence.AuthRepository.RefreshSession;
import cn.nanpo.window.security.AuthProperties;
import cn.nanpo.window.security.TokenCodec;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class AuthService {

    private final SmsCodeService smsCodeService;
    private final AuthRepository repository;
    private final AuditService auditService;
    private final AuthProperties properties;
    private final TokenCodec tokenCodec;
    private final Clock clock;

    @Autowired
    public AuthService(
            SmsCodeService smsCodeService,
            AuthRepository repository,
            AuditService auditService,
            AuthProperties properties,
            TokenCodec tokenCodec) {
        this(smsCodeService, repository, auditService, properties, tokenCodec, Clock.systemUTC());
    }

    AuthService(
            SmsCodeService smsCodeService,
            AuthRepository repository,
            AuditService auditService,
            AuthProperties properties,
            TokenCodec tokenCodec,
            Clock clock) {
        this.smsCodeService = smsCodeService;
        this.repository = repository;
        this.auditService = auditService;
        this.properties = properties;
        this.tokenCodec = tokenCodec;
        this.clock = clock;
    }

    public Instant sendCode(String phone) {
        return smsCodeService.issue(phone);
    }

    @Transactional
    public AuthTokens login(String phone, String code, String ipAddress) {
        smsCodeService.verify(phone, code);
        UserPrincipal principal = repository.findActiveUserByPhone(phone)
                .orElseGet(() -> createCustomer(phone));
        AuthTokens tokens = createSession(principal);
        repository.updateLastLogin(principal.id());
        auditService.record(principal.id(), "AUTH_LOGIN", "USER_ACCOUNT", String.valueOf(principal.id()), ipAddress);
        return tokens;
    }

    @Transactional
    public AuthTokens refresh(String refreshToken, String ipAddress) {
        String refreshHash = tokenCodec.sha256(refreshToken);
        RefreshSession session = repository.findRefreshSession(refreshHash)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_TOKEN_INVALID, "刷新令牌无效或已过期"));
        UserPrincipal principal = repository.findActiveUserById(session.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_TOKEN_INVALID, "账号不可用"));
        repository.revokeSession(session.id());
        AuthTokens tokens = createSession(principal);
        auditService.record(principal.id(), "AUTH_REFRESH", "AUTH_SESSION", String.valueOf(session.id()), ipAddress);
        return tokens;
    }

    @Transactional
    public void logout(UserPrincipal principal, String accessToken, String ipAddress) {
        repository.revokeByAccessHash(tokenCodec.sha256(accessToken));
        auditService.record(principal.id(), "AUTH_LOGOUT", "USER_ACCOUNT", String.valueOf(principal.id()), ipAddress);
    }

    private UserPrincipal createCustomer(String phone) {
        try {
            long userId = repository.createCustomer(phone);
            return repository.findActiveUserById(userId).orElseThrow();
        } catch (DuplicateKeyException exception) {
            return repository.findActiveUserByPhone(phone).orElseThrow();
        }
    }

    private AuthTokens createSession(UserPrincipal principal) {
        String accessToken = tokenCodec.newToken();
        String refreshToken = tokenCodec.newToken();
        Instant now = clock.instant();
        Instant accessExpiresAt = now.plus(properties.accessTokenTtl());
        Instant refreshExpiresAt = now.plus(properties.refreshTokenTtl());
        repository.createSession(
                principal.id(),
                tokenCodec.sha256(accessToken),
                tokenCodec.sha256(refreshToken),
                accessExpiresAt,
                refreshExpiresAt);
        return new AuthTokens(
                "Bearer",
                accessToken,
                refreshToken,
                accessExpiresAt,
                refreshExpiresAt,
                principal);
    }

    public record AuthTokens(
            String tokenType,
            String accessToken,
            String refreshToken,
            Instant accessExpiresAt,
            Instant refreshExpiresAt,
            UserPrincipal user) {
    }
}
