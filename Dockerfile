# One Dockerfile for every service. docker-compose passes the module name:
#   docker build --build-arg MODULE=order-service -t flash-sale/order-service .

# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
# -pl builds only this module, -am also builds what it depends on (the common module)
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -pl ${MODULE} -am package -DskipTests

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar app.jar
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
