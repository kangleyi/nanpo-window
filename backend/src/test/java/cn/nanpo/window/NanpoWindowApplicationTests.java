package cn.nanpo.window;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;

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

@SpringBootTest(properties = "app.storage.type=local")
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

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/farmer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @Order(3)
    void exposesPublishedCatalogFromFlywayData() throws Exception {
        mockMvc.perform(get("/api/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("大南坡村"))
                .andExpect(jsonPath("$.data.recommendedSeason").value("全年"))
                .andExpect(jsonPath("$.data.visitorService.scene").value("VISITOR_SERVICE"))
                .andExpect(jsonPath("$.data.visitorService.phone").value("13782746885"));

        mockMvc.perform(get("/api/public/products").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.items[0].name").value("太行山核桃"))
                .andExpect(jsonPath("$.data.items[0].imageUrls[0]").value("/images/products.jpg"))
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
    void registersCustomersAndLogsInWithPasswords() throws Exception {
        String registeredPhone = "13900000009";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + registeredPhone + "\",\"password\":\"Customer@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.phone").value(registeredPhone))
                .andExpect(jsonPath("$.data.user.roles[0]").value("CUSTOMER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + registeredPhone + "\",\"password\":\"Customer@123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000001\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_CREDENTIALS_INVALID"));

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
                  "consultationPhone": "13782746885",
                  "externalUrl": "https://example.com/test-homestay",
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
                .andExpect(jsonPath("$.data.items[3].name").value("测试山居"))
                .andExpect(jsonPath("$.data.items[3].externalUrl").value("https://example.com/test-homestay"));

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
    void farmerCannotMaintainProductsAndAdminCanManageThemForFarmer() throws Exception {
        String farmerToken = login("13800000001").path("accessToken").asText();
        String adminToken = login("13800000002").path("accessToken").asText();
        String newFarmerPhone = "13800000099";
        String createdFarmerBody = mockMvc.perform(post("/api/admin/farmers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "name": "测试村民",
                                  "villageGroup": "大南坡村测试组",
                                  "introduction": "用于验证后台新增村民账号与信息。"
                                }
                                """.formatted(newFarmerPhone)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试村民"))
                .andExpect(jsonPath("$.data.certificationStatus").value("APPROVED"))
                .andReturn().getResponse().getContentAsString();
        JsonNode createdFarmer = objectMapper.readTree(createdFarmerBody).path("data");
        assertTrue(createdFarmer.path("code").asText().matches("NP-F-\\d{3,}"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + newFarmerPhone + "\",\"password\":\"12345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.roles[0]").value("FARMER"));

        mockMvc.perform(post("/api/admin/farmers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "name": "重复村民",
                                  "villageGroup": "测试组",
                                  "introduction": "重复手机号"
                                }
                                """.formatted(newFarmerPhone)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        String command = """
                {
                  "plotId": 1,
                  "name": "测试山小米",
                  "category": "杂粮",
                  "season": "秋季",
                  "summary": "由村庄运营人员代村民维护的农产品。",
                  "coverUrl": "/images/products.jpg",
                  "imageUrls": ["/images/products.jpg", "/images/walnut-yard.jpg"],
                  "skus": [{
                    "specification": "500g / 袋",
                    "unitPrice": 18.80,
                    "stockNote": "测试现货"
                  }]
                }
                """;

        mockMvc.perform(post("/api/farmer/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        String createdBody = mockMvc.perform(post("/api/admin/farmers/1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.coverUrl").value("/images/products.jpg"))
                .andExpect(jsonPath("$.data.imageUrls.length()").value(2))
                .andExpect(jsonPath("$.data.imageUrls[1]").value("/images/walnut-yard.jpg"))
                .andReturn().getResponse().getContentAsString();
        JsonNode createdProduct = objectMapper.readTree(createdBody).path("data");
        long productId = createdProduct.path("id").asLong();
        long skuId = createdProduct.path("skus").path(0).path("id").asLong();
        String generatedCode = createdProduct.path("skus").path(0).path("code").asText();
        assertTrue(generatedCode.matches("SKU-\\d{6}-\\d{2,4}"));

        String updatedCommand = command.replace("测试山小米", "测试山小米·精选")
                .replace("18.80", "19.80")
                .replace("\"skus\": [{", "\"skus\": [{\"id\":" + skuId + ",");
        mockMvc.perform(put("/api/admin/farmers/1/products/{productId}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedCommand))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("测试山小米·精选"))
                .andExpect(jsonPath("$.data.skus[0].code").value(generatedCode))
                .andExpect(jsonPath("$.data.skus[0].unitPrice").value(19.8));

        mockMvc.perform(post("/api/admin/farmers/1/products/{productId}/publish", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/admin/farmers/1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + productId + ")].status").value("PUBLISHED"));

        mockMvc.perform(get("/api/public/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product.imageUrls[0]").value("/images/products.jpg"))
                .andExpect(jsonPath("$.data.product.imageUrls[1]").value("/images/walnut-yard.jpg"));

        String recordBody = mockMvc.perform(post("/api/admin/farmers/1/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "plotId": 1,
                                  "stage": "HARVEST",
                                  "occurredAt": "2026-08-28T09:00:00",
                                  "originalText": "运营人员向村民核实后，记录小米采收过程。",
                                  "truthConfirmed": true
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        long recordId = objectMapper.readTree(recordBody).path("data").path("id").asLong();

        mockMvc.perform(post("/api/admin/farmers/1/records/{recordId}/submit", recordId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
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

        mockMvc.perform(get("/api/customer/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].orderNo").value(orderNo))
                .andExpect(jsonPath("$.data[0].items[0].productName").value("太行山核桃"));

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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(get("/api/admin/orders")
                        .queryParam("farmerId", "1")
                        .queryParam("status", "PAID")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(orderId));

        mockMvc.perform(post("/api/admin/orders/{id}/prepare", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
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

        mockMvc.perform(post("/api/admin/orders/{id}/prepare", orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict());

        String manualPaymentBody = mockMvc.perform(post("/api/customer/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .header("Idempotency-Key", "manual-payment-order-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andReturn().getResponse().getContentAsString();
        long manualPaymentOrderId = objectMapper.readTree(manualPaymentBody).path("data").path("id").asLong();

        mockMvc.perform(post("/api/admin/orders/{id}/confirm-payment", manualPaymentOrderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    @Order(10)
    void contentOperatorCanUploadPublicListingMedia() throws Exception {
        String accessToken = login("13800000002").path("accessToken").asText();
        byte[] image = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image));
        String ticketBody = mockMvc.perform(post("/api/media/upload-ticket")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mediaType":"IMAGE",
                                  "contentType":"image/png",
                                  "sizeBytes":%d,
                                  "originalName":"listing.png",
                                  "checksumSha256":"%s"
                                }
                                """.formatted(image.length, checksum)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode ticket = objectMapper.readTree(ticketBody).path("data");
        long mediaId = ticket.path("media").path("id").asLong();

        mockMvc.perform(put("/api/media/{id}/content", mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(image))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/media/{id}/complete", mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));
        mockMvc.perform(get("/api/public/media/{id}/content", mediaId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(content().bytes(image));
    }

    @Test
    @Order(11)
    void farmerCannotUploadProductionMedia() throws Exception {
        String farmerToken = login("13800000001").path("accessToken").asText();
        mockMvc.perform(post("/api/media/upload-ticket")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mediaType":"IMAGE",
                                  "contentType":"image/png",
                                  "sizeBytes":8,
                                  "originalName":"harvest.png",
                                  "checksumSha256":"4c4b6a3be1314ab86138bef4314dde022e600960d8689a2c8f8631802d20dab6",
                                  "recordId":1
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(12)
    void farmerCannotGenerateOrConfirmAiCopy() throws Exception {
        String farmerToken = login("13800000001").path("accessToken").asText();
        mockMvc.perform(post("/api/farmer/products/1/ai-copy")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scene\":\"PRODUCT_INTRO\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post("/api/farmer/ai-copy/{id}/confirm", 1)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedText\":\"不能由村民确认。\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @Order(13)
    void visitorCanLeaveInquiryAndAdminCanFollowItUp() throws Exception {
        String inquiry = """
                {
                  "sourceType": "HOMESTAY",
                  "sourceId": 1,
                  "visitAt": "2099-10-01T10:30:00",
                  "partySize": 4,
                  "callbackPhone": "13900001234",
                  "note": "有老人同行，希望安排一楼房间。"
                }
                """;
        String createdBody = mockMvc.perform(post("/api/public/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inquiry))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceType").value("HOMESTAY"))
                .andExpect(jsonPath("$.data.partySize").value(4))
                .andExpect(jsonPath("$.data.callbackPhone").value("13900001234"))
                .andExpect(jsonPath("$.data.status").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(createdBody).path("data").path("id").asLong();

        String accessToken = login("13800000002").path("accessToken").asText();
        mockMvc.perform(get("/api/admin/inquiries")
                        .param("status", "NEW")
                        .param("sourceType", "HOMESTAY")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].note").value("有老人同行，希望安排一楼房间。"));

        mockMvc.perform(post("/api/admin/inquiries/{id}/contacted", id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONTACTED"));
    }

    @Test
    @Order(14)
    void adminCanPublishGoodsSectionSettingsToThePublicSite() throws Exception {
        String accessToken = login("13800000002").path("accessToken").asText();
        String command = """
                {
                  "eyebrow": "南坡当季",
                  "title": "山野风物，按时抵达。",
                  "description": "由村庄运营人员维护的当季好物介绍。",
                  "seasonLabel": "九月",
                  "seasonNote": "核桃与小米进入收获期",
                  "imageUrl": "https://example.com/goods.jpg",
                  "imageCaption": "当季山野好物"
                }
                """;

        mockMvc.perform(put("/api/admin/content/site-sections/goods")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(command))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("山野风物，按时抵达。"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/goods.jpg"));

        mockMvc.perform(get("/api/public/site"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goodsSection.eyebrow").value("南坡当季"))
                .andExpect(jsonPath("$.data.goodsSection.seasonLabel").value("九月"))
                .andExpect(jsonPath("$.data.goodsSection.imageCaption").value("当季山野好物"));
    }

    @Test
    @Order(15)
    void publishedProductionRecordExposesItsOwnMedia() throws Exception {
        String adminToken = login("13800000002").path("accessToken").asText();
        String recordBody = mockMvc.perform(post("/api/admin/farmers/1/records")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "plotId": 1,
                                  "stage": "HARVEST",
                                  "occurredAt": "2026-08-29T08:00:00",
                                  "originalText": "现场记录核桃采摘过程。",
                                  "truthConfirmed": true
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long recordId = objectMapper.readTree(recordBody).path("data").path("id").asLong();

        byte[] image = new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
        String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(image));
        String ticketBody = mockMvc.perform(post("/api/media/upload-ticket")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mediaType":"IMAGE",
                                  "contentType":"image/png",
                                  "sizeBytes":%d,
                                  "originalName":"harvest.png",
                                  "checksumSha256":"%s",
                                  "recordId":%d
                                }
                                """.formatted(image.length, checksum, recordId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long mediaId = objectMapper.readTree(ticketBody).path("data").path("media").path("id").asLong();

        mockMvc.perform(put("/api/media/{id}/content", mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(image))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/media/{id}/complete", mediaId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"));

        mockMvc.perform(get("/api/public/media/{id}/content", mediaId))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/admin/farmers/1/records/{recordId}/submit", recordId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/reviews/records/{recordId}/approve", recordId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedText\":\"现场记录核桃采摘过程。\",\"reviewNote\":\"素材与记录一致\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/public/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productionRecords[?(@.id == " + recordId + ")].media[0].id")
                        .value(Math.toIntExact(mediaId)))
                .andExpect(jsonPath("$.data.productionRecords[?(@.id == " + recordId + ")].media[0].mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.data.productionRecords[?(@.id == " + recordId + ")].media[0].url")
                        .value("/api/public/media/" + mediaId + "/content"));
        mockMvc.perform(get("/api/public/media/{id}/content", mediaId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(content().bytes(image));
    }

    private JsonNode login(String phone) throws Exception {
        JsonNode existing = loginTokens.get(phone);
        if (existing != null) {
            return existing;
        }
        boolean seededAccount = phone.equals("13800000001") || phone.equals("13800000002");
        String password = seededAccount ? "12345678" : "Customer@123";
        String endpoint = seededAccount ? "/api/auth/login" : "/api/auth/register";
        String response = mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode tokens = objectMapper.readTree(response).path("data");
        loginTokens.put(phone, tokens);
        return tokens;
    }
}
