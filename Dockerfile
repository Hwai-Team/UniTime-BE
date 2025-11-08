# 1️⃣ Gradle을 이용해 JAR 빌드
FROM gradle:8.10.2-jdk17-alpine AS build
WORKDIR /app

# Gradle 설정 파일과 소스 복사
COPY build.gradle settings.gradle gradlew gradle/ ./
COPY src ./src

# 빌드 실행
RUN ./gradlew clean bootJar --no-daemon

# 2️⃣ 실행용 JDK 이미지
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 빌드 단계에서 생성된 jar 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 3️⃣ 포트 노출 및 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]