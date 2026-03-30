FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and project metadata first for better layer reuse.
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy application sources and build the executable jar.
COPY src ./src
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
