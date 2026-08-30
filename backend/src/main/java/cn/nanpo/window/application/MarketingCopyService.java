package cn.nanpo.window.application;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.nanpo.window.api.admin.MarketingCopyViews.AiStatus;
import cn.nanpo.window.api.admin.MarketingCopyViews.ImageCopyCommand;
import cn.nanpo.window.api.admin.MarketingCopyViews.MarketingCopyResult;
import cn.nanpo.window.api.admin.MarketingCopyViews.OptimizeCopyCommand;
import cn.nanpo.window.api.admin.MarketingCopyViews.QualityReport;
import cn.nanpo.window.api.admin.MarketingCopyViews.SellingPointCandidate;
import cn.nanpo.window.api.admin.MarketingCopyViews.VisualAnalysis;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class MarketingCopyService {

    private static final String ZHIPU_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final int MAX_IMAGE_BYTES = 6 * 1024 * 1024;
    private static final Pattern IMAGE_DATA_URL = Pattern.compile(
            "^data:image/(jpeg|png|webp);base64,([A-Za-z0-9+/=\\r\\n]+)$",
            Pattern.CASE_INSENSITIVE);
    private static final Map<String, String> RISKY_REPLACEMENTS = Map.ofEntries(
            Map.entry("100%永久", "持久"),
            Map.entry("唯一一款", "一款"),
            Map.entry("全网最低", "价格更友好"),
            Map.entry("行业第一", "表现出色"),
            Map.entry("全球第一", "表现出色"),
            Map.entry("唯一", "别具一格"),
            Map.entry("100%", "更有保障"),
            Map.entry("绝对", "更加"),
            Map.entry("永久", "长期"),
            Map.entry("万能", "多场景"),
            Map.entry("无敌", "亮眼"),
            Map.entry("顶级", "高品质"));
    private static final Map<String, String> UNVERIFIED_CLAIM_REPLACEMENTS = Map.ofEntries(
            Map.entry("香甜多汁", "当季风味值得期待"),
            Map.entry("皮薄肉厚", "果形自然饱满"),
            Map.entry("清脆鲜嫩", "适合家常烹饪"),
            Map.entry("颗粒饱满", "颗粒状态清晰可见"),
            Map.entry("质朴醇香", "质朴外观"),
            Map.entry("醇香", "乡村风味"),
            Map.entry("酥脆", "适合日常分享"),
            Map.entry("脆香", "乡村风味"),
            Map.entry("清香", "乡村风味"),
            Map.entry("浓香", "乡村风味"),
            Map.entry("当天采摘", "当季供应"),
            Map.entry("当日采收", "当季供应"),
            Map.entry("当季现采", "当季供应"),
            Map.entry("当季新收", "当季好物"),
            Map.entry("当季新晒", "当季好物"),
            Map.entry("现摘现发", "按当季批次供应"),
            Map.entry("有机种植", "用心种植"),
            Map.entry("生态种植", "用心种植"),
            Map.entry("无农药", "种植信息可追溯"),
            Map.entry("无污染", "产地环境值得关注"),
            Map.entry("零农残", "品质信息待实物确认"),
            Map.entry("不打蜡", "保留自然外观"),
            Map.entry("零添加", "配料信息清晰"),
            Map.entry("古法制作", "传统风味表达"),
            Map.entry("古法工艺", "传统风味表达"),
            Map.entry("手工制作", "用心制作"),
            Map.entry("古法晾晒", "干燥后的状态"),
            Map.entry("古法风干", "干燥后的状态"),
            Map.entry("阳光晾晒", "干燥后的状态"),
            Map.entry("自然晾晒", "干燥后的状态"),
            Map.entry("手工晾晒", "干燥后的状态"),
            Map.entry("产地直发", "来自产地的乡村好物"),
            Map.entry("农户直供", "来自乡村的在地好物"),
            Map.entry("手工精选", "认真呈现的乡村好物"),
            Map.entry("手工分拣", "认真呈现的乡村好物"),
            Map.entry("原汁原味", "乡村风味"),
            Map.entry("保留原味", "呈现质朴外观"),
            Map.entry("高山种植", "乡村产地特色"),
            Map.entry("富含营养", "适合日常饮食搭配"));
    private static final List<String> VAGUE_VISUAL_POINTS = List.of(
            "新鲜", "天然", "健康", "营养", "美味", "优质", "绿色", "生态", "无添加", "原味");
    private static final List<String> UNVERIFIED_CANDIDATE_TERMS = List.of(
            "口感", "香甜", "清脆", "鲜嫩", "多汁", "饱满", "营养", "无添加", "有机", "农药",
            "农残", "采摘", "现采", "现摘", "晾晒", "风干", "手工", "精选", "直发", "直送", "直供",
            "冷链", "地理标志", "高山种植", "原味", "保鲜", "味觉", "醇香", "酥脆", "脆香", "清香",
            "浓香", "鲜甜", "酸甜", "无污染");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final String apiKey;
    private final String textModel;
    private final String visionModel;

    public MarketingCopyService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            AuditService auditService,
            @Value("${app.ai.zhipu.api-key:}") String apiKey,
            @Value("${app.ai.zhipu.text-model:glm-4-flash-250414}") String textModel,
            @Value("${app.ai.zhipu.vision-model:glm-4v-flash}") String visionModel) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.auditService = auditService;
        this.apiKey = clean(apiKey);
        this.textModel = clean(textModel).isEmpty() ? "glm-4-flash-250414" : clean(textModel);
        this.visionModel = clean(visionModel).isEmpty() ? "glm-4v-flash" : clean(visionModel);
    }

    public AiStatus status() {
        return new AiStatus(!apiKey.isEmpty(), "zhipu", textModel, visionModel);
    }

    public MarketingCopyResult optimize(OptimizeCopyCommand command, UserPrincipal actor, String ipAddress) {
        MarketingCopyResult result = optimizeInternal(normalize(command), "text");
        auditService.record(actor.id(), "AI_MARKETING_COPY_OPTIMIZE", "MARKETING_COPY", result.requestId(), ipAddress);
        return result;
    }

    public MarketingCopyResult fromImage(ImageCopyCommand command, UserPrincipal actor, String ipAddress) {
        if (apiKey.isEmpty()) {
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "未配置 ZHIPU_API_KEY，暂时无法识别图片");
        }
        Matcher matcher = IMAGE_DATA_URL.matcher(command.imageDataUrl());
        if (!matcher.matches()) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "图片格式不正确，仅支持 JPG、PNG 和 WebP");
        }
        byte[] imageBytes;
        try {
            imageBytes = Base64.getMimeDecoder().decode(matcher.group(2).getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "图片数据无法解析");
        }
        if (imageBytes.length == 0 || imageBytes.length > MAX_IMAGE_BYTES) {
            throw new ApiException(ErrorCode.PAYLOAD_TOO_LARGE, "图片不能超过 6MB");
        }

        VisionFacts facts = analyzeImage(command);
        String suppliedName = clean(command.productName());
        String resolvedName = !suppliedName.isEmpty() ? suppliedName
                : !facts.productName().isEmpty() ? facts.productName() : "图中农产品";
        String resolvedCategory = !clean(command.category()).isEmpty()
                ? clean(command.category()) : defaultValue(facts.category(), "农产品");
        boolean nameCompatible = sameProductName(suppliedName, facts.productName())
                || sameProductName(clean(command.category()), facts.productName())
                || sameProductName(clean(command.category()), facts.category());
        boolean nameConflict = !suppliedName.isEmpty() && !facts.productName().isEmpty() && !nameCompatible;
        boolean generationBlocked = !facts.agriculturalProduct()
                || (nameConflict && "high".equals(facts.confidence()));
        String conflictMessage = !facts.agriculturalProduct()
                ? "当前首张封面未识别到明确农产品，请更换农产品主体清晰的封面"
                : generationBlocked
                        ? "当前填写的是“" + suppliedName + "”，但首张封面更像“" + facts.productName()
                                + "”，请调整封面或商品信息后重试"
                        : nameConflict
                                ? "首张封面识别结果与人工名称可能不一致，请人工确认"
                                : "";
        List<SellingPointCandidate> factCandidates = factCandidates(facts, safeList(command.confirmedSellingPoints()));
        VisualAnalysis analysis = new VisualAnalysis(
                facts.description(),
                facts.productName(),
                facts.category(),
                facts.sellingPoints(),
                facts.visibleText(),
                facts.confidence(),
                facts.candidates(),
                resolvedName,
                suppliedName.isEmpty() ? "vision" : "user",
                nameConflict,
                facts.agriculturalProduct(),
                facts.productStage(),
                facts.scene(),
                facts.visibleFeatures(),
                generationBlocked,
                conflictMessage);

        if (generationBlocked) {
            Map<String, Boolean> checks = new LinkedHashMap<>();
            checks.put("agricultural_product", facts.agriculturalProduct());
            checks.put("image_matches_product", !nameConflict);
            QualityReport report = new QualityReport(
                    0, 0, targetMinimum(command.maxLength()), command.maxLength(), Map.copyOf(checks),
                    List.of(conflictMessage));
            Map<String, Object> meta = Map.of(
                    "source", "image",
                    "channel", defaultValue(command.channel(), "ecommerce"),
                    "tone", defaultValue(command.tone(), "friendly"),
                    "provider", "zhipu",
                    "engine", "vision:" + visionModel + " + agricultural-conflict-gate-v2");
            MarketingCopyResult blocked = new MarketingCopyResult(
                    UUID.randomUUID().toString(), "", "", List.of(), factCandidates,
                    report, analysis, meta);
            auditService.record(actor.id(), "AI_MARKETING_COPY_FROM_IMAGE_BLOCKED", "MARKETING_COPY",
                    blocked.requestId(), ipAddress);
            return blocked;
        }

        List<String> confirmedFacts = new ArrayList<>();
        confirmedFacts.addAll(normalizeList(command.confirmedFacts(), 8));
        if (!clean(command.season()).isEmpty()) confirmedFacts.add(clean(command.season()) + "上市");
        if (!clean(command.originalCopy()).isEmpty()) confirmedFacts.add(clean(command.originalCopy()));
        confirmedFacts.addAll(normalizeList(command.confirmedSellingPoints(), 8));
        confirmedFacts = normalizeList(confirmedFacts, 12);

        AgriculturalGenerated generated = generateAgriculturalCopy(
                command, facts, resolvedName, resolvedCategory, confirmedFacts);
        String evidence = String.join("；", confirmedFacts) + "；" + String.join("；", facts.visibleFeatures());
        List<String> generatedPoints = normalizeList(generated.sellingPoints(), 8).stream()
                .map(point -> sanitizeUnsupportedClaims(point, evidence))
                .filter(point -> !point.isEmpty())
                .toList();
        if (generatedPoints.isEmpty()) {
            generatedPoints = facts.sellingPoints().isEmpty()
                    ? List.of("当季乡村好物", "适合家庭分享")
                    : facts.sellingPoints();
        }
        String headline = sanitizeUnsupportedClaims(
                sanitize(generated.headline(), command.prohibitedTerms()), evidence);
        if (headline.isEmpty()) headline = resolvedName + "｜" + generatedPoints.get(0);
        headline = truncate(headline, Math.min(command.maxLength(), 36));
        String copy = sanitizeUnsupportedClaims(
                sanitize(generated.copy(), command.prohibitedTerms()), evidence);
        if (!copy.contains(resolvedName)) copy = resolvedName + "｜" + copy;
        copy = completeCopy(copy, resolvedCategory, command.maxLength());

        OptimizeCopyCommand qualityCommand = new OptimizeCopyCommand(
                resolvedName, resolvedCategory, defaultValue(command.originalCopy(), facts.description()),
                generatedPoints, defaultValue(command.audience(), "关注产地与品质的顾客"),
                defaultValue(command.tone(), "friendly"), defaultValue(command.channel(), "ecommerce"),
                command.maxLength(), safeList(command.prohibitedTerms()));
        QualityReport baseReport = qualityReport(copy, qualityCommand, generatedPoints);
        List<String> warnings = new ArrayList<>(baseReport.warnings());
        if (nameConflict) warnings.add(conflictMessage);
        if (!"high".equals(facts.confidence())) warnings.add("图片识别置信度为" + facts.confidence() + "，请人工确认商品主体");
        QualityReport report = new QualityReport(
                baseReport.score(), baseReport.characterCount(), baseReport.minLength(), baseReport.maxLength(),
                baseReport.checks(), List.copyOf(warnings));
        List<SellingPointCandidate> candidates = mergeCandidates(
                factCandidates, generated.candidates(),
                fallbackMarketingCandidates(resolvedCategory, clean(command.season()), facts),
                fallbackConfirmationCandidates(resolvedCategory),
                safeList(command.confirmedSellingPoints()));
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("source", "image");
        meta.put("channel", defaultValue(command.channel(), "ecommerce"));
        meta.put("tone", defaultValue(command.tone(), "friendly"));
        meta.put("provider", "zhipu");
        meta.put("engine", "vision:" + visionModel + " -> text:" + textModel + " + agricultural-marketing-v2");
        MarketingCopyResult result = new MarketingCopyResult(
                UUID.randomUUID().toString(), headline, copy, generatedPoints, candidates,
                report, analysis, Map.copyOf(meta));
        auditService.record(actor.id(), "AI_MARKETING_COPY_FROM_IMAGE", "MARKETING_COPY", result.requestId(), ipAddress);
        return result;
    }

    private MarketingCopyResult optimizeInternal(OptimizeCopyCommand command, String source) {
        List<String> points = normalizeList(command.sellingPoints(), 8);
        if (points.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "至少填写一个核心卖点");
        }
        GeneratedText generated = apiKey.isEmpty()
                ? localCopy(command, points)
                : generateText(command, points);
        List<String> generatedPoints = normalizeList(generated.sellingPoints(), 8).stream()
                .filter(points::contains)
                .toList();
        if (generatedPoints.isEmpty()) generatedPoints = points;

        String headline = sanitize(generated.headline(), command.prohibitedTerms());
        if (headline.isEmpty()) headline = command.productName() + "｜" + generatedPoints.get(0);
        headline = truncate(headline, Math.min(command.maxLength(), 36));
        String copy = sanitize(generated.copy(), command.prohibitedTerms());
        if (!copy.contains(command.productName())) copy = command.productName() + "｜" + copy;
        copy = completeCopy(copy, command.category(), command.maxLength());

        QualityReport report = qualityReport(copy, command, generatedPoints);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("source", source);
        meta.put("channel", command.channel());
        meta.put("tone", command.tone());
        meta.put("provider", apiKey.isEmpty() ? "local" : "zhipu");
        meta.put("engine", generated.engine());
        return new MarketingCopyResult(
                UUID.randomUUID().toString(), headline, copy, generatedPoints, List.of(), report, null, Map.copyOf(meta));
    }

    private GeneratedText generateText(OptimizeCopyCommand command, List<String> points) {
        int minimum = targetMinimum(command.maxLength());
        String prompt = """
                请把下面的农产品介绍优化成有购买理由的中文电商文案。
                事实边界：保留用户提供的名称、产地、季节、工艺和规格，不新增销量、功效、认证、种植方式或口感事实。
                创意空间：可以主动制造时令感、场景感、家庭分享、送礼心意、乡村故事感和购买行动，但不要写成空泛说明书。
                写作结构：营销钩子开场；紧接真实产品信息；提炼2至4个购买理由；用自然行动句收尾。
                禁止使用“更多细节请参考商品页”“建议结合个人需求判断”等机械免责声明，避免重复表达。
                只输出 JSON，字段必须包含 headline、optimized_copy、selling_points。
                optimized_copy 必须包含商品名和至少一个已提供卖点，长度在 %d 到 %d 个中文字符之间。
                商品名：%s
类目：%s
目标人群：%s
语气：%s
渠道：%s
原始文案：%s
真实卖点：%s
限制词：%s
                """.formatted(
                minimum, command.maxLength(), command.productName(), defaultValue(command.category(), "未提供"),
                command.audience(), command.tone(), command.channel(), command.originalCopy(),
                String.join("、", points), String.join("、", safeList(command.prohibitedTerms())));
        Map<String, Object> body = Map.of(
                "model", textModel,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是懂农产品、电商转化和乡村表达的中文营销策划。约束事实，放开创意。"),
                        Map.of("role", "user", "content", prompt)),
                "temperature", 0.68,
                "max_tokens", 900,
                "response_format", Map.of("type", "json_object"),
                "stream", false);
        JsonNode generated = parseModelJson(callModel(body, "文案模型"));
        return new GeneratedText(
                clean(generated.path("headline").asText()),
                clean(generated.path("optimized_copy").asText()),
                stringArray(generated.path("selling_points"), 8),
                "text:" + textModel + " + agricultural-marketing-v2");
    }

    private VisionFacts analyzeImage(ImageCopyCommand command) {
        String prompt = """
                只根据这张图片独立识别，不参考任何用户填写的商品名称，也不要为了迎合用户猜测主体。
                任务是判断图片是否以农产品为主要主体，并提取农产品事实：中文通用商品名、类目、所处阶段、场景、
                外观和状态等可见特征、可见文字，以及可从画面直接提炼的卖点。
                不猜测口感、功效、品牌、产地、规格、销量、认证、农药情况、采摘时间或加工承诺。
                如果主体是建筑、风景、人物或无法判断，is_agricultural_product 必须为 false。
                只输出 JSON，字段必须包含：
                is_agricultural_product、product_name、category、product_stage、scene、visual_description、
                visible_features、selling_points、visible_text、product_name_confidence、product_candidates。
                product_name_confidence 只能是 high、medium 或 low。
                """;
        Map<String, Object> body = Map.of(
                "model", visionModel,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是独立的农产品视觉质检员，只认图片证据，以严格 JSON 返回。"),
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "image_url", "image_url", Map.of("url", command.imageDataUrl())),
                                Map.of("type", "text", "text", prompt)))),
                "temperature", 0.1,
                "max_tokens", 900,
                "stream", false);
        JsonNode result = parseModelJson(callModel(body, "视觉模型"));
        String confidence = clean(result.path("product_name_confidence").asText()).toLowerCase();
        if (!List.of("high", "medium", "low").contains(confidence)) confidence = "low";
        List<String> candidates = stringArray(result.path("product_candidates"), 3);
        String productName = clean(result.path("product_name").asText());
        if (!productName.isEmpty() && !candidates.contains(productName)) {
            candidates = new ArrayList<>(candidates);
            candidates.add(0, productName);
            candidates = candidates.stream().limit(3).toList();
        }
        List<String> visibleFeatures = stringArray(result.path("visible_features"), 8);
        List<String> visualPoints = stringArray(result.path("selling_points"), 6).stream()
                .filter(MarketingCopyService::isConcreteVisualPoint)
                .toList();
        if (visualPoints.isEmpty()) visualPoints = visibleFeatures.stream().limit(5).toList();
        return new VisionFacts(
                productName,
                clean(result.path("category").asText()),
                clean(result.path("visual_description").asText()),
                visualPoints,
                stringArray(result.path("visible_text"), 8),
                confidence,
                candidates,
                result.path("is_agricultural_product").asBoolean(!productName.isEmpty()),
                clean(result.path("product_stage").asText()),
                clean(result.path("scene").asText()),
                visibleFeatures);
    }

    private AgriculturalGenerated generateAgriculturalCopy(
            ImageCopyCommand command,
            VisionFacts facts,
            String productName,
            String category,
            List<String> confirmedFacts) {
        int minimum = targetMinimum(command.maxLength());
        String prompt = """
                请基于农产品事实生成有购买冲动的中文电商文案。核心原则是“约束事实，放开创意”。

                事实规则：
                1. 人工确认事实可以直接写；首图事实只能写图片真正可见的内容。
                2. 可以主动制造时令感、稀缺感、家庭分享、送礼心意、乡村故事感和购买行动。
                3. 未被人工确认时，正文不得写香甜多汁、皮薄肉厚、当天采摘、现摘现发、有机、无农药、
                   零农残、不打蜡、零添加、古法、高山种植、营养功效等具体强事实。
                4. 同时提出2至4个更大胆的强卖点候选，放在 confirmation_required_points，供用户确认后再次生成。

                写作规则：
                - 根据类目采用对应农产品逻辑：%s
                - 用一句有画面感的营销钩子开场，接真实产品信息，再给购买场景和行动理由。
                - 不写“更多细节请参考商品页”“建议结合个人需求判断”等机械免责声明。
                - 不重复句意，不堆砌形容词，不把图片描述生硬写成检测报告。
                - optimized_copy 长度控制在 %d 至 %d 个中文字符。

                商品名：%s
                类目：%s
                上市季节：%s
                人工确认事实：%s
                人工确认的强卖点：%s
                首图识别商品：%s
                首图阶段：%s
                首图场景：%s
                首图可见特征：%s
                首图客观描述：%s
                目标人群：%s
                语气：%s
                渠道：%s
                用户限制词：%s

                只输出 JSON，必须包含：
                - headline：营销标题
                - optimized_copy：完整营销文案
                - selling_points：正文实际采用的卖点字符串数组
                - marketing_selling_points：安全创意卖点数组，每项包含 text、dimension、basis
                - confirmation_required_points：待确认强卖点数组，每项包含 text、dimension、basis
                """.formatted(
                categoryStrategy(category), minimum, command.maxLength(), productName, category,
                defaultValue(command.season(), "未提供"),
                confirmedFacts.isEmpty() ? "未提供" : String.join("；", confirmedFacts),
                safeList(command.confirmedSellingPoints()).isEmpty()
                        ? "未提供" : String.join("；", command.confirmedSellingPoints()),
                defaultValue(facts.productName(), "未明确识别"),
                defaultValue(facts.productStage(), "未识别"),
                defaultValue(facts.scene(), "未识别"),
                facts.visibleFeatures().isEmpty() ? "未识别" : String.join("、", facts.visibleFeatures()),
                defaultValue(facts.description(), "无"),
                defaultValue(command.audience(), "关注产地与品质的顾客"),
                defaultValue(command.tone(), "friendly"),
                defaultValue(command.channel(), "ecommerce"),
                safeList(command.prohibitedTerms()).isEmpty()
                        ? "无" : String.join("、", command.prohibitedTerms()));
        Map<String, Object> body = Map.of(
                "model", textModel,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "你是懂农产品消费心理、电商转化和乡村叙事的中文营销策划。营销要有力度，但不能把创意伪装成事实。"),
                        Map.of("role", "user", "content", prompt)),
                "temperature", 0.78,
                "max_tokens", 1400,
                "response_format", Map.of("type", "json_object"),
                "stream", false);
        JsonNode generated = parseModelJson(callModel(body, "农产品文案模型"));
        List<SellingPointCandidate> candidates = new ArrayList<>();
        candidates.addAll(candidateArray(generated.path("marketing_selling_points"), "marketing", false));
        candidates.addAll(candidateArray(generated.path("confirmation_required_points"), "confirmation_required", true));
        return new AgriculturalGenerated(
                clean(generated.path("headline").asText()),
                clean(generated.path("optimized_copy").asText()),
                stringArray(generated.path("selling_points"), 8),
                List.copyOf(candidates));
    }

    private List<SellingPointCandidate> candidateArray(JsonNode node, String defaultKind, boolean defaultConfirmation) {
        if (!node.isArray()) return List.of();
        List<SellingPointCandidate> candidates = new ArrayList<>();
        node.forEach(item -> {
            String text = clean(item.isTextual() ? item.asText() : item.path("text").asText());
            if (text.isEmpty() || candidates.stream().anyMatch(candidate -> candidate.text().equals(text))) return;
            String dimension = clean(item.path("dimension").asText());
            String basis = clean(item.path("basis").asText());
            if (containsAny(basis, VAGUE_VISUAL_POINTS.toArray(String[]::new))) {
                basis = "基于农产品消费场景提炼";
            }
            boolean needsConfirmation = defaultConfirmation || containsUnsupportedClaim(text)
                    || containsAny(text + "；" + basis, UNVERIFIED_CANDIDATE_TERMS.toArray(String[]::new));
            String kind = needsConfirmation ? "confirmation_required" : defaultKind;
            candidates.add(new SellingPointCandidate(
                    text,
                    kind,
                    defaultValue(dimension, needsConfirmation ? "强卖点" : "营销创意"),
                    defaultValue(basis, needsConfirmation ? "图片无法直接证明，需人工确认" : "基于农产品消费场景提炼"),
                    needsConfirmation));
        });
        return candidates.stream().limit(8).toList();
    }

    private List<SellingPointCandidate> factCandidates(VisionFacts facts, List<String> confirmedSellingPoints) {
        List<SellingPointCandidate> candidates = new ArrayList<>();
        facts.sellingPoints().forEach(point -> candidates.add(new SellingPointCandidate(
                point, "fact", "首图事实", "当前首张封面可见", false)));
        normalizeList(confirmedSellingPoints, 8).forEach(point -> candidates.add(new SellingPointCandidate(
                point, "fact", "人工确认", "用户已明确确认", false)));
        return deduplicateCandidates(candidates, 10);
    }

    private List<SellingPointCandidate> fallbackMarketingCandidates(
            String category, String season, VisionFacts facts) {
        List<SellingPointCandidate> candidates = new ArrayList<>();
        if (!season.isEmpty()) {
            candidates.add(new SellingPointCandidate(
                    "一年一季的" + season + "尝鲜", "marketing", "时令", "根据人工填写的上市季节提炼", false));
        }
        if (!facts.productStage().isEmpty()) {
            candidates.add(new SellingPointCandidate(
                    facts.productStage() + "状态看得见", "marketing", "过程", "根据首张封面中的产品阶段提炼", false));
        }
        String normalized = clean(category);
        if (containsAny(normalized, "水果", "鲜果")) {
            candidates.add(new SellingPointCandidate("把果园里的时令带回家", "marketing", "情绪", "水果消费场景创意", false));
            candidates.add(new SellingPointCandidate("适合日常鲜食与家庭分享", "marketing", "场景", "水果常见消费场景", false));
        } else if (containsAny(normalized, "坚果", "干果")) {
            candidates.add(new SellingPointCandidate("山野风味的日常分享", "marketing", "情绪", "坚果消费场景创意", false));
            candidates.add(new SellingPointCandidate("适合家庭囤货与节日备礼", "marketing", "场景", "坚果常见消费场景", false));
        } else if (containsAny(normalized, "蔬菜", "菌菇")) {
            candidates.add(new SellingPointCandidate("把当季菜园端上餐桌", "marketing", "情绪", "蔬菜消费场景创意", false));
            candidates.add(new SellingPointCandidate("适合家常烹饪与日常搭配", "marketing", "场景", "蔬菜常见消费场景", false));
        } else if (containsAny(normalized, "粮食", "杂粮", "米面")) {
            candidates.add(new SellingPointCandidate("一日三餐里的乡村本味", "marketing", "情绪", "粮食消费场景创意", false));
            candidates.add(new SellingPointCandidate("适合家庭常备与日常烹饪", "marketing", "场景", "粮食常见消费场景", false));
        } else if (containsAny(normalized, "调味", "香料")) {
            candidates.add(new SellingPointCandidate("为家常菜添一味山野香气", "marketing", "场景", "调味品消费场景创意", false));
        } else {
            candidates.add(new SellingPointCandidate("从乡村走向餐桌的时令好物", "marketing", "情绪", "农产品通用消费场景", false));
            candidates.add(new SellingPointCandidate("适合家庭分享的乡村心意", "marketing", "场景", "农产品通用消费场景", false));
        }
        return List.copyOf(candidates);
    }

    private List<SellingPointCandidate> fallbackConfirmationCandidates(String category) {
        List<String> points;
        String normalized = clean(category);
        if (containsAny(normalized, "水果", "鲜果")) {
            points = List.of("香甜多汁", "皮薄肉厚", "当天采摘");
        } else if (containsAny(normalized, "坚果", "干果")) {
            points = List.of("颗粒饱满", "当季新收", "自然晾晒");
        } else if (containsAny(normalized, "蔬菜", "菌菇")) {
            points = List.of("清脆鲜嫩", "当日采收", "生态种植");
        } else if (containsAny(normalized, "加工", "酱", "干货")) {
            points = List.of("零添加", "手工制作", "古法工艺");
        } else if (containsAny(normalized, "调味", "香料")) {
            points = List.of("香气浓郁", "当季新晒", "手工精选");
        } else {
            points = List.of("当季现采", "农户直供", "手工分拣");
        }
        return points.stream()
                .map(point -> new SellingPointCandidate(
                        point, "confirmation_required", "强卖点", "图片无法直接证明，确认后可用于加强文案", true))
                .toList();
    }

    private List<SellingPointCandidate> mergeCandidates(
            List<SellingPointCandidate> facts,
            List<SellingPointCandidate> generated,
            List<SellingPointCandidate> marketingFallbacks,
            List<SellingPointCandidate> confirmationFallbacks,
            List<String> confirmedSellingPoints) {
        List<SellingPointCandidate> merged = new ArrayList<>();
        merged.addAll(facts);
        merged.addAll(generated);
        long marketingCount = merged.stream().filter(item -> "marketing".equals(item.kind())).count();
        if (marketingCount < 2) merged.addAll(marketingFallbacks);
        long confirmationCount = merged.stream().filter(SellingPointCandidate::needsConfirmation).count();
        if (confirmationCount < 2) merged.addAll(confirmationFallbacks);
        List<String> confirmed = normalizeList(confirmedSellingPoints, 8);
        List<SellingPointCandidate> normalized = merged.stream().map(candidate -> {
            if (confirmed.contains(candidate.text())) {
                return new SellingPointCandidate(
                        candidate.text(), "fact", candidate.dimension(), "用户已明确确认", false);
            }
            return candidate;
        }).toList();
        return deduplicateCandidates(normalized, 14);
    }

    private List<SellingPointCandidate> deduplicateCandidates(List<SellingPointCandidate> values, int limit) {
        Map<String, SellingPointCandidate> unique = new LinkedHashMap<>();
        for (SellingPointCandidate candidate : values) {
            String text = clean(candidate.text());
            if (text.isEmpty()) continue;
            unique.putIfAbsent(text, new SellingPointCandidate(
                    text, candidate.kind(), candidate.dimension(), candidate.basis(), candidate.needsConfirmation()));
            if (unique.size() >= limit) break;
        }
        return List.copyOf(unique.values());
    }

    private String categoryStrategy(String category) {
        String normalized = clean(category);
        if (containsAny(normalized, "水果", "鲜果")) return "突出时令、外观、鲜食分享和果园画面";
        if (containsAny(normalized, "蔬菜", "菌菇")) return "突出采收状态、家常烹饪和当季餐桌";
        if (containsAny(normalized, "坚果", "干果")) return "突出采收晾晒、家庭分享、囤货和备礼";
        if (containsAny(normalized, "粮食", "杂粮", "米面")) return "突出收获加工、颗粒状态和一日三餐";
        if (containsAny(normalized, "调味", "香料")) return "突出原料状态、晾晒加工和烹饪增香场景";
        if (containsAny(normalized, "加工", "酱", "干货")) return "突出原料、工艺、包装和家庭食用场景";
        return "突出时令、产地画面、家庭分享和乡村心意";
    }

    private String callModel(Map<String, Object> body, String providerName) {
        try {
            JsonNode response = restClient.post()
                    .uri(ZHIPU_ENDPOINT)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) throw new IllegalStateException("空响应");
            JsonNode content = response.path("choices").path(0).path("message").path("content");
            if (content.isTextual()) return content.asText();
            if (content.isArray()) {
                StringBuilder text = new StringBuilder();
                content.forEach(part -> text.append(part.path("text").asText("")));
                if (!text.isEmpty()) return text.toString();
            }
            throw new IllegalStateException("响应缺少 content");
        } catch (RestClientResponseException exception) {
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE,
                    providerName + "请求失败（HTTP " + exception.getStatusCode().value() + "）");
        } catch (ResourceAccessException exception) {
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, providerName + "连接超时，请稍后重试");
        } catch (RuntimeException exception) {
            if (exception instanceof ApiException apiException) throw apiException;
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, providerName + "返回结果无法解析");
        }
    }

    private JsonNode parseModelJson(String value) {
        int start = value.indexOf('{');
        int end = value.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型返回的结果不完整");
        }
        try {
            return objectMapper.readTree(value.substring(start, end + 1));
        } catch (JsonProcessingException exception) {
            throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE, "AI 模型返回的结果无法解析");
        }
    }

    private GeneratedText localCopy(OptimizeCopyCommand command, List<String> points) {
        String opener = switch (command.tone()) {
            case "professional" -> "把真实产地信息说清楚，也把选择理由放在前面";
            case "energetic" -> "把当季乡村风味带回家";
            case "premium" -> "一份带着乡村气息的认真心意";
            default -> "从乡村走向餐桌的当季好物";
        };
        String headline = command.productName() + "｜" + points.get(0);
        String copy = opener + "。" + command.productName() + "，" + command.originalCopy()
                + "。" + String.join("、", points.stream().limit(3).toList())
                + "，让它既适合日常品尝，也适合与家人分享。";
        return new GeneratedText(headline, copy, points, "rules-v2 + agricultural-marketing-v2");
    }

    private QualityReport qualityReport(String copy, OptimizeCopyCommand command, List<String> points) {
        int minimum = targetMinimum(command.maxLength());
        List<String> forbidden = new ArrayList<>(RISKY_REPLACEMENTS.keySet());
        forbidden.addAll(safeList(command.prohibitedTerms()));
        List<String> hits = forbidden.stream().filter(term -> !term.isBlank() && copy.contains(term)).distinct().toList();
        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("within_target_length", copy.length() >= minimum && copy.length() <= command.maxLength());
        checks.put("contains_product_name", copy.contains(command.productName()));
        checks.put("contains_selling_point", points.stream().anyMatch(copy::contains));
        checks.put("no_prohibited_terms", hits.isEmpty());
        int score = 100 - (int) checks.values().stream().filter(value -> !value).count() * 25;
        List<String> warnings = new ArrayList<>();
        if (!hits.isEmpty()) warnings.add("仍包含限制词：" + String.join("、", hits));
        if (!checks.get("contains_selling_point")) warnings.add("文案未完整保留可验证卖点");
        if (copy.length() < minimum) warnings.add("当前文案低于目标字数下限 " + minimum + " 字");
        return new QualityReport(score, copy.length(), minimum, command.maxLength(), Map.copyOf(checks), List.copyOf(warnings));
    }

    private String completeCopy(String value, String category, int maxLength) {
        String copy = clean(value);
        copy = copy.replaceAll("更多细节[^。]*。?", "")
                .replaceAll("建议结合[^。]*。?", "")
                .replaceAll("面向[^。]{0,60}建议[^。]*。?", "")
                .replaceAll("以商品页[^。]*为准。?", "");
        copy = clean(copy).replace("。。", "。");
        if (!copy.endsWith("。")) copy += "。";
        for (String extension : categoryExtensions(category)) {
            if (copy.length() >= targetMinimum(maxLength)) break;
            if (!copy.contains(extension) && copy.length() + extension.length() <= maxLength) {
                copy += extension;
            }
        }
        return truncate(copy, maxLength);
    }

    private List<String> categoryExtensions(String category) {
        String normalized = clean(category);
        if (containsAny(normalized, "水果", "鲜果")) {
            return List.of(
                    "趁着当季，把这份果园鲜意带回家。",
                    "无论独享、家庭分享还是走亲访友，都让当季选择更有理由。",
                    "让今天的果篮，多一份来自乡村的季节心意。");
        }
        if (containsAny(normalized, "蔬菜", "菌菇")) {
            return List.of(
                    "把当季菜园的新鲜感端上家常餐桌。",
                    "从一顿家常饭开始，让时令蔬菜成为餐桌上的自然主角。",
                    "一份好食材，也是一桌家常烟火的踏实开场。");
        }
        if (containsAny(normalized, "坚果", "干果")) {
            return List.of(
                    "日常分享或节日备礼，都多一份山野心意。",
                    "办公室随手分享、家中日常常备，都能把秋日收获变成一份具体心意。",
                    "选一份乡村风味，也为相聚时刻多备一份分享心意。");
        }
        if (containsAny(normalized, "粮食", "杂粮", "米面")) {
            return List.of(
                    "让乡村本味自然融入一日三餐。",
                    "从早餐到家常饭，用一份踏实主食连接每天的烟火日常。",
                    "家中常备一份，也为每天的餐桌多一种从容选择。");
        }
        if (containsAny(normalized, "调味", "香料")) {
            return List.of(
                    "为家常烹饪添上一味乡村香气。",
                    "煎炒炖煮都有合适用法，让一日三餐多一层风味表达。",
                    "从一味调料开始，为熟悉的家常菜换一种表达。");
        }
        return List.of(
                "把这份来自乡村的时令心意带回家。",
                "无论日常自用还是与亲友分享，都让购买理由更清晰。",
                "让一份在地好物，连接乡村收获与真实生活场景。");
    }

    private OptimizeCopyCommand normalize(OptimizeCopyCommand command) {
        return new OptimizeCopyCommand(
                clean(command.productName()),
                clean(command.category()),
                clean(command.originalCopy()),
                normalizeList(command.sellingPoints(), 8),
                defaultValue(command.audience(), "关注产地与品质的顾客"),
                defaultValue(command.tone(), "friendly"),
                defaultValue(command.channel(), "ecommerce"),
                command.maxLength(),
                normalizeList(command.prohibitedTerms(), 20));
    }

    private String sanitize(String value, List<String> prohibitedTerms) {
        String result = clean(value);
        for (Map.Entry<String, String> entry : RISKY_REPLACEMENTS.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        for (String prohibited : safeList(prohibitedTerms)) result = result.replace(prohibited, "");
        return clean(result).replace("。。", "。");
    }

    private String sanitizeUnsupportedClaims(String value, String evidence) {
        String result = clean(value);
        String confirmedEvidence = clean(evidence);
        for (Map.Entry<String, String> entry : UNVERIFIED_CLAIM_REPLACEMENTS.entrySet()) {
            if (!confirmedEvidence.contains(entry.getKey())) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
        }
        result = result.replaceAll("干燥后的状态(?:得)?恰到好处", "干燥后的状态清晰可见");
        result = result.replace("质朴乡村风味", "质朴外观");
        return clean(result).replace("。。", "。");
    }

    private boolean containsUnsupportedClaim(String value) {
        return UNVERIFIED_CLAIM_REPLACEMENTS.keySet().stream().anyMatch(value::contains);
    }

    private List<String> stringArray(JsonNode node, int limit) {
        if (!node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            String value = clean(item.asText());
            if (!value.isEmpty() && !values.contains(value) && values.size() < limit) values.add(value);
        });
        return List.copyOf(values);
    }

    private static List<String> normalizeList(List<String> values, int limit) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : safeList(values)) {
            String cleaned = clean(value);
            if (!cleaned.isEmpty()) unique.add(cleaned);
            if (unique.size() >= limit) break;
        }
        return List.copyOf(unique);
    }

    private static List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static int targetMinimum(int maxLength) {
        return Math.max(40, (int) Math.ceil(maxLength * 0.5));
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) return true;
        }
        return false;
    }

    private static boolean isConcreteVisualPoint(String value) {
        String cleaned = clean(value);
        return !cleaned.isEmpty()
                && VAGUE_VISUAL_POINTS.stream().noneMatch(cleaned::contains);
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, Math.max(1, maxLength - 1)).stripTrailing() + "…";
    }

    private static String defaultValue(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static boolean sameProductName(String left, String right) {
        String normalizedLeft = left.toLowerCase().replaceAll("[\\p{P}\\p{Z}\\s]+", "");
        String normalizedRight = right.toLowerCase().replaceAll("[\\p{P}\\p{Z}\\s]+", "");
        return !normalizedLeft.isEmpty() && !normalizedRight.isEmpty()
                && (normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft));
    }

    private record GeneratedText(String headline, String copy, List<String> sellingPoints, String engine) {
    }

    private record VisionFacts(
            String productName,
            String category,
            String description,
            List<String> sellingPoints,
            List<String> visibleText,
            String confidence,
            List<String> candidates,
            boolean agriculturalProduct,
            String productStage,
            String scene,
            List<String> visibleFeatures) {
    }

    private record AgriculturalGenerated(
            String headline,
            String copy,
            List<String> sellingPoints,
            List<SellingPointCandidate> candidates) {
    }
}
