const path = require('path');
const CopyPlugin = require('copy-webpack-plugin');

const bundleOutputPath = path.resolve(__dirname,'../chat-server/src/main/resources/static');
// const bundleOutputName = 'main.js';

//todo write to pack.json.version with pom version

module.exports = {
    mode: 'development',
    plugins: [
        new CopyPlugin([
            { from: path.resolve(__dirname,'src/main/resources'), to: bundleOutputPath }
        ]),
    ]
    ,entry: {
        main: path.resolve(__dirname,'src/main/js/main.js')
    },
    output: {
        filename: '[name].js',
        path: bundleOutputPath
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: [
                            ['@babel/preset-env', {
                                targets: {
                                    esmodules: true
                                }}
                            ]
                        ],
                        plugins: [  '@babel/plugin-proposal-object-rest-spread',
                                    '@babel/plugin-proposal-class-properties',
                        ],
                        cacheDirectory: true
                    }
                }
            }
        ]
    }
    //todo del minifying
    //todo play with this/babel configs
};