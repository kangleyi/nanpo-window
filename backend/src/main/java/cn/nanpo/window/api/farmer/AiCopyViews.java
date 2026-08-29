package cn.nanpo.window.api.farmer;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AiCopyViews {

    private AiCopyViews() {
    }

    public record GenerateCopyCommand(
            @NotBlank @Pattern(regexp = "PRODUCT_INTRO|FARMER_STORY|SHARE_COPY") String scene) {
    }

    public record ConfirmCopyCommand(
            @NotBlank @Size(max = 5000) String confirmedText) {
    }

    public record AiCopyView(
            long id,
            String scene,
            List<Long> sourceRecordIds,
            String modelName,
            String modelVersion,
            String outputText,
            String confirmedText,
            String status,
            LocalDateTime confirmedAt,
            LocalDateTime createdAt,
            long version) {
    }
}
