package cn.nanpo.window.common.api;

public record ApiResponse<T>(String requestId, String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(RequestContext.requestId(), "OK", "success", data);
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(RequestContext.requestId(), code, message, null);
    }
}

