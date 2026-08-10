FROM gradle:8.12-jdk21 AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew
COPY src src/
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean bootJar --no-daemon
RUN mkdir /app/extracted && cd /app/extracted && jar xf /app/build/libs/event-sourcing-orders-1.0.0.jar

FROM build AS test
RUN --mount=type=cache,target=/root/.gradle ./gradlew testClasses --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/extracted/ ./
COPY --from=build /app/gradle/libs.versions.toml gradle/libs.versions.toml
ENV MAIN_CLASS=com.portfolio.eventsourcing.benchmark.BenchmarkRunner
ENTRYPOINT ["sh", "-c", "exec java -cp 'BOOT-INF/classes:BOOT-INF/lib/*' \"$MAIN_CLASS\" \"$@\"", "--"]
