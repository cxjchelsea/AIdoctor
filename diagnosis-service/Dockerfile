FROM openjdk:8-jdk-alpine

WORKDIR /app

# 复制Maven构建的jar包
COPY target/diagnosis-service-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]

