import { defineConfig } from 'vite'

export default defineConfig({
    root: "kotlin",
    server: {
        host: true,
        port: 8000,
        allowedHosts: ["localhost:8000"],
        // Enables crossOriginIsolated so performance.measureUserAgentSpecificMemory()
        // is available for GC-accurate, per-type memory measurement (leak investigation).
        headers: {
            "Cross-Origin-Opener-Policy": "same-origin",
            "Cross-Origin-Embedder-Policy": "require-corp",
        },
    },
})