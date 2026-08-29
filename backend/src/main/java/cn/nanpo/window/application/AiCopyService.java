package cn.nanpo.window.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.nanpo.window.api.farmer.AiCopyViews.AiCopyView;
import cn.nanpo.window.api.farmer.AiCopyViews.ConfirmCopyCommand;
import cn.nanpo.window.api.farmer.AiCopyViews.GenerateCopyCommand;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.ai.AiCopyProvider;
import cn.nanpo.window.infrastructure.ai.AiCopyProvider.GeneratedCopy;
import cn.nanpo.window.infrastructure.ai.AiCopyProvider.SourceFact;
import cn.nanpo.window.infrastructure.persistence.AiCopyRepository;
import cn.nanpo.window.infrastructure.persistence.AiCopyRepository.ProductSource;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class AiCopyService {

    private final AiCopyRepository repository;
    private final AiCopyProvider provider;
    private final AuditService auditService;

    public AiCopyService(AiCopyRepository repository, AiCopyProvider provider, AuditService auditService) {
        this.repository = repository;
        this.provider = provider;
        this.auditService = auditService;
    }

    @Transactional
    public AiCopyView generate(
            long productId, GenerateCopyCommand command, UserPrincipal actor, String ipAddress) {
        ProductSource product = repository.findOwnedProduct(actor.id(), productId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_DENIED, "只能为本人农品生成文案"));
        List<SourceFact> facts = repository.publishedFacts(productId);
        if (facts.isEmpty()) {
            throw new ApiException(ErrorCode.CONFLICT, "至少需要一条已审核发布的生产记录作为来源");
        }
        GeneratedCopy generated = provider.generate(new AiCopyProvider.CopyRequest(
                command.scene(), product.name(), product.farmerName(), product.season(), facts));
        long id = repository.create(actor.id(), command.scene(), facts.stream().map(SourceFact::recordId).toList(), generated);
        auditService.record(actor.id(), "AI_COPY_GENERATE", "AI_GENERATION", String.valueOf(id), ipAddress);
        return repository.find(id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<AiCopyView> list(UserPrincipal actor) {
        return repository.findByOwner(actor.id());
    }

    @Transactional
    public AiCopyView confirm(
            long id, ConfirmCopyCommand command, UserPrincipal actor, String ipAddress) {
        AiCopyView current = repository.find(id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "AI 文案不存在"));
        if (!repository.findByOwner(actor.id()).stream().anyMatch(item -> item.id() == id)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED, "无权确认该文案");
        }
        if (!"DRAFT".equals(current.status())) {
            throw new ApiException(ErrorCode.CONFLICT, "该文案已经确认");
        }
        if (!repository.confirm(id, actor.id(), current.version(), command.confirmedText().trim())) {
            throw new ApiException(ErrorCode.CONFLICT, "文案已被修改，请刷新后重试");
        }
        auditService.record(actor.id(), "AI_COPY_CONFIRM", "AI_GENERATION", String.valueOf(id), ipAddress);
        return repository.find(id).orElseThrow();
    }
}
