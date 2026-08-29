package cn.nanpo.window.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.nanpo.window.api.admin.AdminContentViews.AttractionAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.AttractionCommand;
import cn.nanpo.window.api.admin.AdminContentViews.ContentStatusView;
import cn.nanpo.window.api.admin.AdminContentViews.ExperienceAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.ExperienceCommand;
import cn.nanpo.window.api.admin.AdminContentViews.HomestayAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.HomestayCommand;
import cn.nanpo.window.api.admin.AdminContentViews.GoodsSectionAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.GoodsSectionCommand;
import cn.nanpo.window.common.api.PageResponse;
import cn.nanpo.window.common.error.ApiException;
import cn.nanpo.window.common.error.ErrorCode;
import cn.nanpo.window.infrastructure.persistence.ContentAdminRepository;
import cn.nanpo.window.infrastructure.persistence.ContentAdminRepository.ContentKind;
import cn.nanpo.window.security.UserPrincipal;

@Service
public class ContentAdminService {

    private final ContentAdminRepository repository;
    private final AuditService auditService;

    public ContentAdminService(ContentAdminRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public GoodsSectionAdminView goodsSection() {
        return repository.findGoodsSection()
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "尚未配置已发布的村庄主页"));
    }

    @Transactional
    public GoodsSectionAdminView updateGoodsSection(
            GoodsSectionCommand command, UserPrincipal actor, String ipAddress) {
        GoodsSectionAdminView current = goodsSection();
        repository.updateGoodsSection(current.siteId(), command);
        auditService.record(actor.id(), "CONTENT_UPDATE", "GOODS_SECTION", String.valueOf(current.siteId()), ipAddress);
        return repository.findGoodsSection().orElseThrow();
    }

    @Transactional(readOnly = true)
    public PageResponse<HomestayAdminView> homestays(int page, int size, String status) {
        PageSpec spec = pageSpec(page, size);
        String normalized = status(status);
        return new PageResponse<>(repository.findHomestays(normalized, spec.size(), spec.offset()),
                spec.page(), spec.size(), repository.countHomestays(normalized));
    }

    @Transactional
    public HomestayAdminView createHomestay(HomestayCommand command, UserPrincipal actor, String ipAddress) {
        long id = repository.createHomestay(command);
        HomestayAdminView created = repository.findHomestay(id).orElseThrow();
        auditService.record(actor.id(), "CONTENT_CREATE", "HOMESTAY", String.valueOf(id), ipAddress);
        return created;
    }

    @Transactional
    public HomestayAdminView updateHomestay(long id, HomestayCommand command, UserPrincipal actor, String ipAddress) {
        require(repository.findHomestay(id), "民宿不存在");
        repository.updateHomestay(id, command);
        auditService.record(actor.id(), "CONTENT_UPDATE", "HOMESTAY", String.valueOf(id), ipAddress);
        return repository.findHomestay(id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public PageResponse<ExperienceAdminView> experiences(int page, int size, String status) {
        PageSpec spec = pageSpec(page, size);
        String normalized = status(status);
        return new PageResponse<>(repository.findExperiences(normalized, spec.size(), spec.offset()),
                spec.page(), spec.size(), repository.countExperiences(normalized));
    }

    @Transactional
    public ExperienceAdminView createExperience(ExperienceCommand command, UserPrincipal actor, String ipAddress) {
        long id = repository.createExperience(command);
        ExperienceAdminView created = repository.findExperience(id).orElseThrow();
        auditService.record(actor.id(), "CONTENT_CREATE", "EXPERIENCE", String.valueOf(id), ipAddress);
        return created;
    }

    @Transactional
    public ExperienceAdminView updateExperience(long id, ExperienceCommand command, UserPrincipal actor, String ipAddress) {
        require(repository.findExperience(id), "游玩项目不存在");
        repository.updateExperience(id, command);
        auditService.record(actor.id(), "CONTENT_UPDATE", "EXPERIENCE", String.valueOf(id), ipAddress);
        return repository.findExperience(id).orElseThrow();
    }

    @Transactional(readOnly = true)
    public PageResponse<AttractionAdminView> attractions(int page, int size, String status) {
        PageSpec spec = pageSpec(page, size);
        String normalized = status(status);
        return new PageResponse<>(repository.findAttractions(normalized, spec.size(), spec.offset()),
                spec.page(), spec.size(), repository.countAttractions(normalized));
    }

    @Transactional
    public AttractionAdminView createAttraction(AttractionCommand command, UserPrincipal actor, String ipAddress) {
        long id = repository.createAttraction(command);
        AttractionAdminView created = repository.findAttraction(id).orElseThrow();
        auditService.record(actor.id(), "CONTENT_CREATE", "ATTRACTION", String.valueOf(id), ipAddress);
        return created;
    }

    @Transactional
    public AttractionAdminView updateAttraction(long id, AttractionCommand command, UserPrincipal actor, String ipAddress) {
        require(repository.findAttraction(id), "景点不存在");
        repository.updateAttraction(id, command);
        auditService.record(actor.id(), "CONTENT_UPDATE", "ATTRACTION", String.valueOf(id), ipAddress);
        return repository.findAttraction(id).orElseThrow();
    }

    @Transactional
    public ContentStatusView setPublished(
            String kindPath, long id, boolean published, UserPrincipal actor, String ipAddress) {
        ContentKind kind = ContentKind.fromPath(kindPath)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_ARGUMENT, "不支持的内容类型"));
        ContentStatusView current = repository.findStatus(kind, id)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "内容不存在"));
        String targetStatus = published ? "PUBLISHED" : "DRAFT";
        if (!current.status().equals(targetStatus)) {
            repository.setPublished(kind, id, published);
            auditService.record(actor.id(), published ? "CONTENT_PUBLISH" : "CONTENT_UNPUBLISH",
                    kind.name(), String.valueOf(id), ipAddress);
        }
        return repository.findStatus(kind, id).orElseThrow();
    }

    private <T> T require(java.util.Optional<T> value, String message) {
        return value.orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, message));
    }

    private String status(String value) {
        String normalized = value == null || value.isBlank() ? "ALL" : value.toUpperCase();
        if (!java.util.Set.of("ALL", "DRAFT", "PUBLISHED").contains(normalized)) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "status 只能为 ALL、DRAFT 或 PUBLISHED");
        }
        return normalized;
    }

    private PageSpec pageSpec(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "page 必须大于等于 1，size 必须在 1 到 100 之间");
        }
        return new PageSpec(page, size, (page - 1) * size);
    }

    private record PageSpec(int page, int size, int offset) {
    }
}
