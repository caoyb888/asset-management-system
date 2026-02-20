# ===========================================================
# 资产管理系统 - 微服务 Dockerfile (多阶段构建)
# 用法: docker build --build-arg SERVICE=asset-base -t asset-base .
# ===========================================================

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY backend/ .
ARG SERVICE=asset-base
RUN mvn -pl ${SERVICE} -am clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="asset-team"

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

ARG SERVICE=asset-base
COPY --from=builder /build/${SERVICE}/target/*.jar app.jar
RUN chown -R app:app /app

USER app

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8001

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
