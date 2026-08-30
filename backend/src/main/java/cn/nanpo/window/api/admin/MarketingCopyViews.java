package cn.nanpo.window.api.admin;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class MarketingCopyViews {

    private MarketingCopyViews() {
    }

    public record OptimizeCopyCommand(
            @NotBlank @Size(max = 160) String productName,
            @Size(max = 100) String category,
            @NotBlank @Size(max = 2000) String originalCopy,
            @NotNull @Size(min = 1, max = 8) List<@NotBlank @Size(max = 100) String> sellingPoints,
            @NotBlank @Size(max = 100) String audience,
            @Pattern(regexp = "professional|friendly|energetic|premium") String tone,
            @Pattern(regexp = "general|ecommerce|social|short_video") String channel,
            @Min(40) @Max(500) int maxLength,
            @Size(max = 20) List<@NotBlank @Size(max = 50) String> prohibitedTerms) {
    }

    public record ImageCopyCommand(
            @NotBlank @Size(max = 9_000_000) String imageDataUrl,
            @Size(max = 160) String productName,
            @Size(max = 100) String category,
            @Size(max = 100) String season,
            @Size(max = 2000) String originalCopy,
            @Size(max = 8) List<@NotBlank @Size(max = 100) String> confirmedFacts,
            @Size(max = 8) List<@NotBlank @Size(max = 100) String> confirmedSellingPoints,
            @NotBlank @Size(max = 100) String audience,
            @Size(max = 300) String visualHint,
            @Pattern(regexp = "professional|friendly|energetic|premium") String tone,
            @Pattern(regexp = "general|ecommerce|social|short_video") String channel,
            @Min(40) @Max(500) int maxLength,
            @Size(max = 20) List<@NotBlank @Size(max = 50) String> prohibitedTerms) {
    }

    public record QualityReport(
            int score,
            int characterCount,
            int minLength,
            int maxLength,
            Map<String, Boolean> checks,
            List<String> warnings) {
    }

    public record VisualAnalysis(
            String description,
            String detectedProductName,
            String detectedCategory,
            List<String> detectedSellingPoints,
            List<String> visibleText,
            String productNameConfidence,
            List<String> productCandidates,
            String resolvedProductName,
            String nameSource,
            boolean nameConflict,
            boolean agriculturalProduct,
            String productStage,
            String scene,
            List<String> visibleFeatures,
            boolean generationBlocked,
            String conflictMessage) {
    }

    public record SellingPointCandidate(
            String text,
            String kind,
            String dimension,
            String basis,
            boolean needsConfirmation) {
    }

    public record MarketingCopyResult(
            String requestId,
            String headline,
            String optimizedCopy,
            List<String> sellingPoints,
            List<SellingPointCandidate> sellingPointCandidates,
            QualityReport qualityReport,
            VisualAnalysis visualAnalysis,
            Map<String, Object> meta) {
    }

    public record AiStatus(boolean configured, String provider, String textModel, String visionModel) {
    }
}
