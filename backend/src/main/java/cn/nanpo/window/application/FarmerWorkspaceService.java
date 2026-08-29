package cn.nanpo.window.application;

import java.time.Clock;
import java.time.LocalDateTime;
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
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.persistence.FarmerWorkspaceRepository;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class FarmerWorkspaceService {

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

    @Transactional
    public ProductManageView createProduct(ProductCommand command, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = farmer(actor);
        if (command.plotId() != null && !repository.ownsPlot(farmer.id(), command.plotId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "不能使用其他农户的地块");
        }
        long uniqueSkuCodes = command.skus().stream().map(item -> item.code().toUpperCase()).distinct().count();
        if (uniqueSkuCodes != command.skus().size()) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "SKU 编码不能重复");
        }
        try {
            long id = repository.createProduct(farmer.id(), command);
            auditService.record(actor.id(), "FARM_PRODUCT_CREATE", "PRODUCT", String.valueOf(id), ipAddress);
            return repository.findProduct(farmer.id(), id).orElseThrow();
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(ErrorCode.CONFLICT, "SKU 编码已存在");
        }
    }

    @Transactional(readOnly = true)
    public List<FarmRecordView> records(UserPrincipal actor, String status) {
        return repository.findRecords(farmer(actor).id(), status(status, true));
    }

    @Transactional
    public FarmRecordView createRecord(FarmRecordCommand command, UserPrincipal actor, String ipAddress) {
        FarmerProfileView farmer = farmer(actor);
        if (!repository.ownsProduct(farmer.id(), command.productId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "只能为本人农品添加生产记录");
        }
        if (command.plotId() != null && !repository.ownsPlot(farmer.id(), command.plotId())) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "不能关联其他农户的地块");
        }
        LocalDateTime latestAllowed = LocalDateTime.now(clock).plusMinutes(5);
        if (command.occurredAt().isAfter(latestAllowed)) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "生产时间不能晚于当前时间");
        }
        long id = repository.createRecord(farmer.id(), command);
        auditService.record(actor.id(), "FARM_RECORD_CREATE", "FARM_RECORD", String.valueOf(id), ipAddress);
        return repository.findRecord(farmer.id(), id).orElseThrow();
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
