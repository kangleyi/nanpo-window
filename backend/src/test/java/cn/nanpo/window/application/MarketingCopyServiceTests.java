package cn.nanpo.window.application;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.nanpo.window.api.admin.MarketingCopyViews.ImageCopyCommand;
import cn.nanpo.window.api.admin.MarketingCopyViews.MarketingCopyResult;
import cn.nanpo.window.security.UserPrincipal;

class MarketingCopyServiceTests {

    private static final String ENDPOINT = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final UserPrincipal ADMIN = new UserPrincipal(2, "13800000002", "管理员", Set.of("ADMIN"));

    @Test
    void independentlyRecognizesImageAndSeparatesUnverifiedStrongSellingPoints() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuditService auditService = mock(AuditService.class);
        MarketingCopyService service = new MarketingCopyService(
                builder, new ObjectMapper(), auditService, "test-key", "text-test", "vision-test");

        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(content().string(not(containsString("人工填写的桃子"))))
                .andRespond(withSuccess(modelResponse("""
                        {
                          "is_agricultural_product": true,
                          "product_name": "桃子",
                          "category": "水果",
                          "product_stage": "枝头生长",
                          "scene": "果园",
                          "visual_description": "多颗粉红色桃子生长在枝叶间",
                          "visible_features": ["果面粉红", "枝头生长"],
                          "selling_points": ["枝头状态可见", "自然果园画面"],
                          "visible_text": [],
                          "product_name_confidence": "high",
                          "product_candidates": ["桃子"]
                        }
                        """), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(ENDPOINT))
                .andExpect(content().string(containsString("人工填写的桃子")))
                .andRespond(withSuccess(modelResponse("""
                        {
                          "headline": "把夏天的果园带回家",
                          "optimized_copy": "人工填写的桃子，把夏天的果园带回家。果面粉红，枝头生长状态看得见，香甜多汁，经过阳光晾晒得恰到好处，适合家庭鲜食与分享。",
                          "selling_points": ["枝头生长状态看得见", "适合家庭分享"],
                          "marketing_selling_points": [
                            {"text":"把夏天的果园带回家","dimension":"情绪","basis":"天然健康"},
                            {"text":"产地直送的安心","dimension":"信任","basis":"传统晾晒工艺"},
                            {"text":"质朴醇香","dimension":"感官体验","basis":"消费者味觉期待"}
                          ],
                          "confirmation_required_points": [
                            {"text":"香甜多汁","dimension":"口感","basis":"图片无法证明"}
                          ]
                        }
                        """), MediaType.APPLICATION_JSON));

        MarketingCopyResult result = service.fromImage(command("人工填写的桃子"), ADMIN, "127.0.0.1");

        assertFalse(result.visualAnalysis().generationBlocked());
        assertTrue(result.optimizedCopy().contains("人工填写的桃子"));
        assertFalse(result.optimizedCopy().contains("香甜多汁"));
        assertFalse(result.optimizedCopy().contains("阳光晾晒"));
        assertTrue(result.sellingPointCandidates().stream()
                .anyMatch(candidate -> candidate.text().equals("把夏天的果园带回家")
                        && !candidate.needsConfirmation()
                        && candidate.basis().equals("基于农产品消费场景提炼")));
        assertTrue(result.sellingPointCandidates().stream()
                .anyMatch(candidate -> candidate.text().equals("香甜多汁")
                        && candidate.needsConfirmation()));
        assertTrue(result.sellingPointCandidates().stream()
                .anyMatch(candidate -> candidate.text().equals("产地直送的安心")
                        && candidate.needsConfirmation()));
        assertTrue(result.sellingPointCandidates().stream()
                .anyMatch(candidate -> candidate.text().equals("质朴醇香")
                        && candidate.needsConfirmation()));
        assertTrue(result.qualityReport().checks().get("within_target_length"));
        server.verify();
    }

    @Test
    void blocksCopyGenerationWhenCoverIsNotAnAgriculturalProduct() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MarketingCopyService service = new MarketingCopyService(
                builder, new ObjectMapper(), mock(AuditService.class), "test-key", "text-test", "vision-test");

        server.expect(once(), requestTo(ENDPOINT))
                .andRespond(withSuccess(modelResponse("""
                        {
                          "is_agricultural_product": false,
                          "product_name": "",
                          "category": "建筑",
                          "product_stage": "",
                          "scene": "村庄建筑",
                          "visual_description": "画面主体为建筑",
                          "visible_features": ["建筑外观"],
                          "selling_points": [],
                          "visible_text": [],
                          "product_name_confidence": "high",
                          "product_candidates": []
                        }
                        """), MediaType.APPLICATION_JSON));

        MarketingCopyResult result = service.fromImage(command("桃子"), ADMIN, "127.0.0.1");

        assertTrue(result.visualAnalysis().generationBlocked());
        assertEquals("", result.optimizedCopy());
        assertTrue(result.visualAnalysis().conflictMessage().contains("未识别到明确农产品"));
        server.verify();
    }

    @Test
    void acceptsDetectedParentCategoryForASpecificProductName() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MarketingCopyService service = new MarketingCopyService(
                builder, new ObjectMapper(), mock(AuditService.class), "test-key", "text-test", "vision-test");

        server.expect(once(), requestTo(ENDPOINT))
                .andRespond(withSuccess(modelResponse("""
                        {
                          "is_agricultural_product": true,
                          "product_name": "坚果",
                          "category": "干果",
                          "product_stage": "干燥",
                          "scene": "货架",
                          "visual_description": "木盒中摆放多种坚果",
                          "visible_features": ["木盒陈列"],
                          "selling_points": ["新鲜", "天然", "木盒陈列清晰"],
                          "visible_text": [],
                          "product_name_confidence": "high",
                          "product_candidates": ["坚果"]
                        }
                        """), MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(ENDPOINT))
                .andRespond(withSuccess(modelResponse("""
                        {
                          "headline": "秋日山野心意",
                          "optimized_copy": "太行山核桃，把秋日山野心意带回家。来自核桃坡地，适合日常分享与节日备礼。",
                          "selling_points": ["秋季新收", "适合家庭分享"],
                          "marketing_selling_points": [],
                          "confirmation_required_points": []
                        }
                        """), MediaType.APPLICATION_JSON));

        MarketingCopyResult result = service.fromImage(command("太行山核桃", "坚果"), ADMIN, "127.0.0.1");

        assertFalse(result.visualAnalysis().generationBlocked());
        assertFalse(result.visualAnalysis().nameConflict());
        assertFalse(result.visualAnalysis().detectedSellingPoints().contains("新鲜"));
        assertTrue(result.visualAnalysis().detectedSellingPoints().contains("木盒陈列清晰"));
        server.verify();
    }

    private ImageCopyCommand command(String productName) {
        return command(productName, "水果");
    }

    private ImageCopyCommand command(String productName, String category) {
        return new ImageCopyCommand(
                "data:image/png;base64,AQID",
                productName,
                category,
                "夏季",
                "这是人工填写的介绍",
                List.of("夏季上市"),
                List.of(),
                "关注产地与品质的顾客",
                "夏季上市",
                "friendly",
                "ecommerce",
                220,
                List.of());
    }

    private String modelResponse(String jsonContent) throws Exception {
        return new ObjectMapper().writeValueAsString(java.util.Map.of(
                "choices", List.of(java.util.Map.of(
                        "message", java.util.Map.of("content", jsonContent)))));
    }
}
