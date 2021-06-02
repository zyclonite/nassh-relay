#!/bin/sh
java -Droot.level=info -jar ./target/nassh-relay-app.jar -conf ./src/docker/config.json -cp .
