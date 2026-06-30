package com.example.sftp;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class RemotePathResolver {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    private final Clock clock;

    public RemotePathResolver(Clock clock) {
        this.clock = clock;
    }

    public String resolve(String remoteDirectory, String fileName, RemoteWriteMode mode) {
        String normalizedDirectory = normalizeDirectory(remoteDirectory);
        return switch (mode) {
            case OVERWRITE, FAIL_IF_EXISTS -> normalizedDirectory + "/" + fileName;
            case UNIQUE_NAME -> normalizedDirectory + "/" + appendUniqueSuffix(fileName);
        };
    }

    static String normalizeDirectory(String remoteDirectory) {
        if (remoteDirectory == null || remoteDirectory.isBlank()) {
            throw new IllegalArgumentException("remoteDirectory must not be blank");
        }

        String trimmed = remoteDirectory.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    String appendUniqueSuffix(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        String timestamp = FORMATTER.format(Instant.now(clock));
        String suffix = "-" + timestamp + "-" + UUID.randomUUID();

        if (extensionIndex <= 0) {
            return fileName + suffix;
        }

        String name = fileName.substring(0, extensionIndex);
        String extension = fileName.substring(extensionIndex);
        return name + suffix + extension;
    }
}
