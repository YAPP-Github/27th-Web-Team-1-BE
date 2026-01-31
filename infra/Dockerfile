FROM eclipse-temurin:24-jre-alpine

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --chown=spring:spring build/libs/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
