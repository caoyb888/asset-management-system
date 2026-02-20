#!/bin/bash
#====================================================================
# 资产管理系统 - 项目骨架初始化脚本
# 生成 Maven 多模块后端 + Vue3 前端 + UniApp 移动端 + DevOps 配置
#====================================================================

set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_step()  { echo -e "\n${BLUE}===== $1 =====${NC}"; }

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_ROOT"

GROUP_ID="com.asset"
JAVA_VER="21"
SPRING_BOOT_VER="3.3.6"
SPRING_CLOUD_VER="2023.0.3"
SPRING_CLOUD_ALIBABA_VER="2023.0.1.2"
MYBATIS_PLUS_VER="3.5.9"
FLOWABLE_VER="7.1.0"
SPRINGDOC_VER="2.6.0"

# ============================================================
# 后端微服务模块列表
# ============================================================
COMMON_MODULES=("asset-common" "asset-common-security" "asset-common-mybatis" "asset-common-redis" "asset-common-log")
API_MODULES=("asset-api-base" "asset-api-system")
SERVICE_MODULES=("asset-base:8001" "asset-investment:8002" "asset-operation:8003" "asset-finance:8004" "asset-report:8005" "asset-system:8006" "asset-gis:8007" "asset-workflow:8010" "asset-message:8011" "asset-file:8012" "asset-payment:8013" "asset-gateway:9000")

# ============================================================
log_step "1/7 生成后端 Maven 父 POM"
# ============================================================

cat > pom.xml << 'PARENTPOM'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.asset</groupId>
    <artifactId>asset-management-system</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>资产管理系统</name>
    <description>企业级资产管理系统 - Spring Boot + Vue3 + UniApp</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.6</version>
        <relativePath/>
    </parent>

    <modules>
        <!-- 公共模块 -->
        <module>asset-common</module>
        <module>asset-common-security</module>
        <module>asset-common-mybatis</module>
        <module>asset-common-redis</module>
        <module>asset-common-log</module>
        <!-- Feign API 模块 -->
        <module>asset-api-base</module>
        <module>asset-api-system</module>
        <!-- 业务微服务 -->
        <module>asset-base</module>
        <module>asset-investment</module>
        <module>asset-operation</module>
        <module>asset-finance</module>
        <module>asset-report</module>
        <module>asset-system</module>
        <module>asset-gis</module>
        <module>asset-workflow</module>
        <module>asset-message</module>
        <module>asset-file</module>
        <module>asset-payment</module>
        <module>asset-gateway</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <!-- Spring Cloud -->
        <spring-cloud.version>2023.0.3</spring-cloud.version>
        <spring-cloud-alibaba.version>2023.0.1.2</spring-cloud-alibaba.version>
        <!-- 数据库 & ORM -->
        <mybatis-plus.version>3.5.9</mybatis-plus.version>
        <druid.version>1.2.23</druid.version>
        <shardingsphere.version>5.5.1</shardingsphere.version>
        <flyway.version>10.21.0</flyway.version>
        <!-- 工作流 -->
        <flowable.version>7.1.0</flowable.version>
        <!-- 安全 & 国密 -->
        <sa-token.version>1.39.0</sa-token.version>
        <bouncycastle.version>1.78.1</bouncycastle.version>
        <!-- API文档 -->
        <springdoc.version>2.6.0</springdoc.version>
        <!-- 工具库 -->
        <hutool.version>5.8.32</hutool.version>
        <mapstruct.version>1.6.3</mapstruct.version>
        <easyexcel.version>4.0.3</easyexcel.version>
        <!-- 中间件客户端 -->
        <redisson.version>3.37.0</redisson.version>
        <rocketmq-spring.version>2.3.1</rocketmq-spring.version>
        <minio.version>8.5.13</minio.version>
        <xxl-job.version>2.4.2</xxl-job.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- MyBatis-Plus -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            <!-- Druid 连接池 -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-3-starter</artifactId>
                <version>${druid.version}</version>
            </dependency>
            <!-- Flowable 工作流 -->
            <dependency>
                <groupId>org.flowable</groupId>
                <artifactId>flowable-spring-boot-starter</artifactId>
                <version>${flowable.version}</version>
            </dependency>
            <!-- SpringDoc OpenAPI -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>
            <!-- Hutool 工具库 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            <!-- MapStruct 对象映射 -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>
            <!-- EasyExcel -->
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>easyexcel</artifactId>
                <version>${easyexcel.version}</version>
            </dependency>
            <!-- Redisson -->
            <dependency>
                <groupId>org.redisson</groupId>
                <artifactId>redisson-spring-boot-starter</artifactId>
                <version>${redisson.version}</version>
            </dependency>
            <!-- MinIO -->
            <dependency>
                <groupId>io.minio</groupId>
                <artifactId>minio</artifactId>
                <version>${minio.version}</version>
            </dependency>
            <!-- XXL-Job -->
            <dependency>
                <groupId>com.xuxueli</groupId>
                <artifactId>xxl-job-core</artifactId>
                <version>${xxl-job.version}</version>
            </dependency>
            <!-- 国密 BouncyCastle -->
            <dependency>
                <groupId>org.bouncycastle</groupId>
                <artifactId>bcprov-jdk18on</artifactId>
                <version>${bouncycastle.version}</version>
            </dependency>
            <!-- Flyway 数据库迁移 -->
            <dependency>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-mysql</artifactId>
                <version>${flyway.version}</version>
            </dependency>

            <!-- 内部模块 -->
            <dependency><groupId>com.asset</groupId><artifactId>asset-common</artifactId><version>${project.version}</version></dependency>
            <dependency><groupId>com.asset</groupId><artifactId>asset-common-security</artifactId><version>${project.version}</version></dependency>
            <dependency><groupId>com.asset</groupId><artifactId>asset-common-mybatis</artifactId><version>${project.version}</version></dependency>
            <dependency><groupId>com.asset</groupId><artifactId>asset-common-redis</artifactId><version>${project.version}</version></dependency>
            <dependency><groupId>com.asset</groupId><artifactId>asset-common-log</artifactId><version>${project.version}</version></dependency>
            <dependency><groupId>com.asset</groupId><artifactId>asset-api-base</artifactId><version>${project.version}</version></dependency>
            <dependency><groupId>com.asset</groupId><artifactId>asset-api-system</artifactId><version>${project.version}</version></dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <annotationProcessorPaths>
                            <path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>${lombok.version}</version></path>
                            <path><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId><version>${mapstruct.version}</version></path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <repositories>
        <repository>
            <id>aliyun</id>
            <url>https://maven.aliyun.com/repository/public</url>
        </repository>
    </repositories>
</project>
PARENTPOM
log_info "父 POM 生成完成"

# ============================================================
log_step "2/7 生成公共模块骨架"
# ============================================================

# --- asset-common ---
mkdir -p asset-common/src/main/java/com/asset/common/{constant,enums,exception,model/{dto,vo,entity},util}
cat > asset-common/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-management-system</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>asset-common</artifactId>
    <description>公共工具/实体/常量/异常</description>
    <dependencies>
        <dependency><groupId>cn.hutool</groupId><artifactId>hutool-all</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
        <dependency><groupId>org.mapstruct</groupId><artifactId>mapstruct</artifactId></dependency>
        <dependency><groupId>com.fasterxml.jackson.core</groupId><artifactId>jackson-annotations</artifactId></dependency>
        <dependency><groupId>jakarta.validation</groupId><artifactId>jakarta.validation-api</artifactId></dependency>
    </dependencies>
</project>
EOF

# 统一返回结果
cat > asset-common/src/main/java/com/asset/common/model/R.java << 'JAVAEOF'
package com.asset.common.model;

import lombok.Data;
import java.io.Serializable;

/**
 * 统一响应结果
 */
@Data
public class R<T> implements Serializable {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        return fail(500, msg);
    }
}
JAVAEOF

# 全局异常
cat > asset-common/src/main/java/com/asset/common/exception/BizException.java << 'JAVAEOF'
package com.asset.common.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(500, message);
    }
}
JAVAEOF

# 分页请求
cat > asset-common/src/main/java/com/asset/common/model/dto/PageQuery.java << 'JAVAEOF'
package com.asset.common.model.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
public class PageQuery {
    @Min(1)
    private int pageNum = 1;
    @Min(1) @Max(500)
    private int pageSize = 20;
    private String orderBy;
    private String orderDirection = "asc";
}
JAVAEOF

log_info "asset-common 模块生成完成"

# --- asset-common-security ---
mkdir -p asset-common-security/src/main/java/com/asset/common/security/{config,filter,util,annotation}
cat > asset-common-security/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-management-system</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>asset-common-security</artifactId>
    <description>安全框架 - RBAC + 数据权限 + 国密SM2/SM3/SM4</description>
    <dependencies>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-redis</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.6</version></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
        <dependency><groupId>org.bouncycastle</groupId><artifactId>bcprov-jdk18on</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    </dependencies>
</project>
EOF
log_info "asset-common-security 模块生成完成"

# --- asset-common-mybatis ---
mkdir -p asset-common-mybatis/src/main/java/com/asset/common/mybatis/{config,handler,interceptor}
cat > asset-common-mybatis/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-management-system</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>asset-common-mybatis</artifactId>
    <description>数据库通用配置 - MyBatis-Plus + 多租户 + 数据权限拦截器</description>
    <dependencies>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common</artifactId></dependency>
        <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-spring-boot3-starter</artifactId></dependency>
        <dependency><groupId>com.alibaba</groupId><artifactId>druid-spring-boot-3-starter</artifactId></dependency>
        <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId></dependency>
        <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-mysql</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    </dependencies>
</project>
EOF
log_info "asset-common-mybatis 模块生成完成"

# --- asset-common-redis ---
mkdir -p asset-common-redis/src/main/java/com/asset/common/redis/{config,util}
cat > asset-common-redis/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-management-system</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>asset-common-redis</artifactId>
    <description>Redis通用配置 - 缓存 + 分布式锁</description>
    <dependencies>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common</artifactId></dependency>
        <dependency><groupId>org.redisson</groupId><artifactId>redisson-spring-boot-starter</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    </dependencies>
</project>
EOF
log_info "asset-common-redis 模块生成完成"

# --- asset-common-log ---
mkdir -p asset-common-log/src/main/java/com/asset/common/log/{annotation,aspect,event}
cat > asset-common-log/pom.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-management-system</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>asset-common-log</artifactId>
    <description>操作日志框架 - AOP + 注解 + 异步写入</description>
    <dependencies>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-aop</artifactId></dependency>
        <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
    </dependencies>
</project>
EOF
log_info "asset-common-log 模块生成完成"

# ============================================================
log_step "3/7 生成业务微服务模块骨架"
# ============================================================

generate_service_module() {
    local MODULE_NAME="$1"
    local PORT="$2"
    local PKG_NAME="${MODULE_NAME//asset-/}"
    PKG_NAME="${PKG_NAME//-/}"
    local CLASS_PREFIX=""

    case "$MODULE_NAME" in
        asset-base)       CLASS_PREFIX="Base"; local DESC="基础数据服务 - 项目/楼栋/商铺/品牌/商家" ;;
        asset-investment) CLASS_PREFIX="Investment"; local DESC="招商管理服务 - 意向/合同/变更/解约" ;;
        asset-operation)  CLASS_PREFIX="Operation"; local DESC="营运管理服务 - 台账/日常营运" ;;
        asset-finance)    CLASS_PREFIX="Finance"; local DESC="财务管理服务 - 应收/收款/核销/保证金" ;;
        asset-report)     CLASS_PREFIX="Report"; local DESC="报表管理服务 - 报表ETL + BI看板" ;;
        asset-system)     CLASS_PREFIX="System"; local DESC="系统管理服务 - 用户/角色/权限/字典" ;;
        asset-gis)        CLASS_PREFIX="Gis"; local DESC="GIS可视化服务 - 资产一张图看板" ;;
        asset-workflow)   CLASS_PREFIX="Workflow"; local DESC="工作流服务 - Flowable审批引擎" ;;
        asset-message)    CLASS_PREFIX="Message"; local DESC="消息服务 - 站内消息/Push/邮件/短信" ;;
        asset-file)       CLASS_PREFIX="File"; local DESC="文件服务 - MinIO上传/下载/预览" ;;
        asset-payment)    CLASS_PREFIX="Payment"; local DESC="支付服务 - 微信/支付宝集成" ;;
        asset-gateway)    CLASS_PREFIX="Gateway"; local DESC="网关服务 - API路由/鉴权/限流" ;;
    esac

    mkdir -p "$MODULE_NAME/src/main/java/com/asset/${PKG_NAME}"/{controller,service/impl,mapper,config}
    mkdir -p "$MODULE_NAME/src/main/resources/mapper"
    mkdir -p "$MODULE_NAME/src/test/java/com/asset/${PKG_NAME}"

    # POM
    if [[ "$MODULE_NAME" == "asset-gateway" ]]; then
        cat > "$MODULE_NAME/pom.xml" << GWEOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-management-system</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>${MODULE_NAME}</artifactId>
    <description>${DESC}</description>
    <dependencies>
        <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-gateway</artifactId></dependency>
        <dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId></dependency>
        <dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-sentinel</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-redis</artifactId></dependency>
    </dependencies>
    <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>
GWEOF
    elif [[ "$MODULE_NAME" == "asset-workflow" ]]; then
        cat > "$MODULE_NAME/pom.xml" << WFEOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-management-system</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>${MODULE_NAME}</artifactId>
    <description>${DESC}</description>
    <dependencies>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-security</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-mybatis</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-log</artifactId></dependency>
        <dependency><groupId>org.flowable</groupId><artifactId>flowable-spring-boot-starter</artifactId></dependency>
        <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>
WFEOF
    else
        cat > "$MODULE_NAME/pom.xml" << SVCEOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-management-system</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>${MODULE_NAME}</artifactId>
    <description>${DESC}</description>
    <dependencies>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-security</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-mybatis</artifactId></dependency>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common-log</artifactId></dependency>
        <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId></dependency>
        <dependency><groupId>com.alibaba.cloud</groupId><artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId></dependency>
        <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
        <dependency><groupId>com.alibaba</groupId><artifactId>easyexcel</artifactId></dependency>
        <dependency><groupId>com.xuxueli</groupId><artifactId>xxl-job-core</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>
SVCEOF
    fi

    # Application 启动类
    cat > "$MODULE_NAME/src/main/java/com/asset/${PKG_NAME}/${CLASS_PREFIX}Application.java" << APPEOF
package com.asset.${PKG_NAME};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.asset")
@EnableDiscoveryClient
public class ${CLASS_PREFIX}Application {
    public static void main(String[] args) {
        SpringApplication.run(${CLASS_PREFIX}Application.class, args);
    }
}
APPEOF

    # application.yml
    cat > "$MODULE_NAME/src/main/resources/application.yml" << YMLEOF
server:
  port: ${PORT}

spring:
  application:
    name: ${MODULE_NAME}
  profiles:
    active: dev
  cloud:
    nacos:
      discovery:
        server-addr: \${NACOS_ADDR:127.0.0.1:8848}
        namespace: \${NACOS_NAMESPACE:dev}
      config:
        server-addr: \${NACOS_ADDR:127.0.0.1:8848}
        namespace: \${NACOS_NAMESPACE:dev}
        file-extension: yml
        shared-configs:
          - data-id: application-common.yml
            group: DEFAULT_GROUP
            refresh: true

# SpringDoc API文档
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
YMLEOF

    # application-dev.yml
    cat > "$MODULE_NAME/src/main/resources/application-dev.yml" << DEVEOF
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://\${MYSQL_HOST:127.0.0.1}:\${MYSQL_PORT:3306}/asset_db?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: \${MYSQL_USER:root}
    password: \${MYSQL_PASS:root123}
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20

  data:
    redis:
      host: \${REDIS_HOST:127.0.0.1}
      port: \${REDIS_PORT:6379}
      password: \${REDIS_PASS:}
      database: 0

logging:
  level:
    com.asset: debug
    org.flowable: info
DEVEOF

    # Dockerfile
    cat > "$MODULE_NAME/Dockerfile" << DKEOF
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="asset-team"
RUN apk add --no-cache tzdata && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
WORKDIR /app
COPY target/${MODULE_NAME}-*.jar app.jar
EXPOSE ${PORT}
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseZGC"
ENTRYPOINT ["sh", "-c", "java \$JAVA_OPTS -jar app.jar"]
DKEOF

    log_info "  ${MODULE_NAME} (端口:${PORT}) 生成完成"
}

# API 模块
for api_mod in "${API_MODULES[@]}"; do
    PKG="${api_mod//asset-api-/}"
    mkdir -p "$api_mod/src/main/java/com/asset/api/${PKG}"
    cat > "$api_mod/pom.xml" << APIEOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent><groupId>com.asset</groupId><artifactId>asset-management-system</artifactId><version>1.0.0-SNAPSHOT</version></parent>
    <artifactId>${api_mod}</artifactId>
    <description>Feign API接口定义</description>
    <dependencies>
        <dependency><groupId>com.asset</groupId><artifactId>asset-common</artifactId></dependency>
        <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-openfeign</artifactId></dependency>
    </dependencies>
</project>
APIEOF
    log_info "  ${api_mod} Feign接口模块生成完成"
done

# 生成业务微服务
for svc in "${SERVICE_MODULES[@]}"; do
    IFS=':' read -r name port <<< "$svc"
    generate_service_module "$name" "$port"
done

# ============================================================
log_step "4/7 生成 Flyway 数据库迁移脚本"
# ============================================================

mkdir -p asset-system/src/main/resources/db/migration
cat > asset-system/src/main/resources/db/migration/V1.0.0__init_schema.sql << 'SQLEOF'
-- ====================================================================
-- 资产管理系统 - V1.0.0 初始化数据库
-- ====================================================================

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    tenant_id       BIGINT          DEFAULT 0               COMMENT '租户ID',
    username        VARCHAR(50)     NOT NULL                 COMMENT '用户名',
    password        VARCHAR(200)    NOT NULL                 COMMENT '密码(SM3加密)',
    real_name       VARCHAR(50)     DEFAULT ''               COMMENT '真实姓名',
    phone           VARCHAR(20)     DEFAULT ''               COMMENT '手机号(SM4加密)',
    email           VARCHAR(100)    DEFAULT ''               COMMENT '邮箱',
    avatar          VARCHAR(500)    DEFAULT ''               COMMENT '头像',
    dept_id         BIGINT          DEFAULT NULL             COMMENT '部门ID',
    status          TINYINT         DEFAULT 1                COMMENT '状态(1正常 0禁用)',
    login_ip        VARCHAR(128)    DEFAULT ''               COMMENT '最后登录IP',
    login_time      DATETIME        DEFAULT NULL             COMMENT '最后登录时间',
    create_by       VARCHAR(50)     DEFAULT ''               COMMENT '创建者',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       VARCHAR(50)     DEFAULT ''               COMMENT '更新者',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0                COMMENT '删除标记(0正常 1删除)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, tenant_id),
    KEY idx_tenant (tenant_id),
    KEY idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- 2. 角色表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '角色ID',
    tenant_id       BIGINT          DEFAULT 0               COMMENT '租户ID',
    role_name       VARCHAR(50)     NOT NULL                 COMMENT '角色名称',
    role_code       VARCHAR(50)     NOT NULL                 COMMENT '角色编码',
    data_scope      TINYINT         DEFAULT 1                COMMENT '数据权限(1全部 2自定义 3本部门 4本部门及以下 5本人)',
    sort_order      INT             DEFAULT 0                COMMENT '排序',
    status          TINYINT         DEFAULT 1                COMMENT '状态',
    remark          VARCHAR(500)    DEFAULT ''               COMMENT '备注',
    create_by       VARCHAR(50)     DEFAULT ''               COMMENT '创建者',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(50)     DEFAULT '',
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT         DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ----------------------------
-- 3. 菜单/权限表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_menu (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '菜单ID',
    parent_id       BIGINT          DEFAULT 0               COMMENT '父菜单ID',
    menu_name       VARCHAR(100)    NOT NULL                 COMMENT '菜单名称',
    menu_type       CHAR(1)         NOT NULL                 COMMENT '类型(M目录 C菜单 F按钮)',
    path            VARCHAR(200)    DEFAULT ''               COMMENT '路由地址',
    component       VARCHAR(255)    DEFAULT ''               COMMENT '组件路径',
    perms           VARCHAR(200)    DEFAULT ''               COMMENT '权限标识',
    icon            VARCHAR(100)    DEFAULT ''               COMMENT '图标',
    sort_order      INT             DEFAULT 0                COMMENT '排序',
    visible         TINYINT         DEFAULT 1                COMMENT '是否可见',
    status          TINYINT         DEFAULT 1                COMMENT '状态',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单权限表';

-- ----------------------------
-- 4. 用户角色关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- 5. 角色菜单关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id         BIGINT          NOT NULL COMMENT '角色ID',
    menu_id         BIGINT          NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ----------------------------
-- 6. 字典类型表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    dict_name       VARCHAR(100)    NOT NULL COMMENT '字典名称',
    dict_type       VARCHAR(100)    NOT NULL COMMENT '字典类型',
    status          TINYINT         DEFAULT 1,
    remark          VARCHAR(500)    DEFAULT '',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- ----------------------------
-- 7. 字典数据表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_dict_data (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    dict_type       VARCHAR(100)    NOT NULL COMMENT '字典类型',
    dict_label      VARCHAR(100)    NOT NULL COMMENT '字典标签',
    dict_value      VARCHAR(100)    NOT NULL COMMENT '字典值',
    sort_order      INT             DEFAULT 0,
    status          TINYINT         DEFAULT 1,
    remark          VARCHAR(500)    DEFAULT '',
    create_time     DATETIME        DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ----------------------------
-- 8. 操作日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_oper_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    module          VARCHAR(50)     DEFAULT '' COMMENT '模块',
    biz_type        VARCHAR(50)     DEFAULT '' COMMENT '业务类型',
    method          VARCHAR(200)    DEFAULT '' COMMENT '方法名',
    request_method  VARCHAR(10)     DEFAULT '' COMMENT 'HTTP方法',
    request_url     VARCHAR(500)    DEFAULT '' COMMENT '请求URL',
    request_param   TEXT                       COMMENT '请求参数',
    response_result TEXT                       COMMENT '响应结果',
    oper_user       VARCHAR(50)     DEFAULT '' COMMENT '操作人',
    oper_ip         VARCHAR(128)    DEFAULT '' COMMENT '操作IP',
    status          TINYINT         DEFAULT 1  COMMENT '状态(1成功 0失败)',
    error_msg       TEXT                       COMMENT '错误消息',
    cost_time       BIGINT          DEFAULT 0  COMMENT '耗时(ms)',
    oper_time       DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_oper_time (oper_time),
    KEY idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------
-- 初始数据: 超级管理员
-- ----------------------------
INSERT INTO sys_user (id, username, password, real_name, status) VALUES
(1, 'admin', '$SM3$salt$hash_placeholder', '超级管理员', 1);

INSERT INTO sys_role (id, role_name, role_code, data_scope) VALUES
(1, '超级管理员', 'admin', 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
SQLEOF
log_info "Flyway V1.0.0 初始化SQL生成完成"

# ============================================================
log_step "5/7 生成 Docker Compose 开发环境配置"
# ============================================================

cat > docker-compose.yml << 'DCEOF'
# ====================================================================
# 资产管理系统 - 开发环境 Docker Compose
# 用法: docker compose up -d
# ====================================================================
version: "3.9"

services:
  # ======================== 基础设施 ========================
  mysql:
    image: mysql:8.0
    container_name: asset-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: asset_db
      MYSQL_CHARSET: utf8mb4
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./deploy/mysql/init:/docker-entrypoint-initdb.d
    command: >
      --default-authentication-plugin=caching_sha2_password
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --lower-case-table-names=1
      --max-connections=500
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: asset-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

  nacos:
    image: nacos/nacos-server:v2.4.3
    container_name: asset-nacos
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_DB_NAME: nacos_config
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: root123
      JVM_XMS: 256m
      JVM_XMX: 256m
    ports:
      - "8848:8848"
      - "9848:9848"
    depends_on:
      mysql:
        condition: service_healthy

  # ======================== 中间件 ========================
  minio:
    image: minio/minio:latest
    container_name: asset-minio
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: admin123456
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"

  elasticsearch:
    image: elasticsearch:8.15.3
    container_name: asset-es
    environment:
      discovery.type: single-node
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"
      xpack.security.enabled: "false"
    ports:
      - "9200:9200"
    volumes:
      - es_data:/usr/share/elasticsearch/data

  # ======================== 开发工具 ========================
  traefik:
    image: traefik:v3.2
    container_name: asset-traefik
    command:
      - "--api.insecure=true"
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--entrypoints.web.address=:80"
    ports:
      - "80:80"
      - "8080:8080"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro

  # ======================== 监控 ========================
  prometheus:
    image: prom/prometheus:latest
    container_name: asset-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./deploy/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus

  grafana:
    image: grafana/grafana:latest
    container_name: asset-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin123
    volumes:
      - grafana_data:/var/lib/grafana

volumes:
  mysql_data:
  redis_data:
  minio_data:
  es_data:
  prometheus_data:
  grafana_data:

networks:
  default:
    name: asset-network
DCEOF

# Prometheus 配置
mkdir -p deploy/prometheus
cat > deploy/prometheus/prometheus.yml << 'PROMEOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'asset-services'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
        - 'host.docker.internal:8001'
        - 'host.docker.internal:8002'
        - 'host.docker.internal:8003'
        - 'host.docker.internal:8004'
        - 'host.docker.internal:8005'
        - 'host.docker.internal:8006'
        - 'host.docker.internal:8007'
        - 'host.docker.internal:9000'
PROMEOF

# Nacos初始化SQL
mkdir -p deploy/mysql/init
cat > deploy/mysql/init/01-create-nacos-db.sql << 'NACOS_SQL'
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4;
NACOS_SQL

log_info "Docker Compose 开发环境配置生成完成"

# ============================================================
log_step "6/7 生成前端 Vue3 项目配置"
# ============================================================

mkdir -p asset-ui/{src/{api,views,components,store,router,directives,utils/sm-crypto,styles,layouts},e2e,public}

# package.json
cat > asset-ui/package.json << 'PKGEOF'
{
  "name": "asset-management-ui",
  "version": "1.0.0",
  "private": true,
  "type": "module",
  "scripts": {
    "dev": "vite --host",
    "build": "vue-tsc --noEmit && vite build",
    "preview": "vite preview",
    "lint": "eslint src --ext .ts,.tsx,.vue --fix",
    "type-check": "vue-tsc --noEmit",
    "test:unit": "vitest run",
    "test:e2e": "playwright test",
    "api:generate": "openapi-generator-cli generate -i http://localhost:9000/v3/api-docs -g typescript-axios -o src/api/generated --additional-properties=supportsES6=true"
  },
  "dependencies": {
    "vue": "^3.5.13",
    "vue-router": "^4.4.5",
    "pinia": "^2.2.6",
    "element-plus": "^2.8.8",
    "axios": "^1.7.9",
    "echarts": "^5.5.1",
    "three": "^0.170.0",
    "@amap/amap-jsapi-loader": "^1.0.1",
    "sm-crypto": "^0.3.13",
    "dayjs": "^1.11.13",
    "nprogress": "^0.2.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.2.1",
    "vite": "^6.0.3",
    "vue-tsc": "^2.1.10",
    "typescript": "~5.6.3",
    "@vue/tsconfig": "^0.5.1",
    "eslint": "^9.16.0",
    "@typescript-eslint/eslint-plugin": "^8.17.0",
    "eslint-plugin-vue": "^9.31.0",
    "prettier": "^3.4.2",
    "@playwright/test": "^1.49.1",
    "vitest": "^2.1.8",
    "unplugin-auto-import": "^0.18.6",
    "unplugin-vue-components": "^0.27.5",
    "@openapitools/openapi-generator-cli": "^2.15.3",
    "sass": "^1.82.0"
  }
}
PKGEOF

# tsconfig.json
cat > asset-ui/tsconfig.json << 'TSEOF'
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "noImplicitAny": true,
    "jsx": "preserve",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "esModuleInterop": true,
    "lib": ["ES2022", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "noEmit": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    },
    "types": ["vite/client", "element-plus/global"]
  },
  "include": ["src/**/*.ts", "src/**/*.tsx", "src/**/*.vue", "e2e/**/*.ts"],
  "exclude": ["node_modules", "dist"]
}
TSEOF

# vite.config.ts
cat > asset-ui/vite.config.ts << 'VITEEOF'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3100,
    proxy: {
      '/api': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
    },
  },
  build: {
    target: 'es2022',
    chunkSizeWarningLimit: 2000,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          elementPlus: ['element-plus'],
          echarts: ['echarts'],
          three: ['three'],
        },
      },
    },
  },
})
VITEEOF

