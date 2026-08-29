package cn.nanpo.window.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import cn.nanpo.window.infrastructure.storage.ObjectStorage.UploadRequest;
import cn.nanpo.window.infrastructure.storage.ObjectStorage.UploadTicket;

class CosObjectStorageTest {

    @Test
    void createsShortLivedPutTicketWithoutExposingCredentialsInHeaders() {
        CosObjectStorage storage = new CosObjectStorage(
                "AKIDexample", "secret-example", "", "ap-guangzhou",
                "nanpo-test-1250000000", "/nanpo-window/uploads/", java.time.Duration.ofMinutes(5));
        try {
            UploadTicket ticket = storage.createUploadTicket(new UploadRequest(
                    42L, "IMAGE", "image/jpeg", 128L,
                    "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef", "田间照片.JPG"));

            assertThat(ticket.storageKey())
                    .startsWith("nanpo-window/uploads/")
                    .contains("/users/42/")
                    .endsWith(".jpg");
            assertThat(ticket.uploadUrl()).startsWith("https://").contains("nanpo-test-1250000000");
            assertThat(ticket.headers()).containsEntry("Content-Type", "image/jpeg")
                    .containsEntry("x-cos-meta-sha256",
                            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                    .doesNotContainKeys("Authorization", "SecretId", "SecretKey");
        } finally {
            storage.destroy();
        }
    }
}
