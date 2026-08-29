package cn.nanpo.window.api.farmer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class FarmerViews {

    private FarmerViews() {
    }

    public record FarmerProfileView(
            long id,
            String code,
            String name,
            String villageGroup,
            String introduction,
            String certificationStatus) {
    }

    public record PlotCommand(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 255) String location,
            @Size(max = 100) String area,
            @Size(max = 100) String mainCrop,
            @Size(max = 500) String coverUrl) {
    }

    public record PlotView(
            long id,
            String code,
            String location,
            String area,
            String mainCrop,
            String coverUrl,
            String status,
            LocalDateTime updatedAt) {
    }

    public record SkuCommand(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 160) String specification,
            @NotNull @DecimalMin(value = "0.01") BigDecimal unitPrice,
            @Size(max = 255) String stockNote) {
    }

    public record SkuManageView(
            long id,
            String code,
            String specification,
            BigDecimal unitPrice,
            String stockNote,
            boolean enabled) {
    }

    public record ProductCommand(
            Long plotId,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 100) String category,
            @NotBlank @Size(max = 100) String season,
            @NotBlank @Size(max = 2000) String summary,
            @NotBlank @Size(max = 500) String coverUrl,
            @NotEmpty @Size(max = 20) List<@Valid SkuCommand> skus) {
    }

    public record ProductManageView(
            long id,
            Long plotId,
            String name,
            String category,
            String season,
            String summary,
            String coverUrl,
            String status,
            List<SkuManageView> skus,
            long recordCount,
            LocalDateTime updatedAt) {
    }

    public record FarmRecordCommand(
            @NotNull Long productId,
            Long plotId,
            @NotBlank @Pattern(regexp = "PREPARATION|SOWING|FERTILIZING|GROWING|HARVEST|PROCESSING|PACKING|SHIPPING") String stage,
            @NotNull LocalDateTime occurredAt,
            @NotBlank @Size(max = 5000) String originalText,
            @AssertTrue(message = "必须确认记录来自真实生产过程") boolean truthConfirmed) {
    }

    public record FarmRecordView(
            long id,
            long productId,
            String productName,
            Long plotId,
            String plotCode,
            String stage,
            LocalDateTime occurredAt,
            String originalText,
            String confirmedText,
            boolean truthConfirmed,
            String status,
            String reviewNote,
            Long reviewerUserId,
            LocalDateTime reviewedAt,
            LocalDateTime publishedAt,
            long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<RecordMediaView> media) {
    }

    public record RecordMediaView(
            long id,
            String mediaType,
            String originalName,
            String contentType,
            String status,
            String contentUrl) {
    }

    public record FarmerDashboardView(
            FarmerProfileView farmer,
            long plotCount,
            long productCount,
            long recordCount,
            long pendingReviewCount,
            long publishedRecordCount) {
    }
}
