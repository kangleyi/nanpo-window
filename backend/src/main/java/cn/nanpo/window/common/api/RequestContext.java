package cn.nanpo.window.common.api;

import java.util.UUID;

public final class RequestContext {

    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setRequestId(String requestId) {
        REQUEST_ID.set(requestId);
    }

    public static String requestId() {
        String requestId = REQUEST_ID.get();
        return requestId == null ? UUID.randomUUID().toString() : requestId;
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}

