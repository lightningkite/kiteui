#!/usr/bin/env bash
# by Claude - build, launch, and run integration tests for all platforms
#
# Usage:
#   ./scripts/run-integration-tests.sh [web|android|ios|all]
#
# Each platform:
#   1. Builds the app
#   2. Starts the ai-driver daemon (if not running)
#   3. Launches the app
#   4. Runs the jvmSsrTest integration tests
#   5. Cleans up
#
# The Gradle lock prevents running build tasks from within test JVMs, so this script
# separates the build and test phases into sequential Gradle invocations.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

PLATFORM="${1:-all}"
VITE_PID=""
CLEANUP_PIDS=()

cleanup() {
    echo ""
    echo "==> Cleaning up..."
    for pid in "${CLEANUP_PIDS[@]}"; do
        kill "$pid" 2>/dev/null || true
    done
    if [ -n "$VITE_PID" ]; then
        kill "$VITE_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT

ensure_daemon() {
    echo "==> Ensuring AI Driver daemon is running..."
    if curl -s -X POST http://localhost:7475/cli -d '{"command":"status"}' > /dev/null 2>&1; then
        echo "    Daemon already running."
    else
        echo "    Starting daemon..."
        ./gradlew aiDriverStart
    fi
}

wait_for_port() {
    local port=$1
    local timeout=${2:-120}
    local elapsed=0
    while [ $elapsed -lt "$timeout" ]; do
        if curl -s "http://localhost:$port" > /dev/null 2>&1; then
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    echo "ERROR: Port $port did not become available within ${timeout}s"
    return 1
}

# ---- Web/JS ----

run_web() {
    echo ""
    echo "============================================"
    echo "  Web/JS Integration Test"
    echo "============================================"

    ensure_daemon

    # Build JS first (so Gradle lock is released before test)
    echo "==> Building JS..."
    ./gradlew :example-app:jsBrowserDevelopmentWebpack --console=plain

    # Start Vite dev server in background
    echo "==> Starting Vite dev server..."
    ./gradlew :example-app:viteRun --console=plain &
    VITE_PID=$!
    CLEANUP_PIDS+=("$VITE_PID")

    echo "==> Waiting for Vite on port 3000..."
    if ! wait_for_port 3000 120; then
        echo "FAILED: Vite dev server did not start"
        return 1
    fi

    # Run integration tests (separate Gradle invocation — viteRun holds the lock,
    # but jvmSsrTest is a different compilation so it may work via daemon)
    echo "==> Running Web integration tests..."
    KITEUI_OPEN_BROWSER=true ./gradlew :example-app:jvmSsrTest --tests "*.integration.WebIntegrationTest" --console=plain || true

    # Stop Vite
    kill "$VITE_PID" 2>/dev/null || true
    VITE_PID=""
}

# ---- Android ----

run_android() {
    echo ""
    echo "============================================"
    echo "  Android Integration Test"
    echo "============================================"

    # Check prerequisites
    if ! command -v adb &> /dev/null; then
        echo "SKIPPED: adb not found. Install Android SDK platform-tools."
        return 0
    fi

    local devices
    devices=$(adb devices 2>/dev/null || true)
    if ! echo "$devices" | grep -q "device$"; then
        echo "SKIPPED: No Android device/emulator connected."
        return 0
    fi

    ensure_daemon

    # Build and install
    echo "==> Building and installing Android debug APK..."
    ./gradlew :example-app:installDebug --console=plain

    # Run integration tests
    echo "==> Running Android integration tests..."
    ./gradlew :example-app:jvmSsrTest --tests "*.integration.AndroidIntegrationTest" --console=plain || true
}

# ---- iOS ----

run_ios() {
    echo ""
    echo "============================================"
    echo "  iOS Integration Test"
    echo "============================================"

    # Check prerequisites
    if ! command -v xcrun &> /dev/null; then
        echo "SKIPPED: xcrun not found. Install Xcode command-line tools."
        return 0
    fi

    local sims
    sims=$(xcrun simctl list devices booted 2>/dev/null || true)
    if ! echo "$sims" | grep -q "(Booted)"; then
        echo "SKIPPED: No iOS Simulator booted."
        return 0
    fi

    ensure_daemon

    # Build Kotlin framework
    echo "==> Building iOS framework..."
    ./gradlew :example-app:iosSimulatorArm64MainBinaries --console=plain

    # Build iOS app with xcodebuild (if project exists)
    local IOS_PROJECT="$PROJECT_ROOT/../example-app-ios/KiteUI Example App"
    if [ -d "$IOS_PROJECT" ]; then
        echo "==> Building iOS app..."
        cd "$IOS_PROJECT"

        # Install pods if Podfile exists
        if [ -f "Podfile" ]; then
            pod install 2>/dev/null || true
        fi

        # Determine workspace or project
        local BUILD_TARGET=""
        if ls ./*.xcworkspace 1>/dev/null 2>&1; then
            local WORKSPACE
            WORKSPACE=$(ls -d ./*.xcworkspace | head -1)
            BUILD_TARGET="-workspace $WORKSPACE"
        elif ls ./*.xcodeproj 1>/dev/null 2>&1; then
            local XCODEPROJ
            XCODEPROJ=$(ls -d ./*.xcodeproj | head -1)
            BUILD_TARGET="-project $XCODEPROJ"
        else
            echo "WARNING: No Xcode project/workspace found in $IOS_PROJECT"
            cd "$PROJECT_ROOT"
            return 0
        fi

        # Build for simulator
        # shellcheck disable=SC2086
        xcodebuild $BUILD_TARGET \
            -scheme "KiteUI Example App" \
            -sdk iphonesimulator \
            -destination "platform=iOS Simulator,OS=latest,name=iPhone 16" \
            -derivedDataPath "$PROJECT_ROOT/build/ios-derived" \
            build 2>&1 | tail -5

        # Find and install the app bundle
        local APP_PATH
        APP_PATH=$(find "$PROJECT_ROOT/build/ios-derived" -name "*.app" -type d 2>/dev/null | head -1)
        if [ -n "$APP_PATH" ]; then
            echo "==> Installing on simulator: $APP_PATH"
            xcrun simctl install booted "$APP_PATH"
        else
            echo "WARNING: Could not find built .app bundle"
        fi

        cd "$PROJECT_ROOT"
    else
        echo "NOTE: iOS project not found at $IOS_PROJECT — assuming app is already installed."
    fi

    # Run integration tests
    echo "==> Running iOS integration tests..."
    ./gradlew :example-app:jvmSsrTest --tests "*.integration.IosIntegrationTest" --console=plain || true
}

# ---- Main ----

echo "KiteUI Integration Tests"
echo "Platform: $PLATFORM"
echo "Project:  $PROJECT_ROOT"
echo ""

case "$PLATFORM" in
    web)
        run_web
        ;;
    android)
        run_android
        ;;
    ios)
        run_ios
        ;;
    all)
        run_web
        run_android
        run_ios
        ;;
    *)
        echo "Usage: $0 [web|android|ios|all]"
        echo ""
        echo "Runs integration tests that build, launch, and exercise"
        echo "the KiteUI example app on the specified platform(s) through"
        echo "the ai-driver-server."
        exit 1
        ;;
esac

echo ""
echo "==> Integration tests complete."
