package cn.nanpo.window.api.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.admin.AdminContentViews.AttractionAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.AttractionCommand;
import cn.nanpo.window.api.admin.AdminContentViews.ContentStatusView;
import cn.nanpo.window.api.admin.AdminContentViews.ExperienceAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.ExperienceCommand;
import cn.nanpo.window.api.admin.AdminContentViews.HomestayAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.HomestayCommand;
import cn.nanpo.window.api.admin.AdminContentViews.GoodsSectionAdminView;
import cn.nanpo.window.api.admin.AdminContentViews.GoodsSectionCommand;
import cn.nanpo.window.application.ContentAdminService;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.common.api.PageResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/content")
public class ContentAdminController {

    private final ContentAdminService service;

    public ContentAdminController(ContentAdminService service) {
        this.service = service;
    }

    @GetMapping("/site-sections/goods")
    public ApiResponse<GoodsSectionAdminView> goodsSection() {
        return ApiResponse.success(service.goodsSection());
    }

    @PutMapping("/site-sections/goods")
    public ApiResponse<GoodsSectionAdminView> updateGoodsSection(
            @Valid @RequestBody GoodsSectionCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateGoodsSection(command, actor, clientIp(request)));
    }

    @GetMapping("/homestays")
    public ApiResponse<PageResponse<HomestayAdminView>> homestays(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ALL") String status) {
        return ApiResponse.success(service.homestays(page, size, status));
    }

    @PostMapping("/homestays")
    public ApiResponse<HomestayAdminView> createHomestay(
            @Valid @RequestBody HomestayCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.createHomestay(command, actor, clientIp(request)));
    }

    @PutMapping("/homestays/{id}")
    public ApiResponse<HomestayAdminView> updateHomestay(
            @PathVariable long id,
            @Valid @RequestBody HomestayCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateHomestay(id, command, actor, clientIp(request)));
    }

    @GetMapping("/experiences")
    public ApiResponse<PageResponse<ExperienceAdminView>> experiences(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ALL") String status) {
        return ApiResponse.success(service.experiences(page, size, status));
    }

    @PostMapping("/experiences")
    public ApiResponse<ExperienceAdminView> createExperience(
            @Valid @RequestBody ExperienceCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.createExperience(command, actor, clientIp(request)));
    }

    @PutMapping("/experiences/{id}")
    public ApiResponse<ExperienceAdminView> updateExperience(
            @PathVariable long id,
            @Valid @RequestBody ExperienceCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateExperience(id, command, actor, clientIp(request)));
    }

    @GetMapping("/attractions")
    public ApiResponse<PageResponse<AttractionAdminView>> attractions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "ALL") String status) {
        return ApiResponse.success(service.attractions(page, size, status));
    }

    @PostMapping("/attractions")
    public ApiResponse<AttractionAdminView> createAttraction(
            @Valid @RequestBody AttractionCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.createAttraction(command, actor, clientIp(request)));
    }

    @PutMapping("/attractions/{id}")
    public ApiResponse<AttractionAdminView> updateAttraction(
            @PathVariable long id,
            @Valid @RequestBody AttractionCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.updateAttraction(id, command, actor, clientIp(request)));
    }

    @PostMapping("/{kind}/{id}/publish")
    public ApiResponse<ContentStatusView> publish(
            @PathVariable String kind,
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.setPublished(kind, id, true, actor, clientIp(request)));
    }

    @PostMapping("/{kind}/{id}/unpublish")
    public ApiResponse<ContentStatusView> unpublish(
            @PathVariable String kind,
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.setPublished(kind, id, false, actor, clientIp(request)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
    }
}
