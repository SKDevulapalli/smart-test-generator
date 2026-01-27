# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage - using Ubuntu-based image for better Chrome compatibility
FROM eclipse-temurin:17-jre-jammy

# Install Chrome and ChromeDriver
RUN apt-get update && apt-get install -y \
    wget \
    gnupg \
    ca-certificates \
    fonts-liberation \
    unzip \
    xdg-utils \
    && wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get install -y ./google-chrome-stable_current_amd64.deb || apt-get install -fy \
    && rm google-chrome-stable_current_amd64.deb \
    && rm -rf /var/lib/apt/lists/* \
    # Get Chrome version and download matching ChromeDriver
    && CHROME_VERSION=$(google-chrome --version | grep -oP '\d+\.\d+\.\d+' | head -1) \
    && CHROME_MAJOR=$(echo $CHROME_VERSION | cut -d. -f1) \
    && echo "Chrome version: $CHROME_VERSION, Major: $CHROME_MAJOR" \
    && wget -q "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_${CHROME_MAJOR}" -O /tmp/chrome_version.txt || echo "131" > /tmp/chrome_version.txt \
    && DRIVER_VERSION=$(cat /tmp/chrome_version.txt) \
    && echo "ChromeDriver version: $DRIVER_VERSION" \
    && wget -q "https://storage.googleapis.com/chrome-for-testing-public/${DRIVER_VERSION}/linux64/chromedriver-linux64.zip" -O /tmp/chromedriver.zip || \
       wget -q "https://edgedl.me.gvt1.com/edgedl/chrome/chrome-for-testing/${DRIVER_VERSION}/linux64/chromedriver-linux64.zip" -O /tmp/chromedriver.zip || true \
    && if [ -f /tmp/chromedriver.zip ]; then \
         unzip -q /tmp/chromedriver.zip -d /tmp/ && \
         mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/ && \
         chmod +x /usr/local/bin/chromedriver && \
         rm -rf /tmp/chromedriver* ; \
       fi

# Set environment variables for Chrome and ChromeDriver
ENV CHROME_BIN=/usr/bin/google-chrome
ENV CHROMEDRIVER_PATH=/usr/local/bin/chromedriver

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Railway uses PORT env variable)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
