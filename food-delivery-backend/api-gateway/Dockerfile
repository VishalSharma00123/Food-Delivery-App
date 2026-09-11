# for compilation, base image
FROM eclipse-temurin:17-jdk-jammy AS builder

#
WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Makes the Maven wrapper executable.
RUN chmod +x mvnw

# Downloads dependencies before copying src
RUN ./mvnw dependency:go-offline -B

# Copies application source.
COPY src ./src

# Builds the Spring Boot JAR, Tests are skipped
RUN ./mvnw clean package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-jammy

#
WORKDIR /app

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

COPY --from=builder /app/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "app.jar"]