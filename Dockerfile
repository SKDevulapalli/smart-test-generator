# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage - using Selenium's official Chrome image which has everything pre-configured
FROM selenium/standalone-chrome:latest

USER root

# Install Java 17
RUN apt-get update && apt-get install -y \
    openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/*

# Set Java home
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Railway uses PORT env variable)
EXPOSE 8080

# Run the application as root (needed for Chrome in some environments)
ENTRYPOINT ["java", "-jar", "app.jar"]
