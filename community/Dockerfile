# BUILD
FROM gradle:8.13.0-jdk23 AS builder
WORKDIR /app
COPY community/build.gradle settings.gradle community/gradlew /app/
COPY community/gradle /app/gradle
COPY shared-module /app/shared-module/
COPY community /app/community/
RUN ./gradlew dependencies --no-daemon
RUN ./gradlew :community:build -x test --no-daemon


# FINAL
FROM openjdk:23-jdk-slim
WORKDIR /app
COPY --from=builder /app/community/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080