FROM maven:3.9.5-eclipse-temurin-17-alpine

ENV WEB_CRAWLER_PROJECT=/usr/src/app/logfile.log

WORKDIR /usr/src/app

COPY pom.xml .
COPY src ./src

RUN mvn clean install

EXPOSE 8080/tcp

CMD ["java", "-jar", "target/crawler.jar"]