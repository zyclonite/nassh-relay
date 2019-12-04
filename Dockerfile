FROM maven:3.6.3-jdk-11-slim as maven

WORKDIR /src

ADD . /src
RUN mvn -B clean package


FROM openjdk:11-jre-stretch

WORKDIR /opt

COPY --from=maven --chown=nobody:nogroup /src/src/docker/* /src/target/nassh-relay-app.jar /opt/

USER nobody

ENTRYPOINT ["java", \
            "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory", \
            "--illegal-access=deny", "-jar", "nassh-relay-app.jar" \
]

CMD ["-conf", "config.json", "-cp", "."]

EXPOSE 8022
