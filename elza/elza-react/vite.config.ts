import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  resolve: {
    alias: {
      "src": path.resolve(__dirname, "./src/"),
      "stores": path.resolve(__dirname, "./src/stores/"),
      "components": path.resolve(__dirname, "./src/components/"),
      "actions": path.resolve(__dirname, "./src/actions/"),
      "pages": path.resolve(__dirname, "./src/pages/"),
      "api": path.resolve(__dirname, "./src/api/"),
      "utils": path.resolve(__dirname, "./src/utils/"),
      "typings": path.resolve(__dirname, "./src/typings/"),
      "shared": path.resolve(__dirname, "./src/shared/"),
      '~bootstrap': path.resolve(__dirname, 'node_modules/bootstrap'),
    }
  },
  plugins: [react()],
  server: {
    port: 3000,
    hmr: {
      port: 3001
    },
    proxy: {
      // string shorthand: http://localhost:5173/foo -> http://localhost:4567/foo
      //'/api', '/login'
      '/login': 'http://10.2.0.27:8081',
      '/api': {
        target: 'http://10.2.0.27:8081',
        changeOrigin: true,
        // rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/stomp': {
        target: 'http://10.2.0.27:8081',
        changeOrigin: true,
        ws: true,
        // rewrite: (path) => path.replace(/^\/api/, ''),
      }
    },
  },
})
