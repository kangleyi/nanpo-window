package cn.nanpo.window.infrastructure.ai;

import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class LocalTemplateAiCopyProvider implements AiCopyProvider {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy年M月d日");

    @Override
    public GeneratedCopy generate(CopyRequest request) {
        String facts = request.facts().stream()
                .limit(4)
                .map(fact -> DATE.format(fact.occurredAt()) + "，" + fact.confirmedText())
                .reduce((left, right) -> left + "；" + right)
                .orElseThrow();
        String prefix = switch (request.scene()) {
            case "SHARE_COPY" -> "分享一份来自大南坡村的真实收获：";
            case "FARMER_STORY" -> request.farmerName() + "一直用生产记录讲述土地上的变化。";
            default -> request.productName() + "来自大南坡村，由" + request.farmerName() + "种植。";
        };
        return new GeneratedCopy(prefix + " 已审核记录显示：" + facts + "。内容需由农户确认后使用。",
                "local-factual-template", "1.0");
    }
}
