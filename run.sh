#!/bin/sh
java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory --illegal-access=deny -Droot.level=INFO -jar ./target/nassh-relay-app.jar -conf ./src/docker/config.json -cp .
