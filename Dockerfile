# Render 배포용 Dockerfile.
#
# 멀티스테이지로 나눈 이유: 빌드 단계(Gradle + 소스 전체)는 최종 이미지에 남기지 않는다.
# 실행 단계에는 JRE 와 jar 파일만 남아 이미지가 작고, 빌드 도구가 공격 표면에 들어가지 않는다.

# ── 빌드 단계 ─────────────────────────────────────────
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# 의존성 캐시를 활용하기 위해 wrapper·빌드 스크립트만 먼저 복사한다.
# 소스만 바뀌었을 때는 이 레이어가 캐시에서 재사용된다.
COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle gradle
RUN chmod +x gradlew
RUN ./gradlew --version

COPY src src
RUN ./gradlew clean bootJar --no-daemon

# build/libs 에는 실행 가능한 jar 와 -plain.jar(메인 매니페스트 없음) 두 개가 나온다.
# 버전 번호를 하드코딩하지 않기 위해, -plain 이 아닌 쪽을 찾아 고정된 이름으로 복사해 둔다.
RUN find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \; \
    && test -f /workspace/app.jar

# ── 실행 단계 ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

# 컨테이너 안에서 root 로 실행하지 않는다.
RUN useradd --system --create-home appuser
USER appuser

# Render 는 PORT 환경변수를 주입한다. application.yml 의 server.port: ${PORT:8080} 가 이를 따른다.
ENV SPRING_PROFILES_ACTIVE=prod

# 무료 플랜 메모리(512MB)에 JVM 이 맞도록 힙을 컨테이너 메모리의 비율로 제한한다.
# 고정 -Xmx 대신 비율로 두면 플랜을 올려도 값을 다시 맞출 필요가 없다.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -XX:+UseSerialGC -jar app.jar"]