# Playwright E2E 配置
cat > asset-ui/playwright.config.ts << 'PWEOF'
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['html', { open: 'never' }]],
  use: {
    baseURL: 'http://localhost:3100',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    command: 'pnpm dev',
    port: 3100,
    reuseExistingServer: !process.env.CI,
  },
})
PWEOF

# 示例 E2E 测试
cat > asset-ui/e2e/login.spec.ts << 'E2EEOF'
import { test, expect } from '@playwright/test'

test.describe('登录流程', () => {
  test('管理员正常登录', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[data-testid="username"]', 'admin')
    await page.fill('[data-testid="password"]', 'admin123')
    await page.click('[data-testid="login-btn"]')
    await expect(page).toHaveURL(/dashboard/)
    await expect(page.locator('.user-name')).toContainText('超级管理员')
  })

  test('密码错误提示', async ({ page }) => {
    await page.goto('/login')
    await page.fill('[data-testid="username"]', 'admin')
    await page.fill('[data-testid="password"]', 'wrongpass')
    await page.click('[data-testid="login-btn"]')
    await expect(page.locator('.el-message--error')).toBeVisible()
  })
})
E2EEOF

log_info "前端 Vue3+TS 项目配置生成完成"

# ============================================================
log_step "7/7 生成 CI/CD + GitLab CI 配置"
# ============================================================

# .gitlab-ci.yml
cat > .gitlab-ci.yml << 'CIEOF'
# ====================================================================
# 资产管理系统 - GitLab CI/CD 流水线
# 阶段: 检查 → 测试 → API文档 → 构建 → 部署
# ====================================================================

stages:
  - check
  - test
  - api-docs
  - build
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
  DOCKER_REGISTRY: "registry.cn-hangzhou.aliyuncs.com/asset-system"

cache:
  key: "${CI_COMMIT_REF_SLUG}"
  paths:
    - .m2/repository
    - asset-ui/node_modules
    - asset-ui/.pnpm-store

# -------------------- 代码检查 --------------------
backend-lint:
  stage: check
  image: eclipse-temurin:21-jdk
  script:
    - mvn checkstyle:check spotbugs:check -q
  only:
    changes:
      - "asset-*/**/*.java"
      - "pom.xml"

frontend-lint:
  stage: check
  image: node:20-alpine
  script:
    - cd asset-ui && pnpm install --frozen-lockfile && pnpm lint && pnpm type-check
  only:
    changes:
      - "asset-ui/**"

# -------------------- 单元测试 --------------------
backend-test:
  stage: test
  image: eclipse-temurin:21-jdk
  services:
    - mysql:8.0
    - redis:7-alpine
  variables:
    MYSQL_ROOT_PASSWORD: test123
    MYSQL_DATABASE: asset_test
  script:
    - mvn test -pl asset-common,asset-common-security,asset-common-mybatis,asset-system -q
  artifacts:
    reports:
      junit: "**/target/surefire-reports/TEST-*.xml"

frontend-test:
  stage: test
  image: node:20-alpine
  script:
    - cd asset-ui && pnpm install --frozen-lockfile && pnpm test:unit
  only:
    changes:
      - "asset-ui/src/**"

# -------------------- API文档生成 --------------------
api-docs-generate:
  stage: api-docs
  image: eclipse-temurin:21-jdk
  script:
    # 启动gateway获取聚合API文档
    - mvn spring-boot:run -pl asset-gateway -Dspring-boot.run.profiles=ci &
    - sleep 30
    - curl -f http://localhost:9000/v3/api-docs > api-docs.json
    # 检测破坏性变更
    - npx oasdiff breaking api-docs-prev.json api-docs.json || echo "WARNING: Breaking API changes detected"
    - cp api-docs.json api-docs-prev.json
  artifacts:
    paths:
      - api-docs.json
      - api-docs-prev.json
  only:
    changes:
      - "asset-*/src/main/java/**/controller/**"

api-client-generate:
  stage: api-docs
  image: node:20-alpine
  needs: ["api-docs-generate"]
  script:
    - cd asset-ui
    - npx @openapitools/openapi-generator-cli generate -i ../api-docs.json -g typescript-axios -o src/api/generated --additional-properties=supportsES6=true
  artifacts:
    paths:
      - asset-ui/src/api/generated/

# -------------------- E2E测试 --------------------
e2e-test:
  stage: test
  image: mcr.microsoft.com/playwright:v1.49.1-jammy
  needs: ["frontend-test"]
  script:
    - cd asset-ui && pnpm install --frozen-lockfile && npx playwright test
  artifacts:
    when: always
    paths:
      - asset-ui/playwright-report/
  only:
    refs:
      - develop
      - main

