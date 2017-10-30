const webpack = require('webpack');
const path = require('path');
const fs = require('fs');
const HappyPack = require('happypack');
const CircularDependencyPlugin = require('circular-dependency-plugin');

process.env.BABEL_ENV = 'development';
process.env.NODE_ENV = 'development';

// Default config
const defaultConfig = {
    hot: false,
    circularDependencyCheck: true,
    sourceMap: 'eval-source-map',
    happyPack: false,
    devTools: false,
    port: 8090
};

// File config
let fileConfig = {};
const fileConfigPath = path.resolve(__dirname, '.dev');
if (fs.existsSync(fileConfigPath)) {
    const fileConfigContent = fs.readFileSync(fileConfigPath, 'utf8');
    if (fileConfigContent) {
        try {
            fileConfig = JSON.parse(fileConfigContent);
        } catch (e) {
            console.error("Error in parsing file config");
            fileConfig = {};
        }
    }
}
// Merged - Config
const config = Object.assign(defaultConfig, fileConfig);

// Entry points
const webpackAndPolyfillEntries = [
    `webpack-dev-server/client?http://localhost:${config.port}`,
    'webpack/hot/only-dev-server',
    'babel-polyfill',
];
const reactHotEntry = ['react-hot-loader/patch'];
const appEntry = ['./index.jsx'];

// Plugins
const defaultPlugins = [
    new webpack.HotModuleReplacementPlugin(),
    new webpack.NamedModulesPlugin(),
    new webpack.NoEmitOnErrorsPlugin(),
    new webpack.DefinePlugin({
        __DEV__: true,
        __DEVTOOLS__: config.devTools,
    }),
    new webpack.ProvidePlugin({
        $: "jquery",
        jQuery: "jquery"
    })
];
const circularDependencyPlugin = [
    new CircularDependencyPlugin({
        // exclude detection of files based on a RegExp
        exclude: /node_modules/,
        // add errors to webpack instead of warnings
        failOnError: true
    })
];
const happyPackPlugin = [
    new HappyPack({
        id: 'jsx',
        threads: 6,
        loaders: ['babel-loader']
    }),
];

module.exports = {
    entry: [].concat(
        webpackAndPolyfillEntries,
        config.hot ? reactHotEntry : [],
        appEntry
    ),
    devtool: config.sourceMap,
    output: {
        path: path.join(__dirname, 'assets'),
        filename: 'bundle.js',
        publicPath: `http://localhost:${config.port}/assets/`
    },
    devServer: {
        host: 'localhost',
        port: config.port,

        historyApiFallback: true,
        // respond to 404s with index.html

        hot: config.hot,
        // enable HMR on the server
        headers: { 'Access-Control-Allow-Origin': '*' }
    },
    plugins: [].concat(
        config.circularDependencyCheck ? circularDependencyPlugin : [],
        config.happyPack ? happyPackPlugin : [],
        defaultPlugins
    ),
    resolve: {
        extensions: ['.js', '.jsx'],
        modules: [
            path.resolve(__dirname),
            path.resolve(__dirname, "node_modules")
        ]
    },
    node: {
        net: "empty",
        tls: "empty"
    },
    watchOptions: {
        aggregateTimeout: 300,
        poll: 1000
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                exclude: /node_modules/,
                use: [
                    {loader: config.happyPack ? 'happypack/loader?id=jsx' : 'babel-loader'}
                ]
            },
            {
                test: /\.css/,
                use: [{loader: "style-loader"}, {loader: "css-loader"}]
            },
            {
                test: /\.less$/,
                use: [
                    {loader: "style-loader"},
                    {loader: "css-loader"},
                    {
                        loader: "less-loader",
                        options: {
                            strictMath: true,
                            noIeCompat: true,
                            paths: [
                                path.resolve(__dirname),
                                path.resolve(__dirname, "node_modules")
                            ]
                        }
                    }]
            },
            {
                test: /\.scss$/,
                use: [{loader: "style-loader"}, {loader: "css-loader"}, {loader: "sass-loader"}]
            },
            {
                test: /\.(gif|png)$/,
                use: [{loader: "url-loader?mimetype=image/png"}]
            },
            {
                test: /\.woff(2)?(\?v=[0-9].[0-9].[0-9])?$/,
                use: [{loader: "url-loader?mimetype=application/font-woff"}]
            },
            {test: /\.(ttf|eot|svg)(\?v=[0-9].[0-9].[ž0-9])?$/, use: [{loader: "file-loader?name=[name].[ext]"}]}
        ]
    }
};
