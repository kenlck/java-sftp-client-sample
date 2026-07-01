# java-sftp-client-sample

Minimal Spring Boot 3 + Java 21 + Apache MINA SSHD SFTP client POC.

## What it demonstrates

- private-key authentication from a filesystem path
- passphrase support for encrypted keys
- strict host key verification with `known_hosts`
- optional insecure demo mode
- upload from a generated local file
- upload from in-memory text bytes
- upload from in-memory binary bytes
- all three remote write modes:
  - `OVERWRITE`
  - `FAIL_IF_EXISTS`
  - `UNIQUE_NAME`

## Configuration

Edit `src/main/resources/application.yml`, use `.env.example`, or set environment variables.

```yaml
sftp:
  host: sftp.example.com
  port: 22
  username: my-user
  private-key-path: /Users/me/.ssh/id_rsa
  private-key-passphrase: ${SFTP_PRIVATE_KEY_PASSPHRASE:}
  host-key-verification: STRICT
  known-hosts-path: /Users/me/.ssh/known_hosts
  remote-directory: /upload/demo
  create-remote-directory-if-missing: true
  timeout-millis: 10000
```

### `.env.example`

A sample env file is included at `.env.example`.

```bash
cp .env.example .env
# edit values, then load them into your shell
set -a; source .env; set +a
```

### Preferred secret handling

```bash
export SFTP_PRIVATE_KEY_PASSPHRASE='your-passphrase'
export SFTP_HOST='sftp.example.com'
export SFTP_USERNAME='my-user'
export SFTP_PRIVATE_KEY_PATH="$HOME/.ssh/id_rsa"
export SFTP_KNOWN_HOSTS_PATH="$HOME/.ssh/known_hosts"
export SFTP_REMOTE_DIRECTORY='/upload/demo'
```

### Host key verification modes

#### Strict
Recommended.

```bash
export SFTP_HOST_KEY_VERIFICATION=STRICT
export SFTP_KNOWN_HOSTS_PATH="$HOME/.ssh/known_hosts"
```

#### Insecure demo mode
Accepts any host key. Only for throwaway testing.

```bash
export SFTP_HOST_KEY_VERIFICATION=INSECURE
```

## Key generation

### Recommended: OpenSSH ed25519

```bash
ssh-keygen -t ed25519 -f ~/.ssh/sftp_ed25519 -C "sftp-poc"
```

With passphrase:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/sftp_ed25519 -C "sftp-poc" -N "your-passphrase"
```

### OpenSSH RSA

```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/sftp_rsa -C "sftp-poc"
```

With passphrase:

```bash
ssh-keygen -t rsa -b 4096 -f ~/.ssh/sftp_rsa -C "sftp-poc" -N "your-passphrase"
```

### PEM / legacy RSA

```bash
ssh-keygen -t rsa -b 4096 -m PEM -f ~/.ssh/sftp_rsa_pem -C "sftp-poc"
```

With passphrase:

```bash
ssh-keygen -t rsa -b 4096 -m PEM -f ~/.ssh/sftp_rsa_pem -C "sftp-poc" -N "your-passphrase"
```

Each command creates:
- the private key file, used by this app
- the matching `.pub` public key, which you provide to the SFTP server

Example:

```bash
export SFTP_PRIVATE_KEY_PATH="$HOME/.ssh/sftp_ed25519"
export SFTP_PRIVATE_KEY_PASSPHRASE="your-passphrase"
```

## Run

```bash
mvn spring-boot:run
```

## Expected behavior

On startup the app will:

1. generate `target/generated-demo-files/sample-upload.txt`
2. upload it with `OVERWRITE`
3. upload in-memory text with `OVERWRITE`
4. upload in-memory binary with `OVERWRITE`
5. upload `fail-if-exists.txt` once with `FAIL_IF_EXISTS`
6. upload the same `fail-if-exists.txt` again and log the expected failure
7. upload disk/text/binary again with `UNIQUE_NAME`
8. print a structured final summary table of all scenarios

## Notes on key formats

The POC is intended to work with OpenSSH and PEM/RSA private keys loaded from disk. If your key is encrypted, provide the passphrase.
