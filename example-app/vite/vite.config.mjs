import { defineConfig } from 'vite'

// Cross-origin isolation is opt-in:
//   KITEUI_CROSS_ORIGIN_ISOLATED=1 ./gradlew :example-app:jsViteDev
//
// Setting it enables performance.measureUserAgentSpecificMemory() for GC-accurate, per-type memory
// measurement during a leak investigation. It is off by default because COEP applies to nested
// documents as well as subresources: with it on, every cross-origin iframe fails to load with
// NS_ERROR_DOM_COEP_FAILED, which makes /webview-permissions untestable - the third-party pages its
// camera, microphone, popup and modal probes embed simply never appear. Nothing in the app calls
// the memory API at runtime, and production serves no such headers, so off is also the setting that
// matches what ships.
const crossOriginIsolated = process.env.KITEUI_CROSS_ORIGIN_ISOLATED === '1'

export default defineConfig({
    root: "kotlin",
    server: {
        host: true,
        port: 5173,
        allowedHosts: ["localhost:5173"],
        headers: crossOriginIsolated
            ? {
                "Cross-Origin-Opener-Policy": "same-origin",
                // "credentialless" rather than "require-corp" so that cross-origin images without a
                // CORP header (the picsum.photos ones the example pages use) still load.
                "Cross-Origin-Embedder-Policy": "credentialless",
            }
            : {},
    },
})
