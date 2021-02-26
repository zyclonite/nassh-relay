#!/bin/sh
java --illegal-access=deny -Droot.level=info -jar ./target/nassh-relay-app.jar -conf ./src/docker/config.json -cp .
