FROM eclipse-temurin:17-jdk-alpine
ARG CONFIG_FILE
ARG DB_URL
ARG DB_USER
ARG DB_PASS

LABEL mentainer="phong.ld@3si.vn"
WORKDIR /app

ENV spring.datasource.url="$DB_URL"
ENV spring.datasource.username="$DB_USER"
ENV spring.datasource.password="$DB_PASS"

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN chmod +x ./mvnw

RUN ./mvnw dependency:go-offline
RUN cat ./pom.xml

RUN echo "$CONFIG_FILE" > './src/main/resources/application.properties'
RUN cat ./src/main/resources/application.properties
RUN ./mvnw -Dflyway.configFiles=./src/main/resources/application.properties flyway:migrate
RUN ./mvnw clean package

ENTRYPOINT ["java", "-jar", "./target/spm-0.0.1-SNAPSHOT.jar"]