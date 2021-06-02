FROM maven:3.6.3-jdk-11-slim as maven

WORKDIR /src

ADD . /src
RUN mvn -B clean package
RUN /usr/local/openjdk-11/bin/jlink \
  --no-header-files \
  --no-man-pages \
  --add-modules java.base,java.naming,java.management,java.logging,java.sql,java.xml,java.compiler,jdk.naming.dns,jdk.unsupported \
  --compress 2 \
  --output /usr/local/openjdk-11-slim

FROM debian:buster-slim

ENV JAVA_HOME /usr/local/openjdk-11
ENV PATH $JAVA_HOME/bin:$PATH
ENV LANG C.UTF-8

WORKDIR /opt

COPY --from=maven --chown=nobody:nogroup /src/src/docker/* /src/target/nassh-relay-app.jar /opt/
COPY --from=maven /usr/local/openjdk-11-slim /usr/local/openjdk-11

USER nobody

ENTRYPOINT ["java", "-jar", "nassh-relay-app.jar" ]

CMD ["-conf", "config.json", "-cp", "."]

EXPOSE 8022
