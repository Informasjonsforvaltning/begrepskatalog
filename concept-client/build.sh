#!/usr/bin/env sh
webpack --config webpack.prod.config.js -p
pm2-docker start.js