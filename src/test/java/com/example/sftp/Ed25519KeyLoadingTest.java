package com.example.sftp;

import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.io.resource.PathResource;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Ed25519KeyLoadingTest {

    @Test
    void shouldLoadOpenSshEd25519PrivateKey() throws Exception {
        Path keyPath = Path.of("..", "poc", "client", "alice_ed25519").normalize();
        NamedResource resource = new PathResource(keyPath);

        List<KeyPair> keyPairs = new ArrayList<>();
        try (InputStream inputStream = Files.newInputStream(keyPath)) {
            for (KeyPair keyPair : SecurityUtils.loadKeyPairIdentities(
                    null,
                    resource,
                    inputStream,
                    FilePasswordProvider.EMPTY)) {
                keyPairs.add(keyPair);
            }
        }

        assertThat(keyPairs).hasSize(1);
        assertThat(keyPairs.getFirst().getPrivate().getAlgorithm()).containsIgnoringCase("Ed");
    }
}
