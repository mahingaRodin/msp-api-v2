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
# Copy with explicit name to avoid wildcard issues
COPY --from=build /app/target/*.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]