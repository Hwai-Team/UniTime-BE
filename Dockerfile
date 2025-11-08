# 1️⃣ JDK 17 기반 이미지 (안정 버전)
FROM eclipse-temurin:17-jdk-jammy

# 2️⃣ 작업 디렉토리 지정
WORKDIR /app

# 3️⃣ 빌드 결과물 복사
COPY build/libs/UniTime-0.0.1-SNAPSHOT.jar app.jar

# 4️⃣ 컨테이너가 노출할 포트
EXPOSE 8080

# 5️⃣ Spring Boot 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]