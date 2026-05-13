# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and source together, then build directly
# (skip go-offline — it fails on old http:// repos in ehcache's dependency tree)
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests -B \
    -Dmaven.wagon.http.retryHandler.count=3

# ─── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]