# BUILD
FROM gradle:8.13.0-jdk23 AS builder
WORKDIR /app
COPY notify/build.gradle settings.gradle notify/gradlew /app/
COPY notify/gradle /app/gradle
COPY shared-module /app/shared-module/
COPY notify /app/notify/
RUN ./gradlew dependencies --no-daemon
RUN ./gradlew :notify:build -x test --no-daemon


# FINAL
FROM openjdk:23-jdk-slim
WORKDIR /app
COPY --from=builder /app/notify/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8082