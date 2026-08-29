package cn.nanpo.window.api.media;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class MediaViews {

    private MediaViews() {
    }

    public record UploadTicketCommand(
            @NotBlank @Pattern(regexp = "IMAGE|AUDIO|VIDEO") String mediaType,
            @NotBlank @Size(max = 160) String contentType,
            @Positive long sizeBytes,
            @NotBlank @Size(max = 255) String originalName,
            @Pattern(regexp = "[0-9a-fA-F]{64}") String checksumSha256,
            Long recordId) {
    }

    public record MediaView(
            long id,
            String mediaType,
            String originalName,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            String status,
            String failureReason,
            Instant expiresAt,
            LocalDateTime uploadedAt,
            LocalDateTime createdAt,
            long version) {
    }

    public record UploadTicketView(
            MediaView media,
            String uploadUrl,
            Map<String, String> headers,
            Instant expiresAt) {
    }
}
