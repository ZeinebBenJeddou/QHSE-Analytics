# build avec maven
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# download dependances en cache séparé
RUN ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw package -DskipTests -B

# image  production legere
FROM eclipse-temurin:21-jre-alpine AS production
WORKDIR /app
# user non-root pr  securite
RUN addgroup -S qhse && adduser -S qhse -G qhse
# copy  jar depuis  builder
COPY --from=builder /app/target/*.jar app.jar
# changer  proprietaire
RUN chown qhse:qhse app.jar
USER qhse
# port exposé
EXPOSE 8080
# health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
# lancement
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]