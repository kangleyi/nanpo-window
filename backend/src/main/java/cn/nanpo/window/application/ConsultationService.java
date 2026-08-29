package cn.nanpo.window.application;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.nanpo.window.api.inquiry.InquiryViews.InquiryCommand;
import cn.nanpo.window.api.inquiry.InquiryViews.InquiryView;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.persistence.ConsultationRepository;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class ConsultationService {

    private static final Set<String> STATUSES = Set.of("ALL", "NEW", "CONTACTED", "CLOSED");
    private static final Set<String> SOURCE_TYPES = Set.of("ALL", "HOMESTAY", "EXPERIENCE");

    private final ConsultationRepository repository;
    private final AuditService auditService;

    public ConsultationService(ConsultationRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public InquiryView create(InquiryCommand command) {
        String targetName = repository.findPublishedTarget(command.sourceType(), command.sourceId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "咨询对象不存在或已下线"));
        long id = repository.create(command, targetName);
        return repository.find(id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<InquiryView> adminList(String status, String sourceType) {
        String normalizedStatus = normalize(status, STATUSES, "咨询状态");
        String normalizedType = normalize(sourceType, SOURCE_TYPES, "咨询类型");
        return repository.findAdmin(normalizedStatus, normalizedType);
    }

    @Transactional
    public InquiryView updateStatus(long id, String action, UserPrincipal actor, String ipAddress) {
        String status = switch (action) {
            case "contacted" -> "CONTACTED";
            case "closed" -> "CLOSED";
            default -> throw new ApiException(ErrorCode.INVALID_ARGUMENT, "不支持的咨询状态操作");
        };
        if (!repository.updateStatus(id, status)) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "咨询留言不存在");
        }
        auditService.record(actor.id(), "INQUIRY_" + status, "CONSULTATION_INQUIRY", String.valueOf(id), ipAddress);
        return repository.find(id).orElseThrow();
    }

    private String normalize(String value, Set<String> allowed, String label) {
        String normalized = value == null || value.isBlank() ? "ALL" : value.toUpperCase();
        if (!allowed.contains(normalized)) throw new ApiException(ErrorCode.INVALID_ARGUMENT, label + "不支持");
        return normalized;
    }
}
