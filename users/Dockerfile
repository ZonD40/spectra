# BUILD
FROM gradle:8.13.0-jdk23 AS builder
WORKDIR /app
COPY users/build.gradle settings.gradle users/gradlew /app/
COPY users/gradle /app/gradle
COPY shared-module /app/shared-module/
COPY users /app/users
RUN ./gradlew dependencies --no-daemon
RUN ./gradlew :users:build -x test --no-daemon


# FINAL
FROM openjdk:23-jdk-slim
WORKDIR /app
COPY --from=builder /app/users/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8081