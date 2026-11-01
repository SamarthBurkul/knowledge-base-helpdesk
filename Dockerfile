FROM eclipse-temurin:25-jre

# Run the application as an unprivileged user instead of root.
RUN groupadd --system helpdesk && useradd --system --gid helpdesk --no-create-home helpdesk

# Timestamps shown in the UI use the JVM time zone; override with -e TZ=...
ENV TZ=Asia/Kolkata

WORKDIR /app
COPY target/knowledge-base-helpdesk-1.0.0.jar app.jar
USER helpdesk

EXPOSE 8080

# The JRE base image ships without curl or wget, so the probe uses bash's
# built-in /dev/tcp: it requests /actuator/health and is healthy only when
# the response status line is HTTP 200 (Actuator answers 503 when DOWN).
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && head -n 1 <&3 | grep -q " 200 "' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
