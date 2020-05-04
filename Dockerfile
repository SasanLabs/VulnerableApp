FROM openjdk:8-jdk-alpine
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} vulnerableApp.jar
ENTRYPOINT ["java","-jar","/vulnerableApp.jar"]
