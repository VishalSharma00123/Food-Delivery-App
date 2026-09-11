# Build context must be food-delivery-backend/ (see docker-compose.yml).
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY common-rbac/pom.xml common-rbac/
COPY common-rbac/src common-rbac/src
RUN mvn -f common-rbac/pom.xml -q -B clean install -DskipTests

COPY order-service/pom.xml order-service/
COPY order-service/src order-service/src
RUN mvn -f order-service/pom.xml -q -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/order-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
