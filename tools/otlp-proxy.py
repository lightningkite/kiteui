#!/usr/bin/env python3
"""
by Claude - Minimal OTLP proxy for testing browser telemetry.
Accepts OTLP JSON POSTs on localhost:8099/v1/{traces,metrics,logs}
and forwards them to an upstream OTLP endpoint with auth headers.

Usage:
    python3 tools/otlp-proxy.py

Environment variables:
    OTLP_UPSTREAM   - Upstream OTLP endpoint (default: https://otlp-gateway-prod-us-west-0.grafana.net/otlp)
    OTLP_AUTH       - Authorization header value (e.g., "Basic MTU0OD...")

The proxy adds CORS headers so browsers can POST directly to it.
"""
import os
import sys
import json
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.request import Request, urlopen
from urllib.error import URLError, HTTPError

UPSTREAM = os.environ.get("OTLP_UPSTREAM", "").rstrip("/")
AUTH = os.environ.get("OTLP_AUTH", "")
PORT = int(os.environ.get("OTLP_PROXY_PORT", "8099"))

VALID_SIGNALS = {"/v1/traces", "/v1/metrics", "/v1/logs"}


class OtlpProxyHandler(BaseHTTPRequestHandler):
    def do_OPTIONS(self):
        """Handle CORS preflight."""
        self.send_response(204)
        self._cors_headers()
        self.end_headers()

    def do_POST(self):
        if self.path not in VALID_SIGNALS:
            self.send_response(404)
            self.end_headers()
            return

        if not UPSTREAM:
            self.send_response(501)
            self._cors_headers()
            self.end_headers()
            self.wfile.write(b"OTLP_UPSTREAM not configured")
            return

        content_length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(content_length)

        upstream_url = f"{UPSTREAM}{self.path}"
        req = Request(upstream_url, data=body, method="POST")
        req.add_header("Content-Type", "application/json")
        if AUTH:
            req.add_header("Authorization", AUTH)

        try:
            resp = urlopen(req)
            status = resp.getcode()
            print(f"  -> {self.path} => {status}")
        except HTTPError as e:
            status = e.code
            resp_body = e.read().decode("utf-8", errors="replace")
            print(f"  -> {self.path} => {status}: {resp_body[:200]}")
        except URLError as e:
            status = 502
            print(f"  -> {self.path} => network error: {e.reason}")

        self.send_response(status)
        self._cors_headers()
        self.end_headers()

    def _cors_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")

    def log_message(self, format, *args):
        # Quieter logging
        pass


def main():
    if not UPSTREAM:
        print("ERROR: Set OTLP_UPSTREAM environment variable", file=sys.stderr)
        print("  e.g.: OTLP_UPSTREAM=https://otlp-gateway-prod-us-west-0.grafana.net/otlp", file=sys.stderr)
        sys.exit(1)
    if not AUTH:
        print("WARNING: OTLP_AUTH not set — requests will be sent without Authorization header", file=sys.stderr)

    server = HTTPServer(("127.0.0.1", PORT), OtlpProxyHandler)
    print(f"OTLP proxy listening on http://127.0.0.1:{PORT}")
    print(f"  Upstream: {UPSTREAM}")
    print(f"  Auth: {'configured' if AUTH else 'NONE'}")
    print(f"  Signals: {', '.join(sorted(VALID_SIGNALS))}")
    print()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping.")
        server.server_close()


if __name__ == "__main__":
    main()
