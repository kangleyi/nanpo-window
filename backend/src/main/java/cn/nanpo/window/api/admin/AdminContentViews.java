package cn.nanpo.window.api.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AdminContentViews {

    private AdminContentViews() {
    }

    public record HomestayCommand(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 100) String type,
            @NotBlank @Size(max = 2000) String summary,
            @NotBlank @Size(max = 100) String capacity,
            @NotBlank @Size(max = 100) String price,
            @NotBlank @Size(max = 500) String coverUrl,
            @Size(max = 32) String consultationPhone,
            @Min(0) Integer sortOrder) {
    }

    public record HomestayAdminView(
            long id,
            String name,
            String type,
            String summary,
            String capacity,
            String price,
            String coverUrl,
            String consultationPhone,
            int sortOrder,
            String status,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt) {
    }

    public record ExperienceCommand(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 100) String type,
            @NotBlank @Size(max = 100) String season,
            @NotBlank @Size(max = 100) String duration,
            @NotBlank @Size(max = 2000) String summary,
            @NotBlank @Size(max = 100) String price,
            @NotBlank @Size(max = 500) String coverUrl,
            @Size(max = 500) String videoUrl,
            @Size(max = 2000) String bookingNotes,
            @Min(0) Integer sortOrder) {
    }

    public record ExperienceAdminView(
            long id,
            String name,
            String type,
            String season,
            String duration,
            String summary,
            String price,
            String coverUrl,
            String videoUrl,
            String bookingNotes,
            int sortOrder,
            String status,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt) {
    }

    public record AttractionCommand(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 100) String category,
            @NotNull @DecimalMin("0.0") BigDecimal distanceKm,
            @NotNull @Min(0) Integer driveMinutes,
            @NotBlank @Size(max = 2000) String summary,
            @NotBlank @Size(max = 500) String coverUrl,
            @NotBlank @Size(max = 1000) String mapUrl,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 100) String> highlights,
            @Min(0) Integer sortOrder) {
    }

    public record AttractionAdminView(
            long id,
            String name,
            String category,
            BigDecimal distanceKm,
            int driveMinutes,
            String summary,
            String coverUrl,
            String mapUrl,
            List<String> highlights,
            int sortOrder,
            String status,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt) {
    }

    public record ContentStatusView(long id, String kind, String status, LocalDateTime publishedAt) {
    }
}
