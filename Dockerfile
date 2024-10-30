FROM eclipse-temurin:17-jdk-focal AS build
#ARG CONFIG_FILE

LABEL mentainer="phong.ld@3si.vn"
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw ./
COPY pom.xml ./
RUN chmod +x ./mvnw
RUN chmod 777 ./mvnw
RUN sed -i 's/\r$//' mvnw

COPY src ./src

#RUN echo "$CONFIG_FILE" > './src/main/resources/application.properties'
#RUN cat ./src/main/resources/application.properties

RUN --mount=type=cache,target=/root/.m2,rw ./mvnw -B package

FROM eclipse-temurin:17-jre-focal
WORKDIR /app

ENV JAVA_OPTS="-Dspring.profiles.active=docker -Xms1g -Xmx6g -XX:+UseG1GC -XX:MaxGCPauseMillis=300"

COPY --from=build app/target/spm-0.0.1-SNAPSHOT.jar ./app.jar
COPY --from=build app/target/classes/assets ./target/classes/assets

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]