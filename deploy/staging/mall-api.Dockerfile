FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY apps/mall-api/pom.xml apps/mall-api/pom.xml
RUN mvn -B -f apps/mall-api/pom.xml dependency:go-offline
COPY apps/mall-api/src apps/mall-api/src
RUN mvn -B -f apps/mall-api/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home --home-dir /home/commerceflow commerceflow

WORKDIR /app
COPY --from=build /workspace/apps/mall-api/target/mall-api-0.1.0-SNAPSHOT.jar /app/app.jar
RUN chown -R commerceflow:commerceflow /app
USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
