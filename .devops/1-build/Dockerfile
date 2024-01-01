FROM eclipse-temurin:17-jdk-focal as build
ARG CONFIG_FILE

LABEL mentainer="phong.ld@3si.vn"
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw ./
COPY pom.xml ./
RUN chmod +x ./mvnw
RUN chmod 777 ./mvnw

COPY src ./src

RUN echo "$CONFIG_FILE" > './src/main/resources/application.properties'
RUN cat ./src/main/resources/application.properties

RUN --mount=type=cache,target=/root/.m2,rw ./mvnw -B package

FROM eclipse-temurin:17-jre-focal
WORKDIR /app
RUN echo $(ls -1 ./target)

COPY --from=build target/spm-0.0.1-SNAPSHOT.jar .  
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "spm-0.0.1-SNAPSHOT.jar"]