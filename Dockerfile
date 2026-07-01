# syntax=docker/dockerfile:1

# --- Build stage: compile, generate the OpenAPI sources and package the fat jar ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Prime the dependency cache on the pom only, so code changes don't re-download everything.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests clean package

# --- Runtime stage: slim JRE, non-root (multi-arch base: amd64 + arm64) ---
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /build/target/url-shortener-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
