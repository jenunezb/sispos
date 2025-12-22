# =========================
# STAGE 1: BUILD
# =========================
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test

# =========================
# STAGE 2: RUNTIME
# =========================
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/pos-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-Xms128m","-Xmx256m","-XX:+UseSerialGC","-jar","app.jar"]
