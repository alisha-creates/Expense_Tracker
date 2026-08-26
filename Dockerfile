FROM eclipse-temurin:23-jdk

WORKDIR /app

COPY target/*.jar NexSpend.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "NexSpend.jar"]