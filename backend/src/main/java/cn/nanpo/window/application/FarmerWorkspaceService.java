package cn.nanpo.window.application;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.nanpo.window.api.admin.FarmReviewViews.ApproveRecordCommand;
import cn.nanpo.window.api.admin.FarmReviewViews.RejectRecordCommand;
import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordCommand;
import cn.nanpo.window.api.farmer.FarmerViews.FarmRecordView;
import cn.nanpo.window.api.farmer.FarmerViews.FarmerDashboardView;
import cn.nanpo.window.api.farmer.FarmerViews.FarmerProfileView;
import cn.nanpo.window.api.farmer.FarmerViews.PlotCommand;
import cn.nanpo.window.api.farmer.FarmerViews.PlotView;
import cn.nanpo.window.api.farmer.FarmerViews.ProductCommand;
import cn.nanpo.window.api.farmer.FarmerViews.ProductManageView;
import cn.nanpo.window.api.farmer.FarmerViews.SkuManageView;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.persistence.FarmerWorkspaceRepository;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class FarmerWorkspaceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> REVIEW_STATUSES = Set.of(
            "ALL", "PENDING_REVIEW", "PUBLISHED", "REJECTED");

    private final FarmerWorkspaceRepository repository;
    private final AuditService auditService;
    private final Clock clock;

    @Autowired
    public FarmerWorkspaceService(FarmerWorkspaceRepository repository, AuditService auditService) {
        this(repository, auditService, Clock.systemDefaultZone());
    }

    FarmerWorkspaceService(FarmerWorkspaceRepository repository, AuditService auditService, Clock clock) {
        this.repository = repository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FarmerDashboardView dashboard(UserPrincipal actor) {
        FarmerProfileView farmer = farmer(actor);
        return repository.dashboard(farmer);
    }

    @Transactional(readOnly = true)
    public List<PlotView> plots(UserPrincipal actor) {
        return repository.findPlots(farmer(actor).id());
    }

    @Transactional
    public PlotView createPlot(PlotCommand command, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = farmer(actor);
        try {
            long id = repository.createPlot(farmer.id(), command);
            auditService.record(actor.id(), "FARM_PLOT_CREATE", "LAND_PLOT", String.valueOf(id), ipAddress);
            return repository.findPlot(farmer.id(), id).orElseThrow();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.CONFLICT, "地块编号已存在");
        }
    }

    @Transactional(readOnly = true)
    public List<ProductManageView> products(UserPrincipal actor) {
        return repository.findProducts(farmer(actor).id());
    }

    @Transactional(readOnly = true)
    public List<FarmerProfileView> farmers() {
        return repository.findActiveFarmers();
    }

    @Transactional(readOnly = true)
    public List<PlotView> adminPlots(long farmerId) {
        return repository.findPlots(requireFarmer(farmerId).id());
    }

    @Transactional(readOnly = true)
    public List<ProductManageView> adminProducts(long farmerId) {
        return repository.findProducts(requireFarmer(farmerId).id());
    }

    @Transactional
    public ProductManageView createProduct(ProductCommand command, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = farmer(actor);
        validateProductCommand(farmer.id(), null, command);
        try {
            long id = repository.createProduct(farmer.id(), command);
            auditService.record(actor.id(), "FARM_PRODUCT_CREATE", "PRODUCT", String.valueOf(id), ipAddress);
            return repository.findProduct(farmer.id(), id).orElseThrow();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.CONFLICT, "规格编码生成冲突，请重试");
        }
    }

    @Transactional
    public ProductManageView createProductForFarmer(
            long farmerId, ProductCommand command, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = requireFarmer(farmerId);
        validateProductCommand(farmer.id(), null, command);
        try {
            long id = repository.createProduct(farmer.id(), command);
            auditService.record(actor.id(), "ADMIN_PRODUCT_CREATE", "PRODUCT", String.valueOf(id), ipAddress);
            return repository.findProduct(farmer.id(), id).orElseThrow();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.CONFLICT, "规格编码生成冲突，请重试");
        }
    }

    @Transactional
    public ProductManageView updateProductForFarmer(
            long farmerId, long productId, ProductCommand command, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = requireFarmer(farmerId);
        if (!repository.ownsProduct(farmer.id(), productId)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "农产品不存在或不属于所选村民");
        }
        validateProductCommand(farmer.id(), productId, command);
        try {
            if (!repository.updateProduct(farmer.id(), productId, command)) {
                throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "农产品不存在");
            }
            auditService.record(actor.id(), "ADMIN_PRODUCT_UPDATE", "PRODUCT", String.valueOf(productId), ipAddress);
            return repository.findProduct(farmer.id(), productId).orElseThrow();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.CONFLICT, "规格编码生成冲突，请重试");
        }
    }

    @Transactional
    public ProductManageView setProductPublished(
            long farmerId, long productId, boolean published, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = requireFarmer(farmerId);
        ProductManageView product = repository.findProduct(farmer.id(), productId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "农产品不存在或不属于所选村民"));
        if (published && product.skus().stream().noneMatch(SkuManageView::enabled)) {
            throw new ApiException(ErrorCode.CONFLICT, "至少配置一个可售规格后才能上架");
        }
        repository.setProductPublished(farmer.id(), productId, published);
        auditService.record(actor.id(), published ? "ADMIN_PRODUCT_PUBLISH" : "ADMIN_PRODUCT_UNPUBLISH",
                "PRODUCT", String.valueOf(productId), ipAddress);
        return repository.findProduct(farmer.id(), productId).orElseThrow();
    }

    @Transactional
    public FarmRecordView createRecordForFarmer(
            long farmerId, FarmRecordCommand command, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = requireFarmer(farmerId);
        validateRecordCommand(farmer.id(), command);
        long id = repository.createRecord(farmer.id(), command);
        auditService.record(actor.id(), "ADMIN_FARM_RECORD_CREATE", "FARM_RECORD", String.valueOf(id), ipAddress);
        return repository.findRecord(farmer.id(), id).orElseThrow();
    }

    @Transactional
    public FarmRecordView submitRecordForFarmer(
            long farmerId, long recordId, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = requireFarmer(farmerId);
        FarmRecordView record = repository.findRecord(farmer.id(), recordId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "生产记录不存在或不属于所选村民"));
        if (!Set.of("DRAFT", "REJECTED").contains(record.status())) {
            throw new ApiException(ErrorCode.CONFLICT, "只有草稿或已驳回记录可以提交审核");
        }
        if (!repository.submitRecord(farmer.id(), recordId, record.version())) {
            throw new ApiException(ErrorCode.CONFLICT, "记录已被修改，请刷新后重试");
        }
        auditService.record(actor.id(), "ADMIN_FARM_RECORD_SUBMIT", "FARM_RECORD", String.valueOf(recordId), ipAddress);
        return repository.findRecord(farmer.id(), recordId).orElseThrow();
    }

    private void validateProductCommand(long farmerId, Long productId, ProductCommand command) {
        if (command.plotId() != null && !repository.ownsPlot(farmerId, command.plotId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "不能使用其他农户的地块");
        }
        long specifiedIds = command.skus().stream().filter(item -> item.id() != null).count();
        long uniqueIds = command.skus().stream().map(item -> item.id()).filter(java.util.Objects::nonNull).distinct().count();
        if (specifiedIds != uniqueIds) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "同一规格不能重复提交");
        }
        if (productId == null && specifiedIds > 0) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "新农产品的规格编号由后端自动生成");
        }
        if (productId != null && command.skus().stream()
                .anyMatch(item -> item.id() != null && !repository.ownsSku(productId, item.id()))) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "不能修改其他农产品的规格");
        }
    }

    @Transactional(readOnly = true)
    public List<FarmRecordView> records(UserPrincipal actor, String status) {
        return repository.findRecords(farmer(actor).id(), status(status, true));
    }

    @Transactional
    public FarmRecordView createRecord(FarmRecordCommand command, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = farmer(actor);
        validateRecordCommand(farmer.id(), command);
        long id = repository.createRecord(farmer.id(), command);
        auditService.record(actor.id(), "FARM_RECORD_CREATE", "FARM_RECORD", String.valueOf(id), ipAddress);
        return repository.findRecord(farmer.id(), id).orElseThrow();
    }

    private void validateRecordCommand(long farmerId, FarmRecordCommand command) {
        if (!repository.ownsProduct(farmerId, command.productId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "只能为所选村民的农品添加生产记录");
        }
        if (command.plotId() != null && !repository.ownsPlot(farmerId, command.plotId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "不能关联其他村民的地块");
        }
        var latestAllowed = clock.instant().plus(Duration.ofMinutes(5));
        var occurredAt = command.occurredAt().atZone(BUSINESS_ZONE).toInstant();
        if (occurredAt.isAfter(latestAllowed)) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "生产时间不能晚于当前时间");
        }
    }

    @Transactional
    public FarmRecordView submitRecord(long id, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = farmer(actor);
        FarmRecordView record = repository.findRecord(farmer.id(), id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "生产记录不存在"));
        if (!Set.of("DRAFT", "REJECTED").contains(record.status())) {
            throw new ApiException(ErrorCode.CONFLICT, "只有草稿或已驳回记录可以提交审核");
        }
        if (!repository.submitRecord(farmer.id(), id, record.version())) {
            throw new ApiException(ErrorCode.CONFLICT, "记录已被修改，请刷新后重试");
        }
        auditService.record(actor.id(), "FARM_RECORD_SUBMIT", "FARM_RECORD", String.valueOf(id), ipAddress);
        return repository.findRecord(farmer.id(), id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<FarmRecordView> reviewQueue(String status) {
        return repository.findReviewQueue(status(status, false));
    }

    @Transactional
    public FarmRecordView approveRecord(
            long id, ApproveRecordCommand command, UserPrincipal reviewer, String ipAddress) {
        FarmRecordView record = reviewable(id);
        String confirmedText = command.confirmedText() == null || command.confirmedText().isBlank()
                ? record.originalText()
                : command.confirmedText().trim();
        if (!repository.approveRecord(
                id, record.version(), reviewer.id(), confirmedText, blankToNull(command.reviewNote()))) {
            throw new ApiException(ErrorCode.CONFLICT, "记录已被其他人审核，请刷新后重试");
        }
        auditService.record(reviewer.id(), "FARM_RECORD_APPROVE", "FARM_RECORD", String.valueOf(id), ipAddress);
        return repository.findRecord(id).orElseThrow();
    }

    @Transactional
    public FarmRecordView rejectRecord(
            long id, RejectRecordCommand command, UserPrincipal reviewer, String ipAddress) {
        FarmRecordView record = reviewable(id);
        if (!repository.rejectRecord(id, record.version(), reviewer.id(), command.reviewNote().trim())) {
            throw new ApiException(ErrorCode.CONFLICT, "记录已被其他人审核，请刷新后重试");
        }
        auditService.record(reviewer.id(), "FARM_RECORD_REJECT", "FARM_RECORD", String.valueOf(id), ipAddress);
        return repository.findRecord(id).orElseThrow();
    }

    private FarmerProfileView farmer(UserPrincipal actor) {
        FarmerProfileView farmer = repository.findFarmerByUserId(actor.id())
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "当前账号未绑定农户档案"));
        if (!"APPROVED".equals(farmer.certificationStatus())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "农户身份尚未通过认证");
        }
        return farmer;
    }

    private FarmerProfileView requireFarmer(long farmerId) {
        return repository.findFarmer(farmerId)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "村民档案不存在或已停用"));
    }

    private FarmRecordView reviewable(long id) {
        FarmRecordView record = repository.findRecord(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "生产记录不存在"));
        if (!"PENDING_REVIEW".equals(record.status())) {
            throw new ApiException(ErrorCode.CONFLICT, "只能审核待审记录");
        }
        return record;
    }

    private String status(String value, boolean includeDraft) {
        String normalized = value == null || value.isBlank() ? "ALL" : value.toUpperCase();
        Set<String> allowed = includeDraft
                ? Set.of("ALL", "DRAFT", "PENDING_REVIEW", "PUBLISHED", "REJECTED")
                : REVIEW_STATUSES;
        if (!allowed.contains(normalized)) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "不支持的记录状态");
        }
        return normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
