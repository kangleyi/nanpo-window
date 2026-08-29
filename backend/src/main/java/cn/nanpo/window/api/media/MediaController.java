package cn.nanpo.window.api.media;

import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.api.media.MediaViews.MediaView;
import cn.nanpo.window.api.media.MediaViews.UploadTicketCommand;
import cn.nanpo.window.api.media.MediaViews.UploadTicketView;
import cn.nanpo.window.application.MediaService;
import cn.nanpo.window.application.MediaService.MediaContent;
import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService service;

    public MediaController(MediaService service) {
        this.service = service;
    }

    @PostMapping("/upload-ticket")
    public ApiResponse<UploadTicketView> createTicket(
            @Valid @RequestBody UploadTicketCommand command,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.createTicket(command, actor, clientIp(request)));
    }

    @PutMapping(value = "/{id}/content", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ApiResponse<MediaView> upload(
            @PathVariable long id,
            @RequestBody byte[] content,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.upload(id, content, actor, clientIp(request)));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<MediaView> complete(
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal actor,
            HttpServletRequest request) {
        return ApiResponse.success(service.complete(id, actor, clientIp(request)));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal actor) {
        MediaContent media = service.content(id, actor);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"media-" + id + "\"")
                .body(media.content());
    }

    @GetMapping("/{id}/status")
    public ApiResponse<MediaView> status(
            @PathVariable long id,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.success(service.status(id, actor));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr() : forwardedFor.split(",", 2)[0].trim();
    }
}
