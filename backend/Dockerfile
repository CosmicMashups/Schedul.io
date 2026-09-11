# Multi-stage build: compile with Maven + the full JDK, run on a slim JRE.
# No Maven wrapper is checked in, so build with an image that already has Maven installed.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Cache dependencies separately from source so a source-only change doesn't re-download them.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

# Render sets $PORT; server.port in application.yml already respects it via -D override below.
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
