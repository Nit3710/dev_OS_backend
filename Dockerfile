# Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom files
COPY pom.xml .
COPY devos-core/pom.xml ./devos-core/
COPY devos-api/pom.xml ./devos-api/
COPY devos-ai-integration/pom.xml ./devos-ai-integration/
COPY devos-file-operations/pom.xml ./devos-file-operations/
COPY devos-application/pom.xml ./devos-application/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY devos-core/src ./devos-core/src
COPY devos-api/src ./devos-api/src
COPY devos-ai-integration/src ./devos-ai-integration/src
COPY devos-file-operations/src ./devos-file-operations/src
COPY devos-application/src ./devos-application/src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install necessary packages
RUN apk add --no-cache git curl

# Create app user
RUN addgroup -g 1001 devos && \
    adduser -D -s /bin/sh -u 1001 -G devos devos

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/devos-application/target/devos-application-*.jar app.jar

# Create necessary directories
RUN mkdir -p /app/logs /app/uploads /app/projects && \
    chown -R devos:devos /app

# Switch to non-root user
USER devos

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

# JVM options
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
