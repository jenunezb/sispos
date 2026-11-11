# =========================
# STAGE 1: BUILD
# =========================
FROM gradle:8.8-jdk17 AS build

WORKDIR /app

COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN gradle --no-daemon clean test bootJar

# =========================
# STAGE 2: RUNTIME
# =========================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/pos-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-Xms128m","-Xmx512m","-XX:+UseSerialGC","-XX:MaxMetaspaceSize=256m","-jar","app.jar"]
