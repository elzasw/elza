const proxy = require('http-proxy-middleware');



const HOST = process.env.ENDPOINT || 'http://localhost:8080';

module.exports = function(app) {
    app.use(
        '/api',
        proxy({
            target: HOST,
            ws: true,
            changeOrigin: true
        })
    );
    app.use(
        '/login',
        proxy({
            target: HOST,
            ws: true,
            changeOrigin: true
        })
    );
    app.use(
        '^/stomp',
        proxy({
            target: HOST,
            ws: true,
            changeOrigin: true,
            proxy: true,
        })
    );
};
