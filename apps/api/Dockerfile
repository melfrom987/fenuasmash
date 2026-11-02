# syntax=docker/dockerfile:1.7
#
# Image de l'API FenuaSmash (Spring Boot / Java, build Maven).
# Contexte de build : racine du dépôt (docker build -f apps/api/Dockerfile .)
#
# L'arborescence /repo/apps/api + /repo/contracts reproduit celle du dépôt,
# pour qu'un plugin OpenAPI puisse lire ../../contracts/openapi.yaml au build.

ARG JAVA_VERSION=25

# --- Build : JDK + Maven Wrapper -> un jar exécutable ------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS build
WORKDIR /repo/apps/api

# Dépendances d'abord : cette couche reste en cache tant que pom.xml ne change pas
COPY apps/api/mvnw apps/api/pom.xml ./
COPY apps/api/.mvn/ .mvn/
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY contracts/ /repo/contracts/
COPY apps/api/src/ src/
# Les tests tournent dans la CI (avec une vraie base), pas pendant le build d'image
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
 && cp target/*.jar /tmp/app.jar

# --- Image finale : JRE seul, utilisateur non root ----------------------------
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -G app app

# La JVM s'adapte à la mémoire du conteneur et s'arrête net en cas d'OOM
# (Docker la redémarre) plutôt que de rester dans un état instable.
ENV SERVER_PORT=8080 \
    TZ=UTC \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

WORKDIR /app
COPY --from=build --chown=app:app /tmp/app.jar app.jar
USER app
EXPOSE 8080

# Spring Boot Actuator : 200 si l'API et la base répondent, 503 sinon
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
  CMD wget -qO- "http://127.0.0.1:${SERVER_PORT}/actuator/health" > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
