const path = require('path');

const bundlePath = '../chat-server/src/main/resources/static';
const bundleName = 'chat.bundle.js';

//todo write to pack.json.version with pom version

module.exports = {
    // mode: "development",
    entry: './src/main/js/chat.js',
    output: {
        path: path.resolve(__dirname, bundlePath),
        filename: bundleName,
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: ['@babel/preset-env'],
                        plugins: [  '@babel/plugin-proposal-object-rest-spread',
                                    '@babel/plugin-proposal-class-properties'   ],
                        cacheDirectory: true
                    }
                }
            }
        ]
    }
    //todo del minifying
    //todo play with this/babel configs
};