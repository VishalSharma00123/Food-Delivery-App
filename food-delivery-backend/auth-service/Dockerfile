# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
# Ensure mvnw has execute permissions
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the final lightweight container
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Application port
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
