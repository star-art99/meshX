# MeshCore Architecture

## Overview

MeshCore is designed as a modular, offline-first, decentralized communication platform. Every node is equal – there is no central server or registry.

```
┌─────────────────────────────────────────────────────────────────┐
│                         meshcore-cli                            │
│                      (Clikt CLI tool)                           │
└─────────────────────┬──────────────────────────────────────────┘
                      │ uses
┌─────────────────────▼──────────────────────────────────────────┐
│                         meshcore-api                            │
│                   (Ktor REST server)                            │
└──────────┬──────────────────────────────────────────┬──────────┘
           │ uses                                      │ uses
┌──────────▼──────────┐                  ┌────────────▼──────────┐
│  meshcore-messenger │                  │   meshcore-files       │
│  (E2E messaging)    │                  │  (Chunked transfer)    │
└──────────┬──────────┘                  └────────────┬──────────┘
           │                                          │
           └─────────────────┬────────────────────────┘
                             │ uses
┌────────────────────────────▼────────────────────────────────────┐
│                       meshcore-network                           │
│              Transport abstraction (TCP / UDP / ...)             │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                      meshcore-discovery                          │
│           UDP Broadcast + mDNS peer discovery                    │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                       meshcore-routing                           │
│              Routing table + multi-path selection                │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                       meshcore-crypto                            │
│     Ed25519 / X25519 / ChaCha20-Poly1305 / HKDF / KeyStore      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                       meshcore-storage                           │
│           SQLite + Migration + Peer/Message Repositories         │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                        meshcore-core                             │
│  Lifecycle · Config · Logging · EventBus · Error Model           │
└─────────────────────────────────────────────────────────────────┘
```

## Module Responsibilities

### meshcore-core
Foundation layer. No external dependencies except SLF4J and kotlinx-coroutines.

- **NodeLifecycle**: manages startup/shutdown order, provides CoroutineScope
- **MeshConfig**: loads configuration from file + environment overrides
- **MeshLogger**: SLF4J façade with structured log levels
- **EventBus**: coroutine-based SharedFlow event dispatcher
- **MeshError**: typed domain error hierarchy

### meshcore-crypto
All cryptographic operations. Depends only on BouncyCastle.

- **Ed25519Identity**: key generation, signing, verification
- **X25519KeyPair**: Diffie-Hellman key agreement for session keys
- **ChaCha20Poly1305Aead**: AEAD encryption with random nonces
- **Hkdf**: key derivation from shared secrets
- **FileKeyStore**: encrypted local key storage

**Security rule**: Private keys never leave the device; never appear in logs or API responses.

### meshcore-storage
Local data persistence. No network access.

- **SqliteDatabase**: connection management with WAL mode
- **MigrationRunner**: versioned schema migrations
- **PeerRepository**: CRUD for known peers
- **MessageRepository**: encrypted message metadata storage

### meshcore-network
Transport abstraction. No business logic.

- **Transport**: interface defining send/receive/start/stop
- **TcpTransport**: length-prefixed framing over TCP
- **UdpTransport**: UDP datagrams with broadcast support

### meshcore-discovery
Peer discovery mechanisms.

- **UdpBroadcastDiscovery**: periodic beacon on LAN broadcast
- mDNS: TODO (RFC 6762 + DNS-SD RFC 6763)

### meshcore-routing
Mesh routing decisions.

- **MeshRouter**: routing table and path selection
- TODO: OLSR/Babel-style link-state or distance-vector updates

### meshcore-api
REST API using Ktor.

- `GET /health` – health check
- `GET /api/v1/node` – node information
- `GET /api/v1/peers` – known peers
- `GET /api/v1/peers/{id}` – peer details
- `POST /api/v1/messages/send` – send message

### meshcore-cli
Command-line interface using Clikt.

- `meshcore init` – generate node identity and config
- `meshcore serve` – start the node
- `meshcore status` – show node status
- `meshcore peers` – list known peers
- `meshcore send <peer> <message>` – send a message

## Security Model

### Cryptographic Primitives

| Purpose | Algorithm | Rationale |
|---------|-----------|-----------|
| Identity | Ed25519 | Fast, secure, compact 32-byte keys |
| Key Agreement | X25519 | Elliptic-curve DH, forward secrecy base |
| Session Keys | HKDF-SHA256 | Standard key derivation |
| Encryption | ChaCha20-Poly1305 | AEAD, resistant to timing attacks |
| Hashing | SHA-256 | File integrity, content addressing |

### Threat Model

**Assumptions:**
- The local device is trusted (key material stored locally)
- The network is untrusted (all messages encrypted)
- Peers may be adversarial until authenticated

**Protections:**
- All message payloads are AEAD-encrypted
- Each encryption uses a random 12-byte nonce (no nonce reuse)
- Identity is a key pair – no username/password/email
- Private keys never leave the device unencrypted

**Known limitations (v0.1.0):**
- Database not encrypted at rest (SQLCipher integration TODO)
- Key derivation uses HKDF, not Argon2id for passphrase wrapping
- No Double Ratchet (forward secrecy per-message) yet
- No replay protection window implemented yet

## Dependency Rules

Modules may ONLY depend on modules below them in the layering:

```
meshcore-{cli,web} → meshcore-api → meshcore-{network,discovery,storage,crypto} → meshcore-core
```

Circular dependencies between modules are forbidden.

## Local Development Setup

### Linux (Ubuntu/Debian/Arch)

```bash
# Ensure Java 17
sudo apt install openjdk-17-jdk   # Debian/Ubuntu
# or
sudo pacman -S jdk17-openjdk      # Arch

git clone https://github.com/star-art99/meshX.git
cd meshX
./gradlew build

# Run CLI
./gradlew :meshcore-cli:run --args="--help"
```

### Termux (Android)

```bash
pkg update && pkg upgrade
pkg install openjdk-17 git

git clone https://github.com/star-art99/meshX.git
cd meshX
./gradlew build

# Note: javax.sound is unavailable on Termux.
# Voice/video features are disabled on this platform.
```

## Running Tests

```bash
./gradlew test
```

Test reports: `*/build/reports/tests/test/index.html`
