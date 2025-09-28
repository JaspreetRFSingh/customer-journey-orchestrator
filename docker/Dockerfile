# Multi-stage Dockerfile for optimized image size
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom files first for better layer caching
COPY pom.xml .
COPY core/pom.xml core/
COPY event-processor/pom.xml event-processor/
COPY orchestration-engine/pom.xml orchestration-engine/
COPY channel-executor/pom.xml channel-executor/
COPY infrastructure/pom.xml infrastructure/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY core/src core/src
COPY event-processor/src event-processor/src
COPY orchestration-engine/src orchestration-engine/src
COPY channel-executor/src channel-executor/src
COPY infrastructure/src infrastructure/src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# Copy the built jar
COPY --from=builder /app/infrastructure/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs \
               -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
