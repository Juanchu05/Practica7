# ─────────────────────────────────────────────────────────────────────────────
# Dockerfile para la aplicación Spring Boot - Springuma
# Usa una imagen base oficial de Eclipse Temurin JDK 17 sobre Alpine Linux.
# Alpine es una distribución minimalista (~5MB) que reduce el tamaño final
# de la imagen frente a usar debian/ubuntu como base.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Directorio de trabajo dentro del contenedor donde se copiará el JAR
WORKDIR /app

# Copiar el JAR generado por Maven (target/*.jar) al contenedor.
# El wildcard *.jar evita tener que hardcodear la versión del artefacto.
COPY target/*.jar app.jar

# Puerto en el que escucha la aplicación Spring Boot (definido en application.properties)
EXPOSE 8080

# Comando de arranque del contenedor: ejecuta el JAR con java -jar
ENTRYPOINT ["java", "-jar", "app.jar"]