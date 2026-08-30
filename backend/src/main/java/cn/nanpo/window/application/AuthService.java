package cn.nanpo.window.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.persistence.AuthRepository;
import cn.nanpo.window.infrastructure.persistence.AuthRepository.PasswordAccount;
import cn.nanpo.window.infrastructure.persistence.AuthRepository.RefreshSession;
import cn.nanpo.window.security.AuthProperties;
import cn.nanpo.window.security.TokenCodec;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class AuthService {

    private final AuthRepository repository;
    private final AuditService auditService;
    private final AuthProperties properties;
    private final TokenCodec tokenCodec;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Autowired
    public AuthService(
            AuthRepository repository,
            AuditService auditService,
            AuthProperties properties,
            TokenCodec tokenCodec,
            PasswordEncoder passwordEncoder) {
        this(repository, auditService, properties, tokenCodec, passwordEncoder, Clock.systemUTC());
    }

    AuthService(
            AuthRepository repository,
            AuditService auditService,
            AuthProperties properties,
            TokenCodec tokenCodec,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.repository = repository;
        this.auditService = auditService;
        this.properties = properties;
        this.tokenCodec = tokenCodec;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public AuthTokens register(String phone, String password, String ipAddress) {
        long userId;
        try {
            userId = repository.createCustomer(phone, passwordEncoder.encode(password));
        } catch (DuplicateKeyException exception) {
            throw new ApiException(ErrorCode.CONFLICT, "该手机号已注册，请直接登录");
        }
        UserPrincipal principal = repository.findActiveUserById(userId).orElseThrow();
        AuthTokens tokens = createSession(principal);
        repository.updateLastLogin(principal.id());
        auditService.record(principal.id(), "AUTH_REGISTER", "USER_ACCOUNT", String.valueOf(principal.id()), ipAddress);
        return tokens;
    }

    @Transactional
    public AuthTokens login(String phone, String password, String ipAddress) {
        PasswordAccount account = repository.findActivePasswordAccountByPhone(phone)
                .orElseThrow(this::invalidCredentials);
        if (account.passwordHash() == null || !passwordEncoder.matches(password, account.passwordHash())) {
            throw invalidCredentials();
        }
        UserPrincipal principal = repository.findActiveUserById(account.id())
                .orElseThrow(this::invalidCredentials);
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

    private ApiException invalidCredentials() {
        return new ApiException(ErrorCode.AUTH_CREDENTIALS_INVALID, "手机号或密码错误");
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
