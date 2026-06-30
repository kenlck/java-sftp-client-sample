package com.example.sftp;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.KnownHostsServerKeyVerifier;
import org.apache.sshd.client.keyverifier.RejectAllServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.io.resource.PathResource;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.EnumSet;

@Service
public class SftpUploadService {

    private final SftpProperties properties;
    private final RemotePathResolver remotePathResolver;

    public SftpUploadService(SftpProperties properties) {
        this(properties, new RemotePathResolver(java.time.Clock.systemUTC()));
    }

    SftpUploadService(SftpProperties properties, RemotePathResolver remotePathResolver) {
        this.properties = properties;
        this.remotePathResolver = remotePathResolver;
    }

    public String uploadFile(Path localFile, String fileName, RemoteWriteMode mode) throws IOException, GeneralSecurityException {
        try (InputStream inputStream = Files.newInputStream(localFile)) {
            return uploadStream(inputStream, fileName, mode);
        }
    }

    public String uploadStream(InputStream content, String fileName, RemoteWriteMode mode) throws IOException, GeneralSecurityException {
        String remotePath = remotePathResolver.resolve(properties.getRemoteDirectory(), fileName, mode);

        try (SshClient client = createClient()) {
            client.start();

            try (ClientSession session = client.connect(properties.getUsername(), properties.getHost(), properties.getPort())
                    .verify(properties.getTimeoutMillis())
                    .getSession()) {
                addPrivateKeyIdentity(session);
                session.auth().verify(properties.getTimeoutMillis());

                try (SftpClient sftpClient = SftpClientFactory.instance().createSftpClient(session)) {
                    if (properties.isCreateRemoteDirectoryIfMissing()) {
                        ensureRemoteDirectoryExists(sftpClient, properties.getRemoteDirectory());
                    }

                    if (mode == RemoteWriteMode.FAIL_IF_EXISTS && exists(sftpClient, remotePath)) {
                        throw new IllegalStateException("Remote file already exists: " + remotePath);
                    }

                    writeContent(sftpClient, remotePath, content, mode);
                    return remotePath;
                }
            }
        }
    }

    private SshClient createClient() {
        SshClient client = SshClient.setUpDefaultClient();
        if (properties.getHostKeyVerification() == SftpProperties.HostKeyVerification.INSECURE) {
            client.setServerKeyVerifier(AcceptAllServerKeyVerifier.INSTANCE);
            return client;
        }

        if (!StringUtils.hasText(properties.getKnownHostsPath())) {
            throw new IllegalStateException("knownHostsPath is required when host key verification is STRICT");
        }

        client.setServerKeyVerifier(new KnownHostsServerKeyVerifier(
                RejectAllServerKeyVerifier.INSTANCE,
                Path.of(properties.getKnownHostsPath())
        ));
        return client;
    }

    private void addPrivateKeyIdentity(ClientSession session) throws IOException, GeneralSecurityException {
        Path keyPath = Path.of(properties.getPrivateKeyPath());
        FilePasswordProvider passwordProvider = StringUtils.hasText(properties.getPrivateKeyPassphrase())
                ? FilePasswordProvider.of(properties.getPrivateKeyPassphrase())
                : FilePasswordProvider.EMPTY;

        NamedResource resource = new PathResource(keyPath);
        try (InputStream inputStream = Files.newInputStream(keyPath)) {
            Iterable<KeyPair> keyPairs = SecurityUtils.loadKeyPairIdentities(
                    session,
                    resource,
                    inputStream,
                    passwordProvider
            );

            if (keyPairs == null) {
                throw new IllegalStateException("No private keys could be loaded from " + keyPath);
            }

            for (KeyPair keyPair : keyPairs) {
                session.addPublicKeyIdentity(keyPair);
            }
        }
    }

    private void ensureRemoteDirectoryExists(SftpClient client, String remoteDirectory) throws IOException {
        String normalized = RemotePathResolver.normalizeDirectory(remoteDirectory);
        if (".".equals(normalized) || "/".equals(normalized)) {
            return;
        }

        String[] parts = normalized.split("/");
        String current = normalized.startsWith("/") ? "/" : "";
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            current = current.isEmpty() || current.equals("/") ? current + part : current + "/" + part;
            if (!exists(client, current)) {
                client.mkdir(current);
            }
        }
    }

    private boolean exists(SftpClient client, String path) {
        try {
            client.stat(path);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private void writeContent(SftpClient client, String remotePath, InputStream content, RemoteWriteMode mode) throws IOException {
        EnumSet<SftpClient.OpenMode> openModes = EnumSet.of(SftpClient.OpenMode.Write, SftpClient.OpenMode.Create);
        if (mode == RemoteWriteMode.OVERWRITE || mode == RemoteWriteMode.FAIL_IF_EXISTS) {
            openModes.add(SftpClient.OpenMode.Truncate);
        }

        try (OutputStream outputStream = client.write(remotePath, openModes)) {
            content.transferTo(outputStream);
        }
    }
}
