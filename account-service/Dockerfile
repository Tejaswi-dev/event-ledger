FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY event-gateway/pom.xml event-gateway/pom.xml
COPY account-service/pom.xml account-service/pom.xml
COPY event-gateway/src event-gateway/src
COPY account-service/src account-service/src
RUN mvn -q -pl account-service -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/account-service/target/account-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=5 \
    CMD curl -f http://localhost:8081/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
