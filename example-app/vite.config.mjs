import { defineConfig } from 'vite'

export default defineConfig({
    root: "kotlin",
    server: {
        host: true,
        port: 8000,
        allowedHosts: ["localhost:8000"],
    },
})