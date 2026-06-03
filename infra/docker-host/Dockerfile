# Production image for Windows Docker-host deployments.
# Build context: repository root.

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew --no-daemon clean jar

FROM eclipse-temurin:17-jre-jammy AS prod
WORKDIR /data

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates tini wget \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system riverking \
    && useradd --system --gid riverking --home-dir /data --shell /usr/sbin/nologin riverking \
    && mkdir -p /app /config /data/logs /data/event-assets \
    && chown -R riverking:riverking /app /config /data

COPY --from=build /app/build/libs/riverking-all.jar /app/riverking-all.jar

ENV CONFIG_PATH=/config/config.properties
ENV EVENT_ASSETS_DIR=/data/event-assets
ENV PORT=5005
ENV JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC"

EXPOSE 5005

USER riverking

ENTRYPOINT ["/usr/bin/tini", "-s", "--", "sh", "-c", "exec java $JAVA_OPTS -jar /app/riverking-all.jar"]
