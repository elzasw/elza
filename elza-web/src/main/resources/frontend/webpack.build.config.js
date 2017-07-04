process.env.BABEL_ENV = 'production';
process.env.NODE_ENV = 'production';
const path = require('path');
const webpack = require('webpack');
const CommonsChunkPlugin = require("webpack/lib/optimize/CommonsChunkPlugin");
const CircularDependencyPlugin = require('circular-dependency-plugin');

const SOURCE_MAP = false

module.exports = {
    devtool: SOURCE_MAP ? 'source-map' : false,
    bail: true,
    entry: {
        main: [
            './public_entry.jsx',
            './index.jsx'
        ]
    },
    output: {
        path: path.resolve('../../../../target/react-dist'),
        filename: "[name].js",
        sourceMapFilename: SOURCE_MAP ? "[name].js.map" : null,
    },
    resolve: {
        extensions: ['.js', '.jsx'],
        modules: [
            path.resolve(__dirname),
            path.resolve(__dirname, "node_modules")
        ]
    },
    module: {
        rules: [
            {
                test: /\.jsx?$/,
                exclude: /node_modules/,
                use: [
                    {loader: 'babel-loader'}
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
                            noIeCompat: false,
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
            {test: /\.(ttf|eot|svg)?$/, use: [{loader: "file-loader?name=[name].[ext]"}]}
        ]
    },
    node: {
        net: "empty",
        tls: "empty"
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

        // ignore dev config
        new webpack.IgnorePlugin(/\.\/dev/, /\/config$/),


        new webpack.LoaderOptionsPlugin({
            minimize: true,
            debug: false
        }),

        new webpack.DefinePlugin({
            'process.env.BABEL_ENV': JSON.stringify(process.env.NODE_ENV),
            'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV),
            __DEVELOPMENT__: false,
            __DEVTOOLS__: false,
            __SHOW_DEVTOOLS__: false,
            __DEV__: false
        }),

        // optimizations
        new webpack.optimize.ModuleConcatenationPlugin(),
        new webpack.optimize.UglifyJsPlugin({
            compress: {
                screw_ie8: true,
                warnings: false
            },
            output: {
                comments: false
            },
            sourceMap: SOURCE_MAP
        }),
        new webpack.optimize.OccurrenceOrderPlugin(true)

    ]
};
