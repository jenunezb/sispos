# Imagen Java 17 (compatible con Spring Boot 3)
FROM eclipse-temurin:17-jre

# Directorio de trabajo
WORKDIR /app

# Copiamos el JAR generado por Gradle
COPY build/libs/pos-1.0.0.jar app.jar

# Puerto (Railway usa PORT internamente)
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]
