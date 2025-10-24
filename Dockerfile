FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
RUN mvn dependency:resolve

COPY src src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE=/app/target/*.jar

COPY --from=builder ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]