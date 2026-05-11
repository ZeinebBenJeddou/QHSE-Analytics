# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

COPY pom.xml ./
COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-alpine AS production
WORKDIR /app

RUN addgroup -S qhse && adduser -S qhse -G qhse

COPY --from=builder /app/target/*.jar app.jar
RUN chown qhse:qhse app.jar

USER qhse
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
