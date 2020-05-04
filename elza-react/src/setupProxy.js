const { createProxyMiddleware } = require('http-proxy-middleware');

const HOST = process.env.ENDPOINT || 'http://localhost:8080';
const inArray = function(arr, key) {
    for (let word of arr) {
        if (key.indexOf(word) === 0) {
            return true;
        }
    }
    return false
}

const filterApiUrl = (pathname, req) => {
    const forbidden = ['/api', '/login', '/res', '/static', '/sockjs-node'];

    if (inArray(forbidden, pathname)) {
        return false
    }

    const paths = [
        '/fund',
        '/node',
        '/entity',
        '/arr',
        '/registry',
        '/admin',
    ]
    if (req.method === 'GET' && (
        inArray(paths, pathname) ||
        pathname === "/" ||
        pathname === "" ||
        pathname.indexOf("hot-update") !== -1
    )) {
        return false;
    }
    return true;
}


module.exports = function(app) {
    app.use(
        ['/api', '/login'],
        createProxyMiddleware({
            target: HOST,
            changeOrigin: true,
        }),
    );
    app.use(
        '/',
        createProxyMiddleware(
            filterApiUrl,
        {
            target: HOST,
            ws: true,
            changeOrigin: true,
            logLevel: 'debug',
        }),
    );
};
