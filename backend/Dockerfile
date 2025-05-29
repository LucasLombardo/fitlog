# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn/ .mvn/
COPY src/ ./src

# Download dependencies and build the project
RUN mvn dependency:go-offline
RUN mvn clean package -DskipTests

# Stage 2: Create the final image
FROM openjdk:17-jdk-slim

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
