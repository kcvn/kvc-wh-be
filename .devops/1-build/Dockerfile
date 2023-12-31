FROM eclipse-temurin:17-jdk-alpine
ARG CONFIG_FILE

LABEL mentainer="phong.ld@3si.vn"
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src
RUN chmod +x ./mvnw

RUN echo "$CONFIG_FILE" > './src/main/resources/application.properties'
RUN cat ./src/main/resources/application.properties
RUN ./mvnw -f ./pom.xml install -DskipTests
RUN cat ./pom.xml

RUN ./mvnw -Dflyway.configFiles=./src/main/resources/application.properties flyway:migrate

ADD ./target/23-kcvn-spm-be-0.0.1-SNAPSHOT.jar ./23-kcvn-spm-be.jar

ENTRYPOINT ["java", "-jar", "23-kcvn-spm-be.jar"]