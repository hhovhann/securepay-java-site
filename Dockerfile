# 1. Build and test: Maven on Amazon Corretto 17 (Amazon Linux 2023). The build fails if any test fails.
FROM maven:3.9-amazoncorretto-17-al2023 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY content content
COPY src src
RUN mvn -B package

# 2. Run: Apache Tomcat 11 on Amazon Corretto 17 (Amazon Linux 2023), with the WAR deployed as the site root (ROOT.war).
FROM amazoncorretto:17-al2023
ARG TOMCAT_VERSION=11.0.29
RUN dnf install -y tar gzip shadow-utils && dnf clean all \
 && curl -fsSL -o /tmp/tomcat.tgz "https://archive.apache.org/dist/tomcat/tomcat-11/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
 && curl -fsSL -o /tmp/tomcat.tgz.sha512 "https://archive.apache.org/dist/tomcat/tomcat-11/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz.sha512" \
 && cd /tmp && echo "$(cut -d' ' -f1 tomcat.tgz.sha512)  tomcat.tgz" | sha512sum -c - \
 && mkdir -p /opt/tomcat && tar -xzf /tmp/tomcat.tgz -C /opt/tomcat --strip-components=1 && rm -f /tmp/tomcat.tgz* \
 && rm -rf /opt/tomcat/webapps/* \
 # Error pages must not reveal the server version or stack traces.
 && sed -i 's#</Host>#  <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false"/>\n      </Host>#' /opt/tomcat/conf/server.xml \
 && useradd -r -u 10001 -d /opt/tomcat tomcat \
 # Tomcat runs as an unprivileged user: it can read the server and config, and write only logs, temp, work and webapps.
 && chgrp -R tomcat /opt/tomcat && chmod -R g+rX,o-rwx /opt/tomcat \
 && chown -R tomcat /opt/tomcat/logs /opt/tomcat/temp /opt/tomcat/work /opt/tomcat/webapps
COPY --from=build /src/target/securepay-website.war /opt/tomcat/webapps/ROOT.war
USER tomcat
EXPOSE 8080
CMD ["/opt/tomcat/bin/catalina.sh", "run"]
