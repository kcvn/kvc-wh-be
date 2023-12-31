FROM eclipse-temurin:17-jdk-alpine
ARG CONFIG_FILE
LABEL mentainer="phong.ld@3si.vn"
WORKDIR /app
 
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

RUN sed -i "s|${spring.datasource.url}|\"${DB_URL}\"|g" ./pom.xml
RUN sed -i "s|${spring.datasource.username}|\"${DB_USER}\"|g" ./pom.xml
RUN sed -i "s|${spring.datasource.password}|\"${DB_PASS}\"|g" ./pom.xml
RUN cat ./pom.xml

COPY src ./src

RUN echo "$CONFIG_FILE" > './src/main/resources/application.properties'
RUN cat ./src/main/resources/application.properties
RUN ./mvnw -Dflyway.configFiles=./src/main/resources/application.properties flyway:migrate
RUN ./mvnw clean package

ENTRYPOINT ["java", "-jar", "./target/spm-0.0.1-SNAPSHOT.jar"]