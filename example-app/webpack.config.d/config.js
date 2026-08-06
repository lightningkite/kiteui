if (config.devServer) {
    config.devServer.historyApiFallback = true // route all pages to index.html for development
    config.devServer.port = 9000
    // Opt-in, same as vite.config.mjs - see the longer explanation there. COEP applies to nested
    // documents, so leaving this on unconditionally made every cross-origin iframe on
    // /webview-permissions fail with NS_ERROR_DOM_COEP_FAILED. require-corp is stricter still than
    // vite's credentialless: it also blocks cross-origin images and media that carry no CORP
    // header, which is what took out the autoplay probe's video.
    if (process.env.KITEUI_CROSS_ORIGIN_ISOLATED === "1") {
        config.devServer.headers = {
            "Cross-Origin-Opener-Policy": "same-origin",
            "Cross-Origin-Embedder-Policy": "require-corp",
        }
    }
}

const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;

config.plugins.push(
    new BundleAnalyzerPlugin({
        analyzerMode: 'static',
        openAnalyzer: false,
        generateStatsFile: true,
    }),
)