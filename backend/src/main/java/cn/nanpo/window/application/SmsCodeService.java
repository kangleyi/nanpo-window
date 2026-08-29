package cn.nanpo.window.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.notification.SmsGateway;
import cn.nanpo.window.security.AuthProperties;
import cn.nanpo.window.security.TokenCodec;

@Service
public class SmsCodeService {

    private static final int MAX_ATTEMPTS = 5;

    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final AuthProperties properties;
    private final TokenCodec tokenCodec;
    private final SmsGateway smsGateway;
    private final Clock clock;

    @Autowired
    public SmsCodeService(
            AuthProperties properties,
            TokenCodec tokenCodec,
            SmsGateway smsGateway) {
        this(properties, tokenCodec, smsGateway, Clock.systemUTC());
    }

    SmsCodeService(
            AuthProperties properties,
            TokenCodec tokenCodec,
            SmsGateway smsGateway,
            Clock clock) {
        this.properties = properties;
        this.tokenCodec = tokenCodec;
        this.smsGateway = smsGateway;
        this.clock = clock;
    }

    public Instant issue(String phone) {
        Instant now = clock.instant();
        CodeEntry current = codes.get(phone);
        if (current != null && current.issuedAt().plusSeconds(60).isAfter(now)) {
            throw new ApiException(ErrorCode.CONFLICT, "验证码发送过于频繁，请稍后再试");
        }
        String code = properties.localCode() == null || properties.localCode().isBlank()
                ? randomSixDigits()
                : properties.localCode();
        Instant expiresAt = now.plus(properties.smsCodeTtl());
        codes.put(phone, new CodeEntry(tokenCodec.sha256(phone + ":" + code), now, expiresAt, 0));
        smsGateway.sendLoginCode(phone, code);
        return expiresAt;
    }

    public void verify(String phone, String code) {
        CodeEntry entry = codes.get(phone);
        Instant now = clock.instant();
        if (entry == null || entry.expiresAt().isBefore(now) || entry.attempts() >= MAX_ATTEMPTS) {
            codes.remove(phone);
            throw invalidCode();
        }
        if (!entry.hash().equals(tokenCodec.sha256(phone + ":" + code))) {
            codes.put(phone, new CodeEntry(
                    entry.hash(), entry.issuedAt(), entry.expiresAt(), entry.attempts() + 1));
            throw invalidCode();
        }
        codes.remove(phone);
    }

    private String randomSixDigits() {
        int value = secureRandom.nextInt(1_000_000);
        return "%06d".formatted(value);
    }

    private ApiException invalidCode() {
        return new ApiException(ErrorCode.AUTH_CODE_INVALID, "验证码无效或已过期");
    }

    private record CodeEntry(
            String hash,
            Instant issuedAt,
            Instant expiresAt,
            int attempts) {
    }
}
