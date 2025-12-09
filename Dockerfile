# Multi-stage build para compilar y ejecutar
FROM maven:3.8-openjdk-11-slim AS build

WORKDIR /app

# Copiar archivos de Maven primero (para cache)
COPY pom.xml .

# Copiar SDK de Octane (necesario antes de compilar)
COPY Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar Octane_SDK_Java_3_0_0/lib/

# Copiar código fuente
COPY src ./src

# Compilar aplicación (descarga dependencias y compila)
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:11-jre

WORKDIR /app

# Copiar JAR compilado
COPY --from=build /app/target/rfid-gateway-*.jar app.jar

# Copiar SDK de Octane
COPY --from=build /app/Octane_SDK_Java_3_0_0/lib/OctaneSDKJava-3.0.0.0-jar-with-dependencies.jar /app/lib/octane-sdk.jar

# Crear usuario no-root para seguridad
RUN groupadd -r rfid && useradd -r -g rfid rfid
RUN chown -R rfid:rfid /app
USER rfid

# Exponer puerto
EXPOSE 8080

# Variables de entorno
ENV JAVA_OPTS="-Xmx512m -Xms256m -Djava.security.egd=file:/dev/./urandom"

# Instalar curl para health check
USER root
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
USER rfid

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# Ejecutar aplicación con SDK en classpath usando Spring Boot loader
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dloader.path=/app/lib -jar app.jar"]
