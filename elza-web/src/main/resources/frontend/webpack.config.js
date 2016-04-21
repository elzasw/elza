var webpack = require('webpack');
var path = require('path');

module.exports = {
    entry: [
        './index.jsx',
    ],
    debug: true,
    devtool: '#eval-source-map',
    output: {
        publicPath: 'http://localhost:8090/assets'
    },
    module: {
        loaders: [
            {
                test: /\.jsx?$/,
                exclude: /(node_modules|bower_components)/,
                loaders: ['babel-loader']
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
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery"
        })
    ]
}
