FROM eclipse-temurin:24-jre-alpine

RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone \
    && apk del tzdata

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

RUN mkdir -p /app/logs \
    && chown -R spring:spring /app

COPY --chown=spring:spring build/libs/*.jar app.jar

USER spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:+UseG1GC \
    -Xms256m \
    -Xmx400m \
    -XX:MaxMetaspaceSize=150m \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseStringDeduplication \
    -XX:+ExitOnOutOfMemoryError \
    -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
