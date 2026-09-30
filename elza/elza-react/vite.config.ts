import { defineConfig, loadEnv, Plugin, ViteDevServer } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path';

// https://vitejs.dev/config/

const defaultEndpoint = 'http://localhost:8080';
// Only the SSO endpoint, so the dev session is established the same way as in production:
// by the sign-in itself, not by whatever API call happens to go first.
const proxiedPaths = ['/authenticate'];

// Mints a fresh SPNEGO token per request from the local ticket cache (kinit) and
// forwards it to the backend. Tokens are single-use - servers keep a replay cache.
const createSpnegoPlugin = (kerberosService: string): Plugin => ({
    name: 'spnego-proxy-auth',
    configureServer(server: ViteDevServer) {
        server.middlewares.use(async (request, _response, next) => {
            const isProxiedRequest = proxiedPaths.some(proxiedPath => request.url?.startsWith(proxiedPath));
            if (!isProxiedRequest) {
                next();
                return;
            }

            try {
                const kerberos = await import('kerberos');
                const client = await kerberos.initializeClient(kerberosService);
                request.headers.authorization = `Negotiate ${await client.step('')}`;
            } catch (error) {
                console.error(`[spnego] no token for ${kerberosService}:`, (error as Error).message);
            }

            next();
        });
    },
});

export default ({ mode }) => {

  // Load environment variables
  process.env = { ...process.env, ...loadEnv(mode, process.cwd(), "") }
  const endpoint = process.env.ENDPOINT || defaultEndpoint;
  const backendHostname = new URL(endpoint).hostname;
  // Kerberos has no service principal for a bare IP, so SPNEGO only applies to named hosts.
  const isNamedHost = !/^\d+\.\d+\.\d+\.\d+$/.test(backendHostname);
  const isSpnegoEnabled = process.env.KERBEROS === 'true' && isNamedHost;
  // GSSAPI expects HTTP@host, SSPI on Windows expects HTTP/host.
  const kerberosService = process.env.KERBEROS_SERVICE || `HTTP@${backendHostname}`;

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
        "contexts": path.resolve(__dirname, "./src/contexts/"),
        "shared": path.resolve(__dirname, "./src/shared/"),
        '~bootstrap': path.resolve(__dirname, 'node_modules/bootstrap'),
      }
    },
    define: {'process.env': process.env},
    plugins: [react(), ...(isSpnegoEnabled ? [createSpnegoPlugin(kerberosService)] : [])],
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
        '/logout': {
          target: endpoint,
          changeOrigin: true,
        },
        '/authenticate': {
          target: endpoint,
          changeOrigin: true,
        },
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
