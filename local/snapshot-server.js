#!/usr/bin/env node

/**
 * Standalone HTTP server for saving HTML snapshots from browser tests.
 *
 * Usage:
 *   node local/snapshot-server.js
 *
 * Then run tests in another terminal:
 *   ./gradlew :library:jsTest
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3001;
const OUTPUT_DIR = path.join(__dirname, 'screenshots', 'js');

// Create output directory
fs.mkdirSync(OUTPUT_DIR, { recursive: true });

const server = http.createServer((req, res) => {
  // Enable CORS for all origins
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  // Handle preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    res.end();
    return;
  }

  // Only handle POST to /save-snapshot
  if (req.method !== 'POST' || req.url !== '/save-snapshot') {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
    return;
  }

  let body = '';

  req.on('data', chunk => {
    body += chunk.toString();
  });

  req.on('end', () => {
    try {
      const data = JSON.parse(body);
      const { name, html } = data;

      if (!name || !html) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Missing name or html' }));
        return;
      }

      // Save HTML file
      const filename = `${name}.html`;
      const filepath = path.join(OUTPUT_DIR, filename);
      fs.writeFileSync(filepath, html, 'utf8');

      console.log(`✅ Saved: ${filename} (${html.length} chars)`);

      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        success: true,
        path: filepath,
        size: html.length
      }));

    } catch (error) {
      console.error('❌ Error:', error.message);
      res.writeHead(500, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: error.message }));
    }
  });
});

server.listen(PORT, () => {
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log('📸 KiteUI Snapshot Server');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
  console.log(`🌐 Listening on http://localhost:${PORT}`);
  console.log(`📁 Saving snapshots to: ${OUTPUT_DIR}`);
  console.log('');
  console.log('Ready to receive snapshots from tests...');
  console.log('Press Ctrl+C to stop');
  console.log('━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━');
});

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n\n👋 Shutting down snapshot server...');
  server.close(() => {
    console.log('✅ Server stopped');
    process.exit(0);
  });
});
