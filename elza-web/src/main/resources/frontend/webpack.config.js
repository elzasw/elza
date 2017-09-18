const webpack = require('webpack');
const path = require('path');
const fs = require('fs');
const HappyPack = require('happypack');
const CircularDependencyPlugin = require('circular-dependency-plugin');

const PORT = 8090;

process.env.BABEL_ENV = 'development';
process.env.NODE_ENV = 'development';

function fileExist(filePath){
    try {
        fs.statSync(filePath);
    } catch(err) {
        if(err.code == 'ENOENT') return false;
    }
    return true;
}

console.log(path.resolve(__dirname));

module.exports = {
    entry: [
        //'react-hot-loader/patch',
        `webpack-dev-server/client?http://localhost:${PORT}`,
        'webpack/hot/only-dev-server',
        'babel-polyfill',
        './index.jsx',
    ],
    devtool: 'eval-source-map',
    output: {
        path: path.join(__dirname, 'assets'),
        filename: 'bundle.js',
        publicPath: `http://localhost:${PORT}/assets/`
    },
    devServer: {
        host: 'localhost',
        port: PORT,

        historyApiFallback: true,
        // respond to 404s with index.html

        hot: true,
        // enable HMR on the server
        headers: { 'Access-Control-Allow-Origin': '*' }
    },
    plugins: [
        new CircularDependencyPlugin({
            // exclude detection of files based on a RegExp
            exclude: /node_modules/,
            // add errors to webpack instead of warnings
            failOnError: true
        }),
        /*new HappyPack({
            id: 'jsx',
            threads: 6,
            loaders: ['babel-loader']
        }),*/
        new webpack.HotModuleReplacementPlugin(),
        new webpack.NamedModulesPlugin(),
        new webpack.NoEmitOnErrorsPlugin(),
        new webpack.DefinePlugin({
            __DEV__: true,
            __DEVTOOLS__: fileExist('.dev'),
        }),
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery"
        }),
    ],
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
                    {loader: 'babel-loader'}
                    //{loader: 'happypack/loader?id=jsx'}
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
