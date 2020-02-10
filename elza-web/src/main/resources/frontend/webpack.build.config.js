process.env.BABEL_ENV = 'production';
process.env.NODE_ENV = 'production';
const path = require('path');
const webpack = require('webpack');
const CircularDependencyPlugin = require('circular-dependency-plugin');

const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const styleLoader = MiniCssExtractPlugin.loader;
const cssLoader = require.resolve('css-loader');
const sassLoader = require.resolve('sass-loader');
const lessLoader = require.resolve('less-loader');
const resolveUrlLoader = require.resolve('resolve-url-loader');


const SOURCE_MAP = false

module.exports = {
    devtool: SOURCE_MAP ? 'source-map' : false,
    bail: true,
    entry: {
        main: [
            'babel-polyfill',
            './src/public_entry.jsx',
            './src/index.jsx'
        ]
    },
    output: {
        path: path.resolve('../../../../target/react-dist'),
        filename: "[name].js",
        sourceMapFilename: "[name].js.map",
    },
    resolve: {
        extensions: ['.tsx', '.ts', '.jsx', '.js'],
        modules: [
            path.resolve(__dirname),
            path.resolve(__dirname, "node_modules")
        ],
        alias: {
            'stompjs': path.resolve(__dirname, "node_modules") + '/stompjs/lib/stomp.js',
        }
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: ['babel-loader','ts-loader'],
                exclude: /node_modules/
            },
            {
                test: /\.jsx?$/,
                exclude: /node_modules/,
                use: [
                    {loader: 'babel-loader'}
                ]
            },
            {test: /\.css$/, use: [styleLoader, {loader: cssLoader, options: {sourceMap: true}}]},
            {
                test: /\.less$/, use: [
                    styleLoader,
                    {loader: cssLoader, options: {sourceMap: true}},
                    {loader: lessLoader, options: {
                        sourceMap: true,
                        paths: [
                            path.resolve(__dirname)
                        ]
                    }}
                ]
            },
            {
                test: /\.scss$/, use: [
                    styleLoader,
                    {loader: cssLoader, options: {sourceMap: true}},
                    {loader: resolveUrlLoader, options: {sourceMap: true}},
                    {loader: sassLoader, options: {sourceMap: true}}
                ]
            },
            {
                test: /\.(gif|png)$/,
                use: [{loader: "url-loader?mimetype=image/png"}]
            },
            {
                test: /\.woff(2)?(\?v=[0-9].[0-9].[0-9])?$/,
                use: [{loader: "url-loader?mimetype=application/font-woff"}]
            },
            {test: /\.(ttf|eot|svg)?$/, use: [{loader: "file-loader?name=[name].[ext]"}]}
        ]
    },
    plugins: [
        new CircularDependencyPlugin({
            // exclude detection of files based on a RegExp
            exclude: /node_modules/,
            // add errors to webpack instead of warnings
            failOnError: true
        }),
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery"
        }),

        new webpack.DefinePlugin({
            'process.env.BABEL_ENV': JSON.stringify(process.env.NODE_ENV),
            'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV),
            __DEVELOPMENT__: false,
            __DEVTOOLS__: false,
            __SHOW_DEVTOOLS__: false,
            __DEV__: false
        }),


        new MiniCssExtractPlugin({
            filename: "[name].css",
            chunkFilename: "[id].css"
        }),


    ]
};
