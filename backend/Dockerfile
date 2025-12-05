# Multi-stage build for ServiceSlicer

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -P docker-build

# Stage 2: Runtime image
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Install docker CLI and k6
RUN apt-get update && apt-get install -y \
    curl \
    && curl -fsSL https://get.docker.com -o get-docker.sh \
    && sh get-docker.sh \
    && K6_VERSION=v1.4.2 \
    && ARCH=$(dpkg --print-architecture) \
    && curl -L "https://github.com/grafana/k6/releases/download/${K6_VERSION}/k6-${K6_VERSION}-linux-${ARCH}.tar.gz" -o k6.tar.gz \
    && tar -xzf k6.tar.gz --strip-components=1 -C /usr/local/bin \
    && rm k6.tar.gz \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* get-docker.sh

# Copy the built JAR from build stage
COPY --from=build /app/target/ServiceSlicer-*.jar app.jar

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
