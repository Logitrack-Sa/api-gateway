# --- Etapa 1: Build con Maven y Java 25 ---
FROM maven:3-eclipse-temurin-25@sha256:9b5f82b7166db6112eb123ba310a9e010ec3fe95022ac0728f491121563dfbab AS build
WORKDIR /build

COPY pom.xml ./
COPY src/ src/

RUN mvn --batch-mode --no-transfer-progress -DskipTests=true clean package

# --- Etapa 2: Imagen Final de Ejecución Segura ---
FROM eclipse-temurin:25-jre@sha256:dbb43d81099eec91e0800226ed2296d03146ee7857165b95112372ad0d67e87f

# Crear usuario y grupo de sistema sin privilegios (UID/GID 10001)
RUN groupadd --gid 10001 app && useradd --uid 10001 --gid 10001 app

WORKDIR /app

# Copiar el artefacto empaquetado desde la etapa de compilación
COPY --from=build --chown=10001:10001 /build/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

USER 10001:10001

ENV SERVER_PORT=8080 \
    SERVER_ADDRESS=0.0.0.0 \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
