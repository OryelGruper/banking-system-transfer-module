# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Cache dependencies separately from source so `docker compose up` rebuilds
# are fast on source-only changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl is only needed for the docker-compose healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home appuser
USER appuser

COPY --from=build /build/target/banking-system-transfer-module.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
