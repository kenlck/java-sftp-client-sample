package com.example.sftp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class SftpDemoRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SftpDemoRunner.class);

    private final SftpUploadService sftpUploadService;

    public SftpDemoRunner(SftpUploadService sftpUploadService) {
        this.sftpUploadService = sftpUploadService;
    }

    @Override
    public void run(String... args) throws Exception {
        Path localFile = createSampleFile();
        byte[] textPayload = ("hello from in-memory text upload at " + Instant.now()).getBytes(StandardCharsets.UTF_8);
        byte[] binaryPayload = new byte[]{0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef};

        List<UploadResult> results = new ArrayList<>();

        runCase(results, RemoteWriteMode.OVERWRITE, UploadScenario.DISK_FILE,
                () -> sftpUploadService.uploadFile(localFile, "sample-file.txt", RemoteWriteMode.OVERWRITE));
        runCase(results, RemoteWriteMode.OVERWRITE, UploadScenario.IN_MEMORY_TEXT,
                () -> sftpUploadService.uploadStream(new ByteArrayInputStream(textPayload), "memory-text.txt", RemoteWriteMode.OVERWRITE));
        runCase(results, RemoteWriteMode.OVERWRITE, UploadScenario.IN_MEMORY_BINARY,
                () -> sftpUploadService.uploadStream(new ByteArrayInputStream(binaryPayload), "memory-binary.bin", RemoteWriteMode.OVERWRITE));

        runCase(results, RemoteWriteMode.FAIL_IF_EXISTS, UploadScenario.FAIL_IF_EXISTS_FIRST_UPLOAD,
                () -> sftpUploadService.uploadStream(new ByteArrayInputStream(textPayload), "fail-if-exists.txt", RemoteWriteMode.FAIL_IF_EXISTS));
        runExpectedFailureCase(results, RemoteWriteMode.FAIL_IF_EXISTS, UploadScenario.FAIL_IF_EXISTS_SECOND_UPLOAD,
                () -> sftpUploadService.uploadStream(new ByteArrayInputStream(textPayload), "fail-if-exists.txt", RemoteWriteMode.FAIL_IF_EXISTS));

        runCase(results, RemoteWriteMode.UNIQUE_NAME, UploadScenario.DISK_FILE,
                () -> sftpUploadService.uploadFile(localFile, "sample-file.txt", RemoteWriteMode.UNIQUE_NAME));
        runCase(results, RemoteWriteMode.UNIQUE_NAME, UploadScenario.IN_MEMORY_TEXT,
                () -> sftpUploadService.uploadStream(new ByteArrayInputStream(textPayload), "memory-text.txt", RemoteWriteMode.UNIQUE_NAME));
        runCase(results, RemoteWriteMode.UNIQUE_NAME, UploadScenario.IN_MEMORY_BINARY,
                () -> sftpUploadService.uploadStream(new ByteArrayInputStream(binaryPayload), "memory-binary.bin", RemoteWriteMode.UNIQUE_NAME));

        log.info("Local sample file: {}", localFile.toAbsolutePath());
        log.info("In-memory text payload: {}", new String(textPayload, StandardCharsets.UTF_8));
        log.info("In-memory binary payload (hex): {}", HexFormat.of().formatHex(binaryPayload));
        logSummary(results);
    }

    private Path createSampleFile() throws IOException {
        Path directory = Path.of("target", "generated-demo-files");
        Files.createDirectories(directory);
        Path file = directory.resolve("sample-upload.txt");
        Files.writeString(file, "hello from generated disk file at " + Instant.now(), StandardCharsets.UTF_8);
        return file;
    }

    private void runCase(List<UploadResult> results, RemoteWriteMode mode, UploadScenario scenario, UploadAction action) {
        try {
            String remotePath = action.run();
            results.add(new UploadResult(mode, scenario, remotePath, true, false, "uploaded"));
        } catch (Exception exception) {
            log.error("Upload failed for mode={} scenario={}", mode, scenario, exception);
            results.add(new UploadResult(mode, scenario, "n/a", false, false, exception.getMessage()));
        }
    }

    private void runExpectedFailureCase(List<UploadResult> results, RemoteWriteMode mode, UploadScenario scenario, UploadAction action) {
        try {
            String remotePath = action.run();
            results.add(new UploadResult(mode, scenario, remotePath, false, false, "expected failure but upload succeeded"));
        } catch (Exception exception) {
            log.info("Expected failure observed for mode={} scenario={}: {}", mode, scenario, exception.getMessage());
            results.add(new UploadResult(mode, scenario, "same path as first upload", false, true, exception.getMessage()));
        }
    }

    private void logSummary(List<UploadResult> results) {
        long successCount = results.stream().filter(UploadResult::success).count();
        long expectedFailureCount = results.stream().filter(UploadResult::expectedFailure).count();
        long failureCount = results.size() - successCount - expectedFailureCount;

        log.info("SFTP demo summary:");
        log.info("+----------------+-----------------------------+------------------+--------------------------------------+------------------------------------------+");
        log.info("| mode           | scenario                    | status           | remotePath                           | message                                  |");
        log.info("+----------------+-----------------------------+------------------+--------------------------------------+------------------------------------------+");

        for (UploadResult result : results) {
            log.info("| {} | {} | {} | {} | {} |",
                    pad(result.mode().name(), 14),
                    pad(result.scenario().name(), 27),
                    pad(statusOf(result), 16),
                    pad(truncate(result.remotePath(), 36), 36),
                    pad(truncate(result.message(), 40), 40));
        }

        log.info("+----------------+-----------------------------+------------------+--------------------------------------+------------------------------------------+");
        log.info("Totals: success={} expectedFailure={} failure={} total={}",
                successCount, expectedFailureCount, failureCount, results.size());
    }

    private String statusOf(UploadResult result) {
        if (result.success()) {
            return "SUCCESS";
        }
        if (result.expectedFailure()) {
            return "EXPECTED_FAILURE";
        }
        return "FAILURE";
    }

    private String truncate(String value, int maxWidth) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxWidth) {
            return value;
        }
        return value.substring(0, maxWidth - 3) + "...";
    }

    private String pad(String value, int width) {
        return String.format("%-" + width + "s", value == null ? "" : value);
    }

    @FunctionalInterface
    private interface UploadAction {
        String run() throws Exception;
    }
}
