import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development the backend runs on :8080 and Vite proxies /api to it, so the browser sees one origin.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        // The browser sees one origin; drop the Origin header so the backend's CORS list (dev port 5173 only)
        // does not reject requests from other local ports.
        configure: (proxy) => proxy.on('proxyReq', (req) => req.removeHeader('origin')),
      },
    },
  },
  build: {
    outDir: 'dist',
    chunkSizeWarningLimit: 1200,
  },
});
