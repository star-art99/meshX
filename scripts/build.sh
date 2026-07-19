#!/usr/bin/env bash
# build.sh – Build MeshCore from source
# Usage: ./scripts/build.sh [--skip-tests]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "=== MeshCore Build ==="
echo "Project root: $PROJECT_ROOT"
echo ""

if [[ "${1:-}" == "--skip-tests" ]]; then
    echo "Building without tests..."
    ./gradlew build -x test
else
    echo "Building with tests..."
    ./gradlew build
fi

echo ""
echo "✓ Build complete"
echo ""
echo "To start the node:"
echo "  ./gradlew :meshcore-cli:run --args='serve'"
echo ""
echo "To initialize a new identity:"
echo "  ./gradlew :meshcore-cli:run --args='init'"
