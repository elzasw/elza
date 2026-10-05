import { defineConfig, loadEnv, Plugin, ViteDevServer } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path';

// https://vitejs.dev/config/

const defaultEndpoint = 'http://localhost:8080';
const ssoPaths = ['/authenticate'];
// Set by the connection rather than by the sender, so they are not passed on.
const hopByHopHeaders = ['connection', 'keep-alive', 'transfer-encoding', 'upgrade', 'host'];
// The response body arrives decoded and of a different length than the backend announced.
const rewrittenResponseHeaders = ['set-cookie', 'content-encoding', 'content-length'];

const toForwardedHeaders = (incomingHeaders: NodeJS.Dict<string | string[]>): Record<string, string> => {
    const forwardedHeaders: Record<string, string> = {};
    for (const [name, value] of Object.entries(incomingHeaders)) {
        if (value === undefined || hopByHopHeaders.includes(name)) {
            continue;
        }
        forwardedHeaders[name] = Array.isArray(value) ? value.join('; ') : value;
    }
    return forwardedHeaders;
};

const toCookieHeader = (setCookieHeaders: string[]) =>
    setCookieHeaders.map(setCookieHeader => setCookieHeader.split(';')[0]).join('; ');

// Answers the SPNEGO challenge for the browser, which cannot: it derives the service principal
// from the host in the address bar, and no SPN exists for a development machine. The request is
// forwarded as sent and only the Authorization header is added to the retry, so the exchange runs
// as it does in production. The session the challenge opened travels back with the response.
// Tokens are single-use, so each challenge gets a fresh one - servers keep a replay cache.
const createSpnegoPlugin = (endpoint: string, kerberosService: string): Plugin => ({
    name: 'spnego-proxy-auth',
    configureServer(server: ViteDevServer) {
        server.middlewares.use(async (request, response, next) => {
            const isSsoRequest = ssoPaths.some(ssoPath => request.url?.startsWith(ssoPath));
            if (!isSsoRequest || request.method !== 'GET') {
                next();
                return;
            }

            const targetUrl = `${endpoint}${request.url}`;
            const forwardedHeaders = toForwardedHeaders(request.headers);

            try {
                let backendResponse = await fetch(targetUrl, { headers: forwardedHeaders, redirect: 'manual' });
                const challengeHeader = backendResponse.headers.get('www-authenticate') ?? '';
                const isNegotiateChallenge = backendResponse.status === 401 && challengeHeader.includes('Negotiate');
                let sessionCookies: string[] = [];

                if (isNegotiateChallenge) {
                    sessionCookies = backendResponse.headers.getSetCookie();
                    const kerberos = await import('kerberos');
                    const client = await kerberos.initializeClient(kerberosService);
                    backendResponse = await fetch(targetUrl, {
                        redirect: 'manual',
                        headers: {
                            ...forwardedHeaders,
                            ...(sessionCookies.length > 0 ? { cookie: toCookieHeader(sessionCookies) } : {}),
                            authorization: `Negotiate ${await client.step('')}`,
                        },
                    });
                }

                response.statusCode = backendResponse.status;
                for (const [name, value] of backendResponse.headers) {
                    if (!rewrittenResponseHeaders.includes(name)) {
                        response.setHeader(name, value);
                    }
                }
                const setCookieHeaders = [...sessionCookies, ...backendResponse.headers.getSetCookie()];
                if (setCookieHeaders.length > 0) {
                    response.setHeader('set-cookie', setCookieHeaders);
                }
                response.end(Buffer.from(await backendResponse.arrayBuffer()));
            } catch (error) {
                // Falls through to the plain proxy, so the request reaches the backend signed out.
                console.error(`[spnego] no token for ${kerberosService}:`, (error as Error).message);
                next();
            }
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
    plugins: [
      react(),
      ...(isSpnegoEnabled ? [createSpnegoPlugin(endpoint, kerberosService)] : []),
    ],
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
