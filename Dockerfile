FROM eclipse-temurin:17 AS builder
LABEL authors="limvik"

WORKDIR workspace
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} econome.jar
RUN java -Djarmode=layertools -jar econome.jar extract
#jlink를 이용한 Custom JRE 생성
RUN $JAVA_HOME/bin/jlink \
    # 필요한 modules 추가
    --add-modules ALL-MODULE-PATH \
    # 출력에서 debug information 제거
    --strip-debug \
    # man pages 제외
    --no-man-pages \
    # header files 제외
    --no-header-files \
    # 모든 resources를 ZIP으로 압축
    --compress=2 \
    # runtime image 생성 위치
    --output /javaruntime

# Base Image 지정
FROM debian:buster-slim
RUN useradd limvik
USER limvik
# Java 환경변수 설정
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
# Custom JRE 복사
COPY --from=builder /javaruntime $JAVA_HOME
WORKDIR workspace
COPY --from=builder workspace/dependencies/ ./
COPY --from=builder workspace/spring-boot-loader/ ./
COPY --from=builder workspace/snapshot-dependencies/ ./
COPY --from=builder workspace/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]