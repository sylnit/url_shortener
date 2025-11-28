# Use an official OpenJDK runtime as a base image
FROM eclipse-temurin:21

# Set the working directory in the container
WORKDIR /app

# Copy the Maven wrapper (if used) and build files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (to leverage Docker layer caching)
RUN ./mvnw dependency:resolve

# Copy source code
COPY src src

# Build the application
RUN ./mvnw package -DskipTests

# Expose the port your Spring Boot app runs on (default: 8080)
EXPOSE 8080

COPY target/*.jar app.jar

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]