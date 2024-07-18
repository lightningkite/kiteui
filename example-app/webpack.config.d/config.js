if (config.devServer) {
    config.devServer.historyApiFallback = true // route all pages to index.html for development
    config.devServer.port = 8081
}