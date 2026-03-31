# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Gradle metadata first so Docker can reuse this layer until build config changes.
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

# Reuse Gradle caches across Docker builds even when application sources change.
RUN --mount=type=cache,target=/root/.gradle,id=gradle-cache,sharing=locked \
    chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# Copy only production sources so test-only changes do not invalidate the jar build.
COPY src/main ./src/main
RUN --mount=type=cache,target=/root/.gradle,id=gradle-cache,sharing=locked \
    ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

# Create the runtime user and writable directories needed by the prod profile.
RUN groupadd -r appuser && useradd -r -g appuser appuser \
    && mkdir -p /app /var/log/lucr \
    && chown -R appuser:appuser /app /var/log/lucr

COPY --chown=appuser:appuser --from=builder /app/build/libs/*.jar app.jar

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
