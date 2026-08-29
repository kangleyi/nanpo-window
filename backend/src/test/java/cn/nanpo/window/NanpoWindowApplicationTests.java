package cn.nanpo.window;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.security.MessageDigest;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NanpoWindowApplicationTests {

    private final Map<String, JsonNode> loginTokens = new HashMap<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    void exposesBackendHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @Order(2)
    void servesCompiledFrontendForClientRoutes() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    @Order(3)
    void exposesPublishedCatalogFromFlywayData() throws Exception {
        mockMvc.perform(get("/api/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("大南坡村"))
                .andExpect(jsonPath("$.data.visitorService.scene").value("VISITOR_SERVICE"));

        mockMvc.perform(get("/api/public/products").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].name").value("太行山核桃"))
                .andExpect(jsonPath("$.data.items[0].startingPrice").value(29.9));

        mockMvc.perform(get("/api/public/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.farmer.certificationStatus").value("APPROVED"))
                .andExpect(jsonPath("$.data.productionRecords.length()").value(2));
    }

    @Test
    @Order(4)
    void validatesPagingAndHidesUnpublishedResources() throws Exception {
        mockMvc.perform(get("/api/public/homestays").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mockMvc.perform(get("/api/public/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @Order(5)
    void protectsFarmerAndAdminApiNamespaces() throws Exception {
        mockMvc.perform(get("/api/farmer/plots"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    @Order(6)
    void logsInWithSmsAndResolvesRolesFromDatabase() throws Exception {
        String phone = "13800000001";
        JsonNode tokens = login(phone);
        String accessToken = tokens.path("accessToken").asText();
        String refreshToken = tokens.path("refreshToken").asText();

        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value(phone))
                .andExpect(jsonPath("$.data.roles[0]").value("FARMER"));

        String refreshedBody = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        loginTokens.put(phone, objectMapper.readTree(refreshedBody).path("data"));
    }

    @Test
    @Order(7)
    void adminCanPublishAndUnpublishContentWithoutRedeploying() throws Exception {
        String accessToken = login("13800000002").path("accessToken").asText();
        String command = """
                {
                  "name": "测试山居",
                  "type": "家庭小院",
                  "summary": "由集成测试创建的待发布民宿。",
                  "capacity": "2—3 人",
                  "price": "价格待确认",
                  "coverUrl": "/images/homestay.jpg",
                  "consultationPhone": "0391-0000000",
                  "sortOrder": 99
                }
                """;

        String createdBody = mockMvc.perform(post("/api/admin/content/homestays")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createdBody).path("data").path("id").asLong();

        mockMvc.perform(get("/api/public/homestays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));

        mockMvc.perform(post("/api/admin/content/homestays/{id}/publish", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/public/homestays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.items[3].name").value("测试山居"));

        String updated = command.replace("测试山居", "测试山居·已更新");
        mockMvc.perform(put("/api/admin/content/homestays/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试山居·已更新"));

        mockMvc.perform(post("/api/admin/content/homestays/{id}/unpublish", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(get("/api/public/homestays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    @Order(8)
    void farmerRecordRequiresOwnershipSubmissionAndReviewerApproval() throws Exception {
        String farmerToken = login("13800000001").path("accessToken").asText();
        String adminToken = login("13800000002").path("accessToken").asText();
        String command = """
                {
                  "productId": 1,
                  "plotId": 1,
                  "stage": "PACKING",
                  "occurredAt": "2026-08-28T09:00:00",
                  "originalText": "今天完成了新一批核桃分选和装袋。",
                  "truthConfirmed": true
                }
                """;

        String createdBody = mockMvc.perform(post("/api/farmer/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createdBody).path("data").path("id").asLong();

        mockMvc.perform(get("/api/public/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productionRecords.length()").value(2));

        mockMvc.perform(post("/api/farmer/records/{id}/submit", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        mockMvc.perform(get("/api/admin/reviews/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(id));

        mockMvc.perform(post("/api/admin/reviews/records/{id}/approve", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedText\":\"8 月 28 日完成核桃分选和装袋。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/public/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productionRecords.length()").value(3))
                .andExpect(jsonPath("$.data.productionRecords[2].text").value("8 月 28 日完成核桃分选和装袋。"));

        mockMvc.perform(post("/api/admin/reviews/records/{id}/approve", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(9)
    void orderWorkflowIsIdempotentAndEnforcesStateTransitions() throws Exception {
        String customerToken = login("13900000003").path("accessToken").asText();
        String farmerToken = login("13800000001").path("accessToken").asText();
        String adminToken = login("13800000002").path("accessToken").asText();
        String idempotencyKey = "test-order-00000001";
        String command = """
                {
                  "recipientName": "张小宁",
                  "recipientPhone": "13900000003",
                  "recipientAddress": "河南省郑州市金水区测试路 18 号",
                  "customerNote": "周末收货",
                  "items": [{"skuId": 1, "quantity": 1}]
                }
                """;

        String createdBody = mockMvc.perform(post("/api/customer/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.totalAmount").value(29.9))
                .andExpect(jsonPath("$.data.payment.demo").value(true))
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(createdBody).path("data");
        long orderId = created.path("id").asLong();
        String orderNo = created.path("orderNo").asText();

        mockMvc.perform(post("/api/customer/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId));

        mockMvc.perform(post("/api/customer/orders/{orderNo}/payment-report", orderNo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"0003 核桃\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAYMENT_REPORTED"));

        mockMvc.perform(post("/api/admin/orders/{id}/confirm-payment", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(get("/api/farmer/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(orderId))
                .andExpect(jsonPath("$.data[0].items[0].productName").value("太行山核桃"));

        mockMvc.perform(post("/api/farmer/orders/{id}/prepare", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_TO_SHIP"));

        mockMvc.perform(post("/api/admin/orders/{id}/ship", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingCompany\":\"邮政快递\",\"trackingNo\":\"TEST123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(post("/api/customer/orders/{orderNo}/complete", orderNo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(post("/api/farmer/orders/{id}/prepare", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(10)
    void mediaUploadValidatesOwnershipSignatureSizeAndChecksum() throws Exception {
        String farmerToken = login("13800000001").path("accessToken").asText();
        String customerToken = login("13900000003").path("accessToken").asText();
        String reviewerToken = login("13800000002").path("accessToken").asText();
        byte[] pngHeader = new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10};
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pngHeader));
        String ticketBody = mockMvc.perform(post("/api/media/upload-ticket")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mediaType":"IMAGE",
                                  "contentType":"image/png",
                                  "sizeBytes":8,
                                  "originalName":"harvest.png",
                                  "checksumSha256":"%s",
                                  "recordId":1
                                }
                                """.formatted(checksum)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.media.status").value("CREATED"))
                .andExpect(jsonPath("$.data.uploadUrl").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long mediaId = objectMapper.readTree(ticketBody).path("data").path("media").path("id").asLong();

        mockMvc.perform(put("/api/media/{id}/content", mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(pngHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UPLOADED"));

        mockMvc.perform(post("/api/media/{id}/complete", mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.checksumSha256").value(checksum));

        mockMvc.perform(get("/api/media/{id}/status", mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/media/{id}/content", mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + reviewerToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));
    }

    @Test
    @Order(11)
    void aiCopyUsesOnlyReviewedSourcesAndRequiresOwnerConfirmation() throws Exception {
        String farmerToken = login("13800000001").path("accessToken").asText();
        String body = mockMvc.perform(post("/api/farmer/products/1/ai-copy")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scene\":\"PRODUCT_INTRO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.sourceRecordIds.length()").value(3))
                .andExpect(jsonPath("$.data.modelName").value("local-factual-template"))
                .andReturn().getResponse().getContentAsString();
        long generationId = objectMapper.readTree(body).path("data").path("id").asLong();

        mockMvc.perform(post("/api/farmer/ai-copy/{id}/confirm", generationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedText\":\"这段文案由农户确认，只包含已审核事实。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.confirmedText").value("这段文案由农户确认，只包含已审核事实。"));

        mockMvc.perform(post("/api/farmer/ai-copy/{id}/confirm", generationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedText\":\"不能重复确认。\"}"))
                .andExpect(status().isConflict());
    }

    private JsonNode login(String phone) throws Exception {
        JsonNode existing = loginTokens.get(phone);
        if (existing != null) {
            return existing;
        }
        mockMvc.perform(post("/api/auth/sms/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\"}"))
                .andExpect(status().isOk());
        String response = mockMvc.perform(post("/api/auth/sms/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode tokens = objectMapper.readTree(response).path("data");
        loginTokens.put(phone, tokens);
        return tokens;
    }
}
