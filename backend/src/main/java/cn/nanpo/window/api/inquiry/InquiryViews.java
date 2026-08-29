package cn.nanpo.window.api.inquiry;

import java.time.LocalDateTime;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class InquiryViews {

    private InquiryViews() {
    }

    public record InquiryCommand(
            @NotBlank @Pattern(regexp = "HOMESTAY|EXPERIENCE") String sourceType,
            @Positive long sourceId,
            @NotNull @FutureOrPresent LocalDateTime visitAt,
            @Min(1) @Max(100) int partySize,
            @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "请填写 11 位中国大陆手机号") String callbackPhone,
            @Size(max = 1000) String note) {
    }

    public record InquiryView(
            long id,
            String sourceType,
            long sourceId,
            String targetName,
            LocalDateTime visitAt,
            int partySize,
            String callbackPhone,
            String note,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}
