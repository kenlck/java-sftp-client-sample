package com.example.sftp;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "sftp")
@Validated
public class SftpProperties {

    @NotBlank
    private String host;

    @Min(1)
    @Max(65535)
    private int port = 22;

    @NotBlank
    private String username;

    @NotBlank
    private String privateKeyPath;

    private String privateKeyPassphrase;

    private HostKeyVerification hostKeyVerification = HostKeyVerification.STRICT;

    private String knownHostsPath;

    @NotBlank
    private String remoteDirectory;

    private boolean createRemoteDirectoryIfMissing = true;

    private long timeoutMillis = 10_000;

    public enum HostKeyVerification {
        STRICT,
        INSECURE
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

    public HostKeyVerification getHostKeyVerification() {
        return hostKeyVerification;
    }

    public void setHostKeyVerification(HostKeyVerification hostKeyVerification) {
        this.hostKeyVerification = hostKeyVerification;
    }

    public String getKnownHostsPath() {
        return knownHostsPath;
    }

    public void setKnownHostsPath(String knownHostsPath) {
        this.knownHostsPath = knownHostsPath;
    }

    public String getRemoteDirectory() {
        return remoteDirectory;
    }

    public void setRemoteDirectory(String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
    }

    public boolean isCreateRemoteDirectoryIfMissing() {
        return createRemoteDirectoryIfMissing;
    }

    public void setCreateRemoteDirectoryIfMissing(boolean createRemoteDirectoryIfMissing) {
        this.createRemoteDirectoryIfMissing = createRemoteDirectoryIfMissing;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
}
