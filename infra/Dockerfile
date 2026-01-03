FROM gradle:jdk24-alpine AS build

WORKDIR /app

COPY --link gradle gradle
COPY --link gradlew .
COPY --link settings.gradle.kts .
COPY --link build.gradle.kts .

RUN --mount=type=cache,target=/root/.gradle \
    gradle dependencies --no-daemon --stacktrace

COPY --link src src

RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=cache,target=/app/build \
    gradle bootJar --no-daemon -x test --parallel --build-cache && \
    cp build/libs/*.jar app.jar

FROM eclipse-temurin:24-jre-alpine

RUN apk add --no-cache wget tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=build --chown=spring:spring /app/app.jar app.jar

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
