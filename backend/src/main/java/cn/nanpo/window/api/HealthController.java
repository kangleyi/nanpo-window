package cn.nanpo.window.api;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.nanpo.window.common.api.ApiResponse;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<HealthView> health() {
        return ApiResponse.success(new HealthView(
                "UP",
                "nanpo-window-backend",
                Instant.now()));
    }

    public record HealthView(String status, String service, Instant timestamp) {
    }
}
