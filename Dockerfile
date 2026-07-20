FROM gradle:8.12-jdk21 AS build
WORKDIR /app
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./
COPY src src/
RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/build/libs/event-sourcing-orders-1.0.0.jar app.jar
RUN jar xf app.jar
ENTRYPOINT ["java", "-cp", "BOOT-INF/classes:BOOT-INF/lib/*", "com.portfolio.eventsourcing.benchmark.BenchmarkRunner"]
