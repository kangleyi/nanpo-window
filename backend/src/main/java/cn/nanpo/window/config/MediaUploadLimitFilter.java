package cn.nanpo.window.config;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.nanpo.window.common.api.ApiResponse;
import cn.nanpo.window.common.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MediaUploadLimitFilter extends OncePerRequestFilter {

    private static final long MAX_UPLOAD_BYTES = 100L * 1024 * 1024;

    private final ObjectMapper objectMapper;

    public MediaUploadLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("PUT".equals(request.getMethod())
                && request.getRequestURI().matches("/api/media/\\d+/content")
                && request.getContentLengthLong() > MAX_UPLOAD_BYTES) {
            ErrorCode error = ErrorCode.PAYLOAD_TOO_LARGE;
            response.setStatus(error.status().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.failure(error.code(), "媒体文件超过 100MB 上限"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
