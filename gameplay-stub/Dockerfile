FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src/main/kotlin ./src/main/kotlin
COPY src/main/resources/profanity.txt ./src/main/resources/profanity.txt
COPY gameplay-stub/build.gradle.kts ./gameplay-stub/build.gradle.kts
COPY gameplay-stub/src/main ./gameplay-stub/src/main
RUN chmod +x gradlew && ./gradlew --no-daemon jar :gameplay-stub:jar

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system riverking && useradd --system --gid riverking riverking \
    && mkdir -p /app /data/gameplay-stub && chown -R riverking:riverking /data
WORKDIR /data
COPY --from=build /build/build/libs/riverking-all.jar /app/backend.jar
COPY --from=build /build/gameplay-stub/build/libs/gameplay-stub.jar /app/worker.jar
COPY gameplay-stub/logback.xml /app/logback.xml
ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:+UseG1GC"
USER riverking
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dlogback.configurationFile=/app/logback.xml -cp '/app/*' stub.MainKt \"$@\"", "--"]
