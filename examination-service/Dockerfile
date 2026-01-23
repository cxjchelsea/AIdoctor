FROM openjdk:8-jdk-alpine

WORKDIR /app

COPY target/examination-service-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]

