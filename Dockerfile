# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (caching optimization)
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn -DskipTests clean package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 5000
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+UseContainerSupport"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]