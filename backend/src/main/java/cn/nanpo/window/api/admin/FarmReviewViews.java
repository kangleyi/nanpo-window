package cn.nanpo.window.api.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class FarmReviewViews {

    private FarmReviewViews() {
    }

    public record ApproveRecordCommand(
            @Size(max = 5000) String confirmedText,
            @Size(max = 1000) String reviewNote) {
    }

    public record RejectRecordCommand(
            @NotBlank @Size(max = 1000) String reviewNote) {
    }
}
