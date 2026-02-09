#!/bin/sh
java -Droot.level=info --enable-native-access=ALL-UNNAMED -XX:+UseZGC -XX:+ExitOnOutOfMemoryError -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -jar ./target/nassh-relay-app.jar -conf ./src/docker/config.json
