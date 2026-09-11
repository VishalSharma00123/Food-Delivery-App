# Build context must be food-delivery-backend/ (see docker-compose.yml).
# Alpine Temurin is not available for linux/arm64.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY common-rbac/pom.xml common-rbac/
COPY common-rbac/src common-rbac/src
RUN mvn -f common-rbac/pom.xml -q -B clean install -DskipTests

COPY restaurent-service/pom.xml restaurent-service/
COPY restaurent-service/src restaurent-service/src
RUN mvn -f restaurent-service/pom.xml -q -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/restaurent-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
