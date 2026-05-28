FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x ./gradlew && ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown appuser:appuser app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
