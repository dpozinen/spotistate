# Multi-stage Dockerfile for Spotistate
# Stage 1: Build Frontend
FROM node:18 AS frontend-build

WORKDIR /app/frontend

# Copy package files
COPY frontend/package*.json ./

# Clear npm cache and install dependencies
RUN npm cache clean --force
RUN npm install

# Copy frontend source code
COPY frontend/ ./

# Build the React app
RUN npm run build

# Stage 2: Build Backend
FROM gradle:8.4-jdk17-alpine AS backend-build

WORKDIR /app/backend

# Copy Gradle files
COPY backend/gradle/ ./gradle/
COPY backend/gradlew ./
COPY backend/gradlew.bat ./
COPY backend/build.gradle.kts ./
COPY backend/settings.gradle.kts ./

# Make gradlew executable
RUN chmod +x ./gradlew

# Download dependencies (this layer will be cached if dependencies don't change)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY backend/src/ ./src/

# Build the application
RUN ./gradlew bootJar --no-daemon

# Stage 3: Runtime
FROM openjdk:17-jdk-slim

WORKDIR /app

# Install nginx for serving static files
RUN apt-get update && \
    apt-get install -y nginx && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy built frontend from first stage
COPY --from=frontend-build /app/frontend/build /var/www/html

# Copy built backend jar from second stage
COPY --from=backend-build /app/backend/build/libs/*.jar app.jar

# Copy nginx configuration
COPY docker/nginx.conf /etc/nginx/sites-available/default

# Create startup script
COPY docker/start.sh /start.sh
RUN chmod +x /start.sh

# Expose ports
EXPOSE 80 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Start both nginx and spring boot
CMD ["/start.sh"]
