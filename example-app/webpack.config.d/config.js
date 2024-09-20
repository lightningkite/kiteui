if (config.devServer) {
    config.devServer.historyApiFallback = true // route all pages to index.html for development
    config.devServer.port = 8086
    config.devServer.headers = {
        "Cross-Origin-Opener-Policy": "same-origin",
        "Cross-Origin-Embedder-Policy": "require-corp",
    }
}