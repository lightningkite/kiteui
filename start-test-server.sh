#!/usr/bin/env bash
# Starts the local echo server that KiteUI's cross-platform network tests need
# (TestServerPort.kt). Under Gradle this was a build service; under the Kotlin
# Toolchain the server is a normal module, started by hand before `kotlin test -p jvm`.
#
# Idempotent: if something already listens on the fixed port, this is a no-op, so
# running it twice -- or once after the chain already started it -- is safe.
#
# Usage:
#   ./start-test-server.sh          # start if needed, print PID file path
#   ./start-test-server.sh --stop   # stop a server this script started
set -uo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
PORT=18787
PID_FILE="${TMPDIR:-/tmp}/kiteui-test-server.pid"

port_has_listener() {
    nc -z 127.0.0.1 "$PORT" 2>/dev/null
}

if [ "${1:-}" = "--stop" ]; then
    # The recorded pid is the `sh -c` wrapper, not the JVM it spawned (nohup's $!),
    # so also kill whatever actually listens on the port.
    [ -f "$PID_FILE" ] && kill "$(cat "$PID_FILE")" 2>/dev/null && rm -f "$PID_FILE"
    listener=$(lsof -tiTCP:"$PORT" -sTCP:LISTEN 2>/dev/null)
    [ -n "$listener" ] && kill $listener 2>/dev/null
    if port_has_listener; then
        echo "could not stop test-server on $PORT" >&2
        exit 1
    fi
    echo "stopped test-server"
    exit 0
fi

if port_has_listener; then
    echo "test-server already listening on $PORT"
    [ -f "$PID_FILE" ] && echo "pid file: $PID_FILE"
    exit 0
fi

# stdin must stay open or the server exits the moment it starts (it reads stdin
# to EOF before shutting down) -- hence the pipe.
nohup sh -c "tail -f /dev/null | '$HERE/kotlin' run -m test-server $PORT" \
    >/tmp/kiteui-test-server.log 2>&1 &
echo $! >"$PID_FILE"

# Wait for LISTENING rather than sleeping, so we don't race the first tests.
for _ in $(seq 1 60); do
    port_has_listener && break
    sleep 0.5
done

if port_has_listener; then
    echo "test-server listening on $PORT (pid $(cat "$PID_FILE"))"
else
    echo "test-server did not come up; see /tmp/kiteui-test-server.log" >&2
    exit 1
fi