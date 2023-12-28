FROM eclipse-temurin:17-jdk-focal
ARG CONFIG_FILE
WORKDIR /app
 
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
 
COPY src ./src
RUN echo "$CONFIG_FILE" > './src/main/resources/application.properties'
RUN cat ./src/main/resources/application.properties
RUN ./mvnw -Dflyway.configFiles=./src/main/resources/application.properties flyway:migrate
RUN ./mvnw clean install
RUN ./mvnw jooq-codegen:generate

CMD ["./mvnw", "spring-boot:run"]