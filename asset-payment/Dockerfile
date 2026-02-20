FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="asset-team"
RUN apk add --no-cache tzdata && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
WORKDIR /app
COPY target/asset-payment-*.jar app.jar
EXPOSE 8013
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseZGC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
