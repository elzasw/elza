const proxy = require('http-proxy-middleware');



module.exports = function(app) {
    app.use(
        '/api',
        proxy({
            target: 'http://localhost:8080',
            ws: true,
            changeOrigin: true
        })
    );
    app.use(
        '/login',
        proxy({
            target: 'http://localhost:8080',
            target: 'http://elza-depend.e1.marbes.cz',
            ws: true,
            changeOrigin: true
        })
    );
    app.use(
        '/stomp',
        proxy({
            target: 'http://localhost:8080',
            target: 'http://elza-depend.e1.marbes.cz',
            ws: true,
            changeOrigin: true
        })
    );
};
