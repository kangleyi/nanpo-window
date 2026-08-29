package cn.nanpo.window.infrastructure.storage;

import java.time.Instant;
import java.util.Map;

public interface ObjectStorage {

    UploadTicket createUploadTicket(UploadRequest request);

    void put(String storageKey, byte[] content);

    StoredObject stat(String storageKey);

    byte[] read(String storageKey);

    record UploadRequest(
            long ownerUserId,
            String mediaType,
            String contentType,
            long sizeBytes,
            String checksumSha256) {
    }

    record UploadTicket(
            String storageKey,
            String uploadUrl,
            Map<String, String> headers,
            Instant expiresAt) {
    }

    record StoredObject(long sizeBytes, String checksumSha256) {
    }
}
