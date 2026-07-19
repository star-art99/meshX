# MeshCore

**MeshCore** is a production-grade, modular, offline-first decentralized communication platform for Linux and Termux (Android).

Every node is simultaneously a client, server, relay, and router. The network functions without the internet.

## Features

- 🔐 **End-to-end encryption** – Ed25519 identity, X25519 key agreement, ChaCha20-Poly1305 AEAD
- 📡 **Offline-first mesh networking** – TCP/UDP transport with automatic peer discovery
- 🗄️ **Local SQLite database** – encrypted-at-rest storage with schema migrations
- 🌐 **REST API** – Ktor-backed API for external integration
- 💻 **Full CLI** – all features accessible from the command line
- 🔌 **Plugin system** – extensible capability-based architecture

## Quick Start

### Prerequisites

- Java 17+
- Linux or Termux (Android)

### Build

```bash
./gradlew build
```

### Initialize a node

```bash
./gradlew :meshcore-cli:run --args="init"
```

### Start the node

```bash
./gradlew :meshcore-cli:run --args="serve"
```

### Check status

```bash
./gradlew :meshcore-cli:run --args="status"
```

### List peers

```bash
./gradlew :meshcore-cli:run --args="peers"
```

### Send a test message

```bash
./gradlew :meshcore-cli:run --args="send <peer-id> 'Hello!'"
```

## Architecture

See [docs/architecture.md](docs/architecture.md) for a full architecture overview.

## Modules

| Module | Description |
|--------|-------------|
| `meshcore-core` | Lifecycle, config, logging, event bus, error model |
| `meshcore-crypto` | Ed25519 identity, X25519 key agreement, ChaCha20-Poly1305 AEAD, HKDF |
| `meshcore-storage` | SQLite database, migrations, peer/message repositories |
| `meshcore-network` | Transport abstraction, TCP + UDP adapters |
| `meshcore-discovery` | UDP broadcast peer discovery, mDNS scaffold |
| `meshcore-routing` | Mesh routing table, multi-path skeleton |
| `meshcore-sync` | Data synchronization (scaffold) |
| `meshcore-api` | REST API server (Ktor/Netty) |
| `meshcore-cli` | CLI tool (Clikt) |
| `meshcore-web` | Local web dashboard (scaffold) |
| `meshcore-plugin` | Plugin system with capability enforcement |
| `meshcore-messenger` | E2E messaging service |
| `meshcore-files` | Chunked file transfer with integrity checks |
| `meshcore-voice` | Voice calls (scaffold) |
| `meshcore-video` | Video calls (scaffold) |

## Security

See [SECURITY.md](SECURITY.md) for the security model and vulnerability reporting.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development guidelines.

## License

Apache License 2.0 – see [LICENSE](LICENSE).
