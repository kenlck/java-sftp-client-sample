package com.example.sftp;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemotePathResolverTest {

    private final RemotePathResolver resolver = new RemotePathResolver(
            Clock.fixed(Instant.parse("2026-07-01T10:15:30Z"), ZoneOffset.UTC)
    );

    @Test
    void shouldResolveOverwritePath() {
        assertThat(resolver.resolve("/upload/demo", "sample-file.txt", RemoteWriteMode.OVERWRITE))
                .isEqualTo("/upload/demo/sample-file.txt");
    }

    @Test
    void shouldResolveFailIfExistsPath() {
        assertThat(resolver.resolve("/upload/demo/", "memory-text.txt", RemoteWriteMode.FAIL_IF_EXISTS))
                .isEqualTo("/upload/demo/memory-text.txt");
    }

    @Test
    void shouldResolveUniquePathWithTimestampAndUuid() {
        assertThat(resolver.resolve("/upload/demo", "memory-binary.bin", RemoteWriteMode.UNIQUE_NAME))
                .matches("/upload/demo/memory-binary-20260701-101530-[0-9a-f\\-]{36}\\.bin");
    }

    @Test
    void shouldRejectBlankDirectory() {
        assertThatThrownBy(() -> RemotePathResolver.normalizeDirectory("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remoteDirectory");
    }
}
