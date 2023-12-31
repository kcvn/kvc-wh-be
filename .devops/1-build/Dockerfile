FROM eclipse-temurin:17-jdk-alpine
ARG CONFIG_FILE
LABEL mentainer="phong.ld@3si.vn"
WORKDIR /app
 
RUN echo "$CONFIG_FILE" > '/src/main/resources/application.properties'
RUN chmod +x mvnw
RUN mvnw -Dflyway.configFiles=/src/main/resources/application.properties flyway:migrate
RUN mvnw clean package
 
COPY target/spm-0.0.1-SNAPSHOT.jar /app/spm.jar

ENTRYPOINT ["java", "-jar", "spm.jar"]