# 1️⃣ Gradle이랑 JDK 17이 같이 들어있는 베이스 이미지
FROM gradle:8.10.2-jdk17-alpine AS build
WORKDIR /app

# 2️⃣ Gradle 설정 파일 / 소스 복사
# settings 파일이 settings.gradle.kts이면 밑에 이름만 바꿔줘
COPY build.gradle settings.gradle ./
COPY src ./src

# 3️⃣ Gradle로 Spring Boot JAR 빌드
RUN gradle clean bootJar --no-daemon

# 4️⃣ 실제 실행용 JDK 이미지
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# 5️⃣ 빌드 단계에서 만든 jar만 들고 옴
COPY --from=build /app/build/libs/*.jar app.jar

# 6️⃣ 서버 포트
EXPOSE 8080

# 7️⃣ 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]