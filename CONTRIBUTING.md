# Contributing to MeshCore

Thank you for considering a contribution to MeshCore!

## Development Setup

### Prerequisites

- Java 17+
- Git

### Clone and build

```bash
git clone https://github.com/star-art99/meshX.git
cd meshX
./gradlew build
```

### Running tests

```bash
./gradlew test
```

## Code Style

- Kotlin code follows the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- All public APIs must have KDoc comments
- Prefer immutable data (`val`, data classes) over mutable state
- Handle errors with the `MeshError` hierarchy (not raw exceptions in public APIs)

## Branch Naming

- Feature: `feature/<short-description>`
- Bug fix: `fix/<short-description>`
- Documentation: `docs/<short-description>`

## Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(crypto): add X25519 key agreement
fix(storage): handle SQLite WAL pragma ordering
docs(api): document REST endpoints
```

## Pull Request Guidelines

1. Create a branch from `main`
2. Write tests for new functionality
3. Ensure `./gradlew build` passes
4. Reference related issues in the PR description
5. Keep PRs focused – one logical change per PR

## Security

Security issues must be reported privately. See [SECURITY.md](SECURITY.md).

## Module Boundaries

- Modules may only depend on modules lower in the dependency hierarchy
- No circular dependencies
- Business logic belongs in service modules, not in CLI/API modules

## Adding a New Module

1. Create the directory structure: `meshcore-<name>/src/main/kotlin/io/meshcore/<name>/`
2. Create `meshcore-<name>/build.gradle.kts`
3. Add to `settings.gradle.kts`
4. Add module README
5. Add unit tests

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.
