# Multi-stage build to produce a lightweight runtime image
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Pre-fetch dependencies
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

# Build source
COPY src ./src
RUN mvn -q -DskipTests package dependency:copy-dependencies

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy compiled artifacts
COPY --from=build /app/target/kafka-order-system-1.0-SNAPSHOT.jar ./app.jar
COPY --from=build /app/target/dependency ./lib

# Default to the consumer controller; override MAIN_CLASS to switch entrypoints
ENV MAIN_CLASS=com.example.kafka.controller.ConsumerController
ENTRYPOINT ["sh", "-c", "java -cp app.jar:lib/* ${MAIN_CLASS}"]