# -------------------- 构建镜像 --------------------
build-backend:
  stage: build
  image: docker:24
  services:
    - docker:24-dind
  script:
    - mvn package -DskipTests -q
    - |
      for svc in asset-base asset-investment asset-operation asset-finance asset-report asset-system asset-gis asset-workflow asset-message asset-file asset-payment asset-gateway; do
        docker build -t $DOCKER_REGISTRY/$svc:$CI_COMMIT_SHORT_SHA ./$svc
        docker push $DOCKER_REGISTRY/$svc:$CI_COMMIT_SHORT_SHA
      done
  only:
    refs:
      - main

build-frontend:
  stage: build
  image: node:20-alpine
  script:
    - cd asset-ui && pnpm install --frozen-lockfile && pnpm build
    - docker build -t $DOCKER_REGISTRY/asset-ui:$CI_COMMIT_SHORT_SHA ./asset-ui
    - docker push $DOCKER_REGISTRY/asset-ui:$CI_COMMIT_SHORT_SHA
  only:
    refs:
      - main

# -------------------- 部署 --------------------
deploy-dev:
  stage: deploy
  script:
    - cd deploy && docker compose pull && docker compose up -d
  environment:
    name: development
  only:
    refs:
      - develop

deploy-prod:
  stage: deploy
  script:
    - kubectl set image deployment/asset-gateway asset-gateway=$DOCKER_REGISTRY/asset-gateway:$CI_COMMIT_SHORT_SHA -n asset-prod
    # ... 其他服务同理
  environment:
    name: production
  when: manual
  only:
    refs:
      - main
CIEOF

# .gitignore
cat > .gitignore << 'GIEOF'
# Java
target/
*.class
*.jar
*.log
.mvn/wrapper/maven-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
.project
.classpath
.settings/

# Node
node_modules/
dist/
.pnpm-store/

# Docker
docker-compose.override.yml

# OS
.DS_Store
Thumbs.db

# Env
.env.local
.env.*.local
*.env

# Test
playwright-report/
test-results/
coverage/
GIEOF

# README
cat > README.md << 'READMEEOF'
# 资产管理系统

企业级资产管理系统，基于 Spring Boot + Vue3 + UniApp + Docker/K8s 全栈架构。

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端 | Spring Boot 3.3 + MyBatis-Plus + Flowable |
| 前端 | Vue3 + TypeScript + Element Plus + ECharts |
| 移动端 | UniApp (iOS/Android/H5/小程序) |
| 数据库 | MySQL 8.0 / 达梦DM8 + Redis 7 + ES 8 |
| 部署 | Docker + K8s + GitLab CI |

## 快速开始

```bash
# 1. 环境初始化 (Ubuntu 24.04)
sudo bash init-env.sh --dev

# 2. 启动基础设施
docker compose up -d

# 3. 后端编译
mvn clean package -DskipTests

# 4. 前端启动
cd asset-ui && pnpm install && pnpm dev
```

## 项目结构

```
asset-management-system/
├── asset-common/              # 公共工具/实体
├── asset-common-security/     # 安全框架(RBAC+国密)
├── asset-common-mybatis/      # 数据库配置(MP+多租户)
├── asset-common-redis/        # Redis配置
├── asset-common-log/          # 日志框架
├── asset-api-*/               # Feign接口定义
├── asset-base/                # 基础数据服务 :8001
├── asset-investment/          # 招商管理服务 :8002
├── asset-operation/           # 营运管理服务 :8003
├── asset-finance/             # 财务管理服务 :8004
├── asset-report/              # 报表管理服务 :8005
├── asset-system/              # 系统管理服务 :8006
├── asset-gis/                 # GIS可视化服务 :8007
├── asset-workflow/            # 工作流服务   :8010
├── asset-message/             # 消息服务     :8011
├── asset-file/                # 文件服务     :8012
├── asset-payment/             # 支付服务     :8013
├── asset-gateway/             # 网关服务     :9000
├── asset-ui/                  # Vue3前端
├── docker-compose.yml         # 开发环境
├── deploy/                    # 部署配置
└── .gitlab-ci.yml             # CI/CD流水线
```
READMEEOF

log_info "CI/CD + Git配置生成完成"

# ============================================================
echo ""
echo "============================================"
echo "  项目骨架生成完成!"
echo "============================================"
echo ""
echo "  后端模块: 5 公共 + 2 API + 12 微服务 = 19 个Maven模块"
echo "  前端项目: asset-ui (Vue3 + TypeScript + Playwright)"
echo "  数据库:   Flyway V1.0.0 初始化脚本"
echo "  基础设施: Docker Compose (MySQL+Redis+Nacos+MinIO+ES+Traefik+Prometheus+Grafana)"
echo "  CI/CD:    GitLab CI 8阶段流水线"
echo ""
echo "  启动步骤:"
echo "    1. docker compose up -d        # 启动基础设施"
echo "    2. mvn clean package -DskipTests # 编译后端"
echo "    3. cd asset-ui && pnpm install && pnpm dev  # 启动前端"
echo "============================================"
