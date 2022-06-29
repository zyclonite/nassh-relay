FROM maven:3.8.6-eclipse-temurin-17 as maven

WORKDIR /src

ADD . /src
RUN mvn -B clean package
RUN /opt/java/openjdk/bin/jlink \
  --no-header-files \
  --no-man-pages \
  --add-modules java.base,java.naming,java.management,java.logging,java.sql,java.xml,java.compiler,jdk.naming.dns,jdk.unsupported \
  --compress 2 \
  --output /opt/java/openjdk-17-slim

FROM debian:bullseye-slim

ENV JAVA_HOME /usr/local/openjdk-17
ENV PATH $JAVA_HOME/bin:$PATH
ENV LANG C.UTF-8

WORKDIR /opt

COPY --from=maven --chown=nobody:nogroup /src/src/docker/* /src/target/nassh-relay-app.jar /opt/
COPY --from=maven /opt/java/openjdk-17-slim /usr/local/openjdk-17

USER nobody

ENTRYPOINT ["java", "-jar", "nassh-relay-app.jar" ]

CMD ["-conf", "config.json", "-cp", "."]

EXPOSE 8022
