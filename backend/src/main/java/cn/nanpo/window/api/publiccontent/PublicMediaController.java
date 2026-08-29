package cn.nanpo.window.api.publiccontent;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.application.MediaService;
import cn.nanpo.window.application.MediaService.MediaContent;

@RestController
@RequestMapping("/api/public/media")
public class PublicMediaController {

    private final MediaService service;

    public PublicMediaController(MediaService service) {
        this.service = service;
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@PathVariable long id) {
        MediaContent media = service.publicContent(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"media-" + id + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .body(media.content());
    }
}
