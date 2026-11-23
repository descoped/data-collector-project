#!/usr/bin/env bash

# The server and the image data-collector:dev: the debug image, with the tools of the JDK (see Dockerfile)
(cd .. && ./mvnw -pl data-collector-server -am clean package -DskipTests) &&\
docker build -t data-collector:dev --target debug .
