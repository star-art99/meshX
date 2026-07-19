# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 0.1.x   | ✅ Yes    |

## Reporting a Vulnerability

**Please do NOT report security vulnerabilities via public GitHub issues.**

To report a security vulnerability, please open a **private security advisory** on GitHub:

1. Go to the repository's **Security** tab
2. Click **"Report a vulnerability"**
3. Fill in the details

You will receive a response within 48 hours. We will work with you to understand and address the issue before any public disclosure.

## Security Architecture

### Cryptographic Choices

| Primitive | Algorithm | Notes |
|-----------|-----------|-------|
| Identity | Ed25519 | RFC 8032, 256-bit security |
| Key Agreement | X25519 | RFC 7748, ephemeral DH |
| Key Derivation | HKDF-SHA256 | RFC 5869 |
| Encryption | ChaCha20-Poly1305 | RFC 7539, AEAD |
| Hashing | SHA-256 / SHA-512 | NIST standard |

All cryptographic operations use **BouncyCastle** (org.bouncycastle:bcprov-jdk18on).

### Key Security Properties

- **Private keys never leave the device** – there is no key escrow or backup server
- **No nonce reuse** – ChaCha20-Poly1305 uses a fresh random 12-byte nonce per encryption
- **No plaintext secrets in logs** – private keys are filtered from all log output
- **API does not expose private keys** – only public keys are visible in API responses

### Known Limitations (v0.1.0)

- **Database not encrypted at rest** – SQLite is unencrypted (SQLCipher integration is TODO)
- **No replay protection** – timestamp window not yet implemented
- **Passphrase key wrapping** – HKDF is used instead of Argon2id/scrypt for passphrase-based key wrapping
- **No Double Ratchet** – forward secrecy per message not yet implemented
- **Node discovery is unauthenticated** – UDP beacon packets are not signed (authentication happens during handshake)

## Security Best Practices for Deployment

1. Run the node on a trusted device
2. Use firewall rules to restrict API access to localhost (`127.0.0.1` only)
3. Regularly rotate node identity keys
4. Keep the software up-to-date
5. Review `~/.meshcore/` permissions (should be readable only by the owning user)
