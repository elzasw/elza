import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path';

// https://vitejs.dev/config/

const defaultEndpoint = 'http://localhost:8080';

export default ({ mode }) => {

  // Load environment variables
  process.env = { ...process.env, ...loadEnv(mode, process.cwd(), "") }
  const endpoint = process.env.ENDPOINT || defaultEndpoint;

  return defineConfig({
    // Make paths relative
    base: "./",
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
    define: {'process.env': process.env},
    plugins: [react()],
    build: {
      sourcemap: true,
      rollupOptions: {
        output: {
          entryFileNames: "static/res/js/[name].js",
          assetFileNames: "static/res/assets/[name][extname]",
          chunkFileNames: "static/res/js/[name].js"
        }
      }
    },
    server: {
      port: 3000,
      hmr: {
        port: 3001
      },
      proxy: {
        '/login': endpoint,
        '/api': {
          target: endpoint,
          changeOrigin: true,
        },
        '/stomp': {
          target: endpoint,
          changeOrigin: true,
          ws: true,
        }
      },
    },
  })
}
