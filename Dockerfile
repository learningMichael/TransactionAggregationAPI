# ─────────────────────────────────────────────
# Stage 1: Build
# Uses full JDK to compile and package the app
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy Gradle wrapper and build files first (layer caching — only re-downloads deps if these change)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Pre-download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code and build
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ─────────────────────────────────────────────
# Stage 2: Run
# Uses lightweight JRE — no compiler, no source code
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Security: run as non-root user
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# Copy only the compiled JAR from the build stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Health check — Kubernetes/Docker will use this to determine liveness
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Enable Virtual Threads at the JVM level (Spring Boot 3.2+ honours this via Tomcat config)
ENTRYPOINT ["java", \
  "-Dspring.threads.virtual.enabled=true", \
  "-jar", "app.jar"]
