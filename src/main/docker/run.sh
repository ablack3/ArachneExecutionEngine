#!/usr/bin/env bash

if [ -f "/libs-r/$R_INSTALL_SCRIPT" ]; then
 Rscript /libs-r/$R_INSTALL_SCRIPT
fi
java -Djava.security.egd=file:/dev/./urandom -jar /execution-engine.jar