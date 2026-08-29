package cn.nanpo.window.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
public class LocalObjectStorage implements ObjectStorage {

    private final Path root;

    public LocalObjectStorage(@Value("${app.storage.local-root:${java.io.tmpdir}/nanpo-window-media}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public UploadTicket createUploadTicket(UploadRequest request) {
        String key = "users/" + request.ownerUserId() + "/" + UUID.randomUUID();
        return new UploadTicket(key, "", Map.of(), Instant.now().plus(15, ChronoUnit.MINUTES));
    }

    @Override
    public void put(String storageKey, byte[] content) {
        Path target = target(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("无法写入本地对象存储", exception);
        }
    }

    @Override
    public StoredObject stat(String storageKey) {
        Path target = target(storageKey);
        try {
            byte[] content = Files.readAllBytes(target);
            return new StoredObject(content.length, sha256(content));
        } catch (IOException exception) {
            throw new IllegalStateException("对象不存在或无法读取", exception);
        }
    }

    @Override
    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(target(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("对象不存在或无法读取", exception);
        }
    }

    private Path target(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法存储键");
        }
        return target;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行时不支持 SHA-256", exception);
        }
    }
}
