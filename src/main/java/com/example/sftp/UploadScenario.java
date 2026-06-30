package com.example.sftp;

public enum UploadScenario {
    DISK_FILE,
    IN_MEMORY_TEXT,
    IN_MEMORY_BINARY,
    FAIL_IF_EXISTS_FIRST_UPLOAD,
    FAIL_IF_EXISTS_SECOND_UPLOAD
}
