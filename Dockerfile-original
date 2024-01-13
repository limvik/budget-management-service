FROM eclipse-temurin:17 AS builder
LABEL authors="limvik"

WORKDIR workspace
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} econome.jar
RUN java -Djarmode=layertools -jar econome.jar extract

FROM eclipse-temurin:17
RUN useradd limvik
USER limvik
WORKDIR workspace
COPY --from=builder workspace/dependencies/ ./
COPY --from=builder workspace/spring-boot-loader/ ./
COPY --from=builder workspace/snapshot-dependencies/ ./
COPY --from=builder workspace/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]