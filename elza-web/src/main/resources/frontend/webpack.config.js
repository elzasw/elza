var webpack = require('webpack');
var path = require('path');

process.env.BABEL_ENV = 'development';
process.env.NODE_ENV = 'development';

module.exports = {
    entry: [
        'webpack/hot/only-dev-server',
        ///'react-hot-loader/patch', - HOT3
        './index.jsx',
    ],
    debug: true,
    devtool: '#eval-source-map',
    output: {
        publicPath: 'http://localhost:8090/assets'
    },
    historyApiFallback: true,
    module: {
        loaders: [
            {
                test: /\.jsx?$/,
                exclude: /(node_modules|bower_components)/,
                loaders: ['react-hot', 'babel-loader'] // Po upgrade na HOT 3 smazat
            },
            {
                test: /\.json$/,
                loader: 'json'
            },
            {
                test: /\.css$/,
                loader: 'style-loader!css-loader?outputStyle=expanded&indentedSyntax'
            },
            {
                test: /\.less$/,
                loader: "style!css!less?outputStyle=expanded&indentedSyntax"
            },
            {
                test: /\.scss$/,
                loader: "style!css!sass?outputStyle=expanded&indentedSyntax"
            },
            {test: /\.woff(\?v=\d+\.\d+\.\d+)?$/, loader: "file"},
            {test: /\.woff2(\?v=\d+\.\d+\.\d+)?$/, loader: "file"},
            {test: /\.ttf(\?v=\d+\.\d+\.\d+)?$/, loader: "file"},
            {test: /\.eot(\?v=\d+\.\d+\.\d+)?$/, loader: "file"},
            {test: /\.svg(\?v=\d+\.\d+\.\d+)?$/, loader: "file"},
            {test: /\.png(\?v=\d+\.\d+\.\d+)?$/, loader: "file"}
        ]
    },
    node: {
        net: "empty",
        tls: "empty"
    },
    externals: {
    },
    resolve: {
        modulesDirectories: [
            "node_modules",
            path.resolve('./')
        ],
        extensions: ['', '.js', '.jsx']
    },
    plugins: [
        // Webpack 1 - pro hot
        new webpack.optimize.OccurenceOrderPlugin(),
        new webpack.NoErrorsPlugin(),
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery"
        }),
        new webpack.DefinePlugin({
            __DEVTOOLS__: false,
            __SHOW_DEVTOOLS__: false,
            __DEV__: true
        })
    ]
}
