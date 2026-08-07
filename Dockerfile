FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends fonts-thai-tlwg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /workspace/target/hotel-system-0.0.1-SNAPSHOT.jar /app/app.jar

# The restored production database already has its schema and master data.
# Set HOTEL_SCHEMA_INIT_ENABLED=true only for an intentional schema migration.
ENV HOTEL_SCHEMA_INIT_ENABLED=false

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
