package cn.nanpo.window.infrastructure.ai;

import java.time.LocalDateTime;
import java.util.List;

public interface AiCopyProvider {

    GeneratedCopy generate(CopyRequest request);

    record CopyRequest(
            String scene,
            String productName,
            String farmerName,
            String season,
            List<SourceFact> facts) {
    }

    record SourceFact(long recordId, String stage, LocalDateTime occurredAt, String confirmedText) {
    }

    record GeneratedCopy(String text, String modelName, String modelVersion) {
    }
}
