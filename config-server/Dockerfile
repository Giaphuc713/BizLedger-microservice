FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl wget
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
