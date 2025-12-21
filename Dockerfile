# Stage 1: Build the application
# We use a stable Maven image with Eclipse Temurin 21 (LTS)
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project files
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Copy the source code
COPY src ./src

# Build the JAR, skipping tests for speed
# This requires <java.version>21</java.version> in your pom.xml
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
# We use the minimal JRE 21 Alpine image (~150MB total size)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security (Alpine syntax)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR from the builder stage
# We use a wildcard *.jar to match the versioned filename automatically
COPY --from=builder /app/target/*.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]