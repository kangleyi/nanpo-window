package cn.nanpo.window.infrastructure.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;

/**
 * Tencent Cloud COS storage. The browser uploads directly with a short-lived PUT URL;
 * the permanent SecretKey is only held by the backend.
 */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "cos")
public class CosObjectStorage implements ObjectStorage, DisposableBean {

    private static final String CHECKSUM_HEADER = "x-cos-meta-sha256";

    private final COSClient client;
    private final String bucket;
    private final String uploadPrefix;
    private final Duration uploadUrlTtl;

    public CosObjectStorage(
            @Value("${app.storage.cos.secret-id}") String secretId,
            @Value("${app.storage.cos.secret-key}") String secretKey,
            @Value("${app.storage.cos.secret-token:}") String secretToken,
            @Value("${app.storage.cos.region}") String region,
            @Value("${app.storage.cos.bucket}") String bucket,
            @Value("${app.storage.cos.upload-prefix:xiangjian-xicun/uploads}") String uploadPrefix,
            @Value("${app.storage.cos.upload-url-ttl:PT5M}") Duration uploadUrlTtl) {
        requireText(secretId, "COS_SECRET_ID");
        requireText(secretKey, "COS_SECRET_KEY");
        requireText(region, "COS_REGION");
        requireText(bucket, "COS_BUCKET");
        if (!bucket.matches("^[a-z0-9][a-z0-9.-]*-[0-9]+$")) {
            throw new IllegalArgumentException("COS_BUCKET 必须使用 bucket-appid 完整格式");
        }
        if (uploadUrlTtl.isNegative() || uploadUrlTtl.isZero() || uploadUrlTtl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("COS_UPLOAD_URL_TTL 必须大于 0 且不超过 1 小时");
        }
        this.bucket = bucket;
        this.uploadPrefix = normalizePrefix(uploadPrefix);
        this.uploadUrlTtl = uploadUrlTtl;
        COSCredentials credentials = StringUtils.hasText(secretToken)
                ? new BasicSessionCredentials(secretId, secretKey, secretToken)
                : new BasicCOSCredentials(secretId, secretKey);
        this.client = new COSClient(credentials, new ClientConfig(new Region(region)));
    }

    @Override
    public UploadTicket createUploadTicket(UploadRequest request) {
        Instant expiresAt = Instant.now().plus(uploadUrlTtl);
        String key = objectKey(request);
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", request.contentType());
        if (StringUtils.hasText(request.checksumSha256())) {
            headers.put(CHECKSUM_HEADER, request.checksumSha256().toLowerCase());
        }

        GeneratePresignedUrlRequest presign = new GeneratePresignedUrlRequest(bucket, key, HttpMethodName.PUT);
        presign.setExpiration(Date.from(expiresAt));
        headers.forEach(presign::putCustomRequestHeader);
        String uploadUrl = client.generatePresignedUrl(presign).toString();
        return new UploadTicket(key, uploadUrl, Map.copyOf(headers), expiresAt);
    }

    @Override
    public void put(String storageKey, byte[] content) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        metadata.addUserMetadata("sha256", sha256(content));
        client.putObject(new PutObjectRequest(bucket, storageKey, new ByteArrayInputStream(content), metadata));
    }

    @Override
    public StoredObject stat(String storageKey) {
        ObjectMetadata metadata = client.getObjectMetadata(bucket, storageKey);
        return new StoredObject(metadata.getContentLength(), metadata.getUserMetadata().get("sha256"));
    }

    @Override
    public byte[] read(String storageKey) {
        try (COSObject object = client.getObject(bucket, storageKey);
                InputStream content = object.getObjectContent()) {
            return content.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取 COS 对象", exception);
        }
    }

    @Override
    public void destroy() {
        client.shutdown();
    }

    private String objectKey(UploadRequest request) {
        String date = LocalDate.now(ZoneOffset.UTC).toString().replace("-", "/");
        String suffix = safeExtension(request.originalName());
        String relative = date + "/users/" + request.ownerUserId() + "/" + UUID.randomUUID() + suffix;
        return uploadPrefix.isEmpty() ? relative : uploadPrefix + "/" + relative;
    }

    private String safeExtension(String originalName) {
        int dot = originalName == null ? -1 : originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) return "";
        String extension = originalName.substring(dot + 1).toLowerCase();
        return extension.matches("[a-z0-9]{1,10}") ? "." + extension : "";
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) return "";
        String normalized = prefix.trim().replaceAll("^/+|/+$", "");
        if (normalized.contains("..") || normalized.contains("\\")) {
            throw new IllegalArgumentException("COS_UPLOAD_PREFIX 不合法");
        }
        return normalized;
    }

    private void requireText(String value, String environmentName) {
        if (!StringUtils.hasText(value)) throw new IllegalArgumentException(environmentName + " 未配置");
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行时不支持 SHA-256", exception);
        }
    }
}
