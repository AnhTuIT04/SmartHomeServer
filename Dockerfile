# Stage 1: Build
FROM eclipse-temurin:23-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and project files
COPY mvnw mvnw.cmd ./  
COPY .mvn .mvn  
COPY pom.xml ./  

# Download dependencies first to leverage caching
RUN ./mvnw dependency:go-offline -B  

# Copy source code
COPY src src  

# Build the application
RUN ./mvnw clean package -DskipTests  

# Stage 2: Run
FROM eclipse-temurin:23-jre

WORKDIR /app

# Copy target directory from the builder stage
COPY --from=builder /app/target /app/target 

# Expose the application port
EXPOSE 8080  

# Run the application
ENTRYPOINT ["java", "-jar", "target/smart-home-0.0.1-SNAPSHOT.jar"]
