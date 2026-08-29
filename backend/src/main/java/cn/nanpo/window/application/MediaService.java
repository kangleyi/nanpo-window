package cn.nanpo.window.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.nanpo.window.api.media.MediaViews.MediaView;
import cn.nanpo.window.api.media.MediaViews.UploadTicketCommand;
import cn.nanpo.window.api.media.MediaViews.UploadTicketView;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.persistence.MediaRepository;
import cn.nanpo.window.infrastructure.persistence.MediaRepository.MediaRow;
import cn.nanpo.window.infrastructure.storage.ObjectStorage;
import cn.nanpo.window.infrastructure.storage.ObjectStorage.StoredObject;
import cn.nanpo.window.infrastructure.storage.ObjectStorage.UploadRequest;
import cn.nanpo.window.infrastructure.storage.ObjectStorage.UploadTicket;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class MediaService {

    private static final Map<String, Set<String>> CONTENT_TYPES = Map.of(
            "IMAGE", Set.of("image/jpeg", "image/png", "image/webp"),
            "AUDIO", Set.of("audio/mpeg", "audio/wav", "audio/mp4"),
            "VIDEO", Set.of("video/mp4", "video/webm"));
    private static final Map<String, Long> SIZE_LIMITS = Map.of(
            "IMAGE", 10L * 1024 * 1024,
            "AUDIO", 30L * 1024 * 1024,
            "VIDEO", 100L * 1024 * 1024);

    private final MediaRepository repository;
    private final ObjectStorage objectStorage;
    private final AuditService auditService;

    public MediaService(MediaRepository repository, ObjectStorage objectStorage, AuditService auditService) {
        this.repository = repository;
        this.objectStorage = objectStorage;
        this.auditService = auditService;
    }

    @Transactional
    public UploadTicketView createTicket(
            UploadTicketCommand command, UserPrincipal actor, String ipAddress) {
        validateTicket(command);
        boolean contentOperator = actor.roles().contains("CONTENT_OPERATOR") || actor.roles().contains("SUPER_ADMIN");
        if (command.recordId() == null && !contentOperator) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "只有内容管理员可以上传上架素材");
        }
        if (command.recordId() != null && !contentOperator && !repository.ownsRecord(actor.id(), command.recordId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "只能为本人的生产记录上传素材");
        }
        UploadTicket ticket = objectStorage.createUploadTicket(new UploadRequest(
                actor.id(), command.mediaType(), command.contentType(), command.sizeBytes(), command.checksumSha256(),
                command.originalName()));
        long id = repository.create(actor.id(), command, ticket.storageKey(), ticket.expiresAt());
        if (command.recordId() != null) repository.attachToRecord(command.recordId(), id);
        auditService.record(actor.id(), "MEDIA_TICKET_CREATE", "MEDIA_ASSET", String.valueOf(id), ipAddress);
        String uploadUrl = ticket.uploadUrl() == null || ticket.uploadUrl().isBlank()
                ? "/api/media/" + id + "/content" : ticket.uploadUrl();
        return new UploadTicketView(view(id), uploadUrl, ticket.headers(), ticket.expiresAt());
    }

    @Transactional
    public MediaView upload(long id, byte[] content, UserPrincipal actor, String ipAddress) {
        MediaRow media = owned(id, actor);
        if ("READY".equals(media.status())) return MediaRepository.view(media);
        if (Instant.now().isAfter(media.expiresAt())) {
            throw new ApiException(ErrorCode.CONFLICT, "上传凭证已过期，请重新创建");
        }
        if (content.length != media.sizeBytes()) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "文件大小与上传凭证不一致");
        }
        if (!matchesSignature(media.contentType(), content)) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "文件内容与声明的媒体类型不一致");
        }
        objectStorage.put(media.storageKey(), content);
        if (!repository.markUploaded(id, media.version())) {
            throw new ApiException(ErrorCode.CONFLICT, "媒体状态已更新，请刷新后重试");
        }
        auditService.record(actor.id(), "MEDIA_UPLOAD", "MEDIA_ASSET", String.valueOf(id), ipAddress);
        return view(id);
    }

    @Transactional
    public MediaView complete(long id, UserPrincipal actor, String ipAddress) {
        MediaRow media = owned(id, actor);
        if ("READY".equals(media.status()) || "FAILED".equals(media.status())) return MediaRepository.view(media);
        if (!"CREATED".equals(media.status()) && !"UPLOADED".equals(media.status())) {
            throw new ApiException(ErrorCode.CONFLICT, "文件尚未上传");
        }
        StoredObject stored;
        try {
            stored = objectStorage.stat(media.storageKey());
        } catch (RuntimeException exception) {
            repository.markFailed(id, media.version(), "对象存储校验失败");
            return auditedResult(id, actor, ipAddress);
        }
        String failure = stored.sizeBytes() != media.sizeBytes() ? "文件大小校验失败"
                : media.checksumSha256() != null && !media.checksumSha256().equalsIgnoreCase(stored.checksumSha256())
                        ? "文件摘要校验失败" : null;
        if (failure != null) {
            repository.markFailed(id, media.version(), failure);
        } else {
            if ("CREATED".equals(media.status())) {
                if (!repository.markUploaded(id, media.version())) {
                    throw new ApiException(ErrorCode.CONFLICT, "媒体状态已更新，请刷新后重试");
                }
                media = repository.find(id).orElseThrow();
            }
            if (!repository.markReady(id, media.version(), stored.checksumSha256())) {
                throw new ApiException(ErrorCode.CONFLICT, "媒体状态已更新，请刷新后重试");
            }
        }
        return auditedResult(id, actor, ipAddress);
    }

    private MediaView auditedResult(long id, UserPrincipal actor, String ipAddress) {
        MediaView result = view(id);
        auditService.record(actor.id(), "READY".equals(result.status()) ? "MEDIA_READY" : "MEDIA_FAILED",
                "MEDIA_ASSET", String.valueOf(id), ipAddress);
        return result;
    }

    @Transactional(readOnly = true)
    public MediaView status(long id, UserPrincipal actor) {
        return MediaRepository.view(owned(id, actor));
    }

    @Transactional(readOnly = true)
    public MediaContent content(long id, UserPrincipal actor) {
        MediaRow media = repository.find(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "媒体不存在"));
        boolean reviewer = actor.roles().contains("REVIEWER") || actor.roles().contains("SUPER_ADMIN");
        if (media.ownerUserId() != actor.id() && !(reviewer && repository.isRecordMedia(id))) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "无权访问该媒体");
        }
        if (!"READY".equals(media.status())) {
            throw new ApiException(ErrorCode.CONFLICT, "媒体尚未处理完成");
        }
        return new MediaContent(media.originalName(), media.contentType(), objectStorage.read(media.storageKey()));
    }

    @Transactional(readOnly = true)
    public MediaContent publicContent(long id) {
        MediaRow media = repository.find(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "媒体不存在"));
        if (!"READY".equals(media.status()) || repository.isRecordMedia(id)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "公开媒体不存在");
        }
        return new MediaContent(media.originalName(), media.contentType(), objectStorage.read(media.storageKey()));
    }

    private MediaView view(long id) {
        return MediaRepository.view(repository.find(id).orElseThrow());
    }

    private MediaRow owned(long id, UserPrincipal actor) {
        MediaRow media = repository.find(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "媒体不存在"));
        if (media.ownerUserId() != actor.id() && !actor.roles().contains("SUPER_ADMIN")) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "无权访问该媒体");
        }
        return media;
    }

    private void validateTicket(UploadTicketCommand command) {
        if (!CONTENT_TYPES.getOrDefault(command.mediaType(), Set.of()).contains(command.contentType())) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "不支持的媒体格式");
        }
        if (command.sizeBytes() > SIZE_LIMITS.get(command.mediaType())) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "媒体文件超过大小限制");
        }
        if (command.originalName().contains("/") || command.originalName().contains("\\")) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "文件名不合法");
        }
    }

    private boolean matchesSignature(String contentType, byte[] value) {
        if (value.length < 4) return false;
        return switch (contentType) {
            case "image/jpeg" -> u(value[0]) == 0xff && u(value[1]) == 0xd8 && u(value[2]) == 0xff;
            case "image/png" -> u(value[0]) == 0x89 && value[1] == 'P' && value[2] == 'N' && value[3] == 'G';
            case "image/webp" -> ascii(value, 0, "RIFF") && value.length >= 12 && ascii(value, 8, "WEBP");
            case "audio/mpeg" -> ascii(value, 0, "ID3") || (u(value[0]) == 0xff && (u(value[1]) & 0xe0) == 0xe0);
            case "audio/wav" -> ascii(value, 0, "RIFF") && value.length >= 12 && ascii(value, 8, "WAVE");
            case "audio/mp4", "video/mp4" -> value.length >= 12 && ascii(value, 4, "ftyp");
            case "video/webm" -> u(value[0]) == 0x1a && u(value[1]) == 0x45 && u(value[2]) == 0xdf && u(value[3]) == 0xa3;
            default -> false;
        };
    }

    private boolean ascii(byte[] value, int offset, String expected) {
        byte[] bytes = expected.getBytes(StandardCharsets.US_ASCII);
        if (value.length < offset + bytes.length) return false;
        for (int index = 0; index < bytes.length; index++) if (value[offset + index] != bytes[index]) return false;
        return true;
    }

    private int u(byte value) {
        return value & 0xff;
    }

    public record MediaContent(String originalName, String contentType, byte[] content) {
    }
}
