FROM openjdk:17-slim-buster as build
ARG CONFIG_FILE

LABEL mentainer="phong.ld@3si.vn"
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN chmod 777 ./mvnw
RUN ./mvnw -B dependency:go-offline

COPY src ./src

RUN echo "$CONFIG_FILE" > './src/main/resources/application.properties'
RUN cat ./src/main/resources/application.properties

RUN ./mvnw -f ./pom.xml -B package

FROM openjdk:17-jre-slim-buster

COPY ./target/23-kcvn-spm-be-0.0.1-SNAPSHOT.jar ./23-kcvn-spm-be.jar

ENTRYPOINT ["java", "-jar", "23-kcvn-spm-be.jar"]