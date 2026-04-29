# JDK 21 기반의 OpenJDK 이미지를 사용
# FROM openjdk:21-jdk-slim
FROM amazoncorretto:25-al2023-headless

# 작업 디렉토리 설정
WORKDIR / app

# 빌드된 JAR 파일을 컨테이너에 복사
COPY build/libs/*.jar app.jar

# 8080 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]