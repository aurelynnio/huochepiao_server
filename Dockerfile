FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

ARG MODULE

COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY discovery-service/pom.xml discovery-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY search-service/pom.xml search-service/pom.xml
COPY ticket-service/pom.xml ticket-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml

RUN mvn --no-transfer-progress -B -DskipTests -pl ${MODULE} -am dependency:go-offline

COPY . .
RUN mvn --no-transfer-progress -B -DskipTests -pl ${MODULE} -am package

FROM eclipse-temurin:21-jre
WORKDIR /app

ARG MODULE
COPY --from=build /workspace/${MODULE}/target/${MODULE}-1.0-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
