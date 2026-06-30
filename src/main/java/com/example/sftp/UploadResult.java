package com.example.sftp;

public record UploadResult(
        RemoteWriteMode mode,
        UploadScenario scenario,
        String remotePath,
        boolean success,
        boolean expectedFailure,
        String message
) {
}
