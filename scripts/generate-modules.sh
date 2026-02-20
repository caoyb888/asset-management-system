#!/bin/bash
#=============================================================================
# 资产管理系统 - 后端业务模块批量脚手架生成脚本
# 基于 asset-base 模板快速生成其余业务微服务
#=============================================================================
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="${PROJECT_DIR}/backend"
BASE_PKG="com.asset"

GREEN='\033[0;32m'; BLUE='\033[0;34m'; NC='\033[0m'
info() { echo -e "${BLUE}[GEN]${NC} $1"; }
ok()   { echo -e "${GREEN}[ OK]${NC} $1"; }

# 服务定义: 名称|端口|中文描述|包名后缀
SERVICES=(
  "asset-investment|8002|招商管理服务|investment"
  "asset-operation|8003|营运管理服务|operation"
  "asset-finance|8004|财务管理服务|finance"
  "asset-report|8005|报表管理服务|report"
  "asset-system|8006|系统管理服务|system"
  "asset-gis|8007|GIS可视化服务|gis"
  "asset-workflow|8010|工作流服务|workflow"
  "asset-message|8011|消息服务|message"
  "asset-file|8012|文件服务|file"
  "asset-payment|8013|支付服务|payment"
)

for SERVICE_DEF in "${SERVICES[@]}"; do
  IFS='|' read -r SVC_NAME SVC_PORT SVC_DESC SVC_PKG <<< "$SERVICE_DEF"
  SVC_DIR="${BACKEND_DIR}/${SVC_NAME}"

  if [ -d "$SVC_DIR/src" ]; then
    info "${SVC_NAME} 已存在，跳过"
    continue
  fi

  info "生成 ${SVC_NAME} (${SVC_DESC}, 端口:${SVC_PORT})..."

  # 目录结构
  PKG_PATH="${BASE_PKG//.//}/${SVC_PKG}"
  mkdir -p "${SVC_DIR}/src/main/java/${PKG_PATH}"/{controller,service/impl,mapper,domain/{entity,dto,vo,query}}
  mkdir -p "${SVC_DIR}/src/main/resources"/{mapper,db/migration}
  mkdir -p "${SVC_DIR}/src/test/java/${PKG_PATH}"

  # pom.xml
  CLASS_NAME="Asset$(echo "${SVC_PKG}" | sed 's/.*/\u&/')Application"
  cat > "${SVC_DIR}/pom.xml" << POMEOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-parent</artifactId><version>1.0.0-SNAPSHOT</version><relativePath>../asset-parent/pom.xml</relativePath></parent>
    <artifactId>${SVC_NAME}</artifactId>
    <description>${SVC_DESC}</description>
    <dependencies>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-security</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-mybatis</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-redis</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-log</artifactId></dependency>
        <dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId></dependency>
        <dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId></dependency>
        <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
        <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-mysql</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId><version>\${spring-boot.version}</version><executions><execution><goals><goal>repackage</goal></goals></execution></executions></plugin></plugins></build>
</project>
POMEOF

  # application.yml
  cat > "${SVC_DIR}/src/main/resources/application.yml" << YMLEOF
server:
  port: ${SVC_PORT}
spring:
  application:
    name: ${SVC_NAME}
  profiles:
    active: \${SPRING_PROFILES_ACTIVE:dev}
  cloud:
    nacos:
      discovery:
        server-addr: \${NACOS_ADDR:localhost:8848}
      config:
        server-addr: \${NACOS_ADDR:localhost:8848}
        file-extension: yml
        shared-configs:
          - data-id: application-common.yml
            group: DEFAULT_GROUP
            refresh: true
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://\${DB_HOST:localhost}:\${DB_PORT:3306}/asset_db?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: \${DB_USER:asset}
    password: \${DB_PWD:asset2026}
  data:
    redis:
      host: \${REDIS_HOST:localhost}
      port: \${REDIS_PORT:6379}
      password: \${REDIS_PWD:asset2026}
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
springdoc:
  api-docs:
    path: /v3/api-docs
  info:
    title: ${SVC_DESC} API
    version: 1.0.0
logging:
  level:
    com.asset: debug
YMLEOF

  # Main Application class
  cat > "${SVC_DIR}/src/main/java/${PKG_PATH}/${CLASS_NAME}.java" << JAVAEOF
package ${BASE_PKG}.${SVC_PKG};

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "${BASE_PKG}")
@EnableDiscoveryClient
@MapperScan("${BASE_PKG}.${SVC_PKG}.mapper")
public class ${CLASS_NAME} {
    public static void main(String[] args) {
        SpringApplication.run(${CLASS_NAME}.class, args);
    }
}
JAVAEOF

  ok "${SVC_NAME} → :${SVC_PORT}"
done

echo ""
echo "================================================================"
echo -e "${GREEN}✓ 全部业务模块脚手架生成完成${NC}"
echo "================================================================"
echo ""
echo "生成的服务:"
echo "  asset-base        :8001  基础数据服务 (已存在)"
for SERVICE_DEF in "${SERVICES[@]}"; do
  IFS='|' read -r SVC_NAME SVC_PORT SVC_DESC SVC_PKG <<< "$SERVICE_DEF"
  printf "  %-20s :%-5s %s\n" "$SVC_NAME" "$SVC_PORT" "$SVC_DESC"
done
echo ""
echo "下一步:"
echo "  1. 取消 asset-parent/pom.xml 中对应 <module> 的注释"
echo "  2. cd backend/asset-parent && mvn clean install -DskipTests"
echo ""
