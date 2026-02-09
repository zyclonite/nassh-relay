FROM maven:3.9.12-eclipse-temurin-25 as maven

WORKDIR /src

ADD . /src
RUN mvn -B clean package
RUN /opt/java/openjdk/bin/jlink \
  --no-header-files \
  --no-man-pages \
  --add-modules java.base,java.naming,java.management,java.logging,java.sql,java.xml,java.compiler,jdk.naming.dns,jdk.unsupported \
  --compress zip-6 \
  --output /opt/java/openjdk-25-slim

FROM ubuntu:jammy

ENV JAVA_HOME /usr/local/openjdk-25
ENV PATH $JAVA_HOME/bin:$PATH
ENV LANG C.UTF-8

WORKDIR /opt

COPY --from=maven --chown=nobody:nogroup /src/src/docker/* /src/target/nassh-relay-app.jar /opt/
COPY --from=maven /opt/java/openjdk-25-slim /usr/local/openjdk-25

USER nobody

ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-XX:+UseZGC", "-XX:TrimNativeHeapInterval=5000", "-XX:+ExitOnOutOfMemoryError", "-XX:+AlwaysPreTouch", "-XX:+DisableExplicitGC", "-jar", "nassh-relay-app.jar" ]

CMD ["-conf", "config.json"]

EXPOSE 8022
