#!/bin/bash
#=============================================================================
# 资产管理系统 - Ubuntu 24.04 环境初始化脚本
# 技术栈: Spring Boot 3.3 + Vue3 + UniApp + Docker/K8s
#=============================================================================
set -euo pipefail

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()  { echo -e "${BLUE}[INFO]${NC} $1"; }
ok()    { echo -e "${GREEN}[ OK ]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }

JAVA_VERSION="17"
NODE_VERSION="20"
MAVEN_VERSION="3.9.9"

echo ""
echo "  ╔══════════════════════════════════════════╗"
echo "  ║   资产管理系统 - 环境初始化 (Ubuntu 24.04) ║"
echo "  ╚══════════════════════════════════════════╝"
echo ""

# ---------- 1. 系统基础 ----------
info "1/8  安装系统基础包..."
sudo apt-get update -qq
sudo apt-get install -y -qq curl wget git unzip zip gnupg lsb-release \
    ca-certificates apt-transport-https software-properties-common \
    build-essential net-tools jq tree > /dev/null 2>&1
ok "系统基础包"

# ---------- 2. JDK 17 ----------
info "2/8  安装 OpenJDK ${JAVA_VERSION}..."
if ! java -version 2>&1 | grep -q "\"${JAVA_VERSION}"; then
    sudo apt-get install -y -qq openjdk-${JAVA_VERSION}-jdk > /dev/null 2>&1
fi
JAVA_HOME_PATH="/usr/lib/jvm/java-${JAVA_VERSION}-openjdk-$(dpkg --print-architecture)"
grep -q "JAVA_HOME" ~/.bashrc 2>/dev/null || cat >> ~/.bashrc << EOF

# === Asset Management System ===
export JAVA_HOME=${JAVA_HOME_PATH}
export PATH=\$JAVA_HOME/bin:\$PATH
EOF
export JAVA_HOME="${JAVA_HOME_PATH}" && export PATH="$JAVA_HOME/bin:$PATH"
ok "JDK $(java -version 2>&1 | head -1 | awk -F'"' '{print $2}')"

# ---------- 3. Maven ----------
info "3/8  安装 Maven ${MAVEN_VERSION}..."
if ! command -v mvn &>/dev/null; then
    wget -q "https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -O /tmp/maven.tar.gz
    sudo tar -xzf /tmp/maven.tar.gz -C /opt/ && rm /tmp/maven.tar.gz
    sudo ln -sf /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn
fi
ok "Maven $(mvn -version 2>&1 | head -1 | awk '{print $3}')"

# Maven settings (阿里云镜像)
mkdir -p ~/.m2
[ -f ~/.m2/settings.xml ] || cat > ~/.m2/settings.xml << 'XML'
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id><mirrorOf>central</mirrorOf>
      <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>jdk17</id>
      <activation><activeByDefault>true</activeByDefault></activation>
      <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
      </properties>
    </profile>
  </profiles>
</settings>
XML

# ---------- 4. Node.js + pnpm ----------
info "4/8  安装 Node.js ${NODE_VERSION}.x + pnpm..."
if ! command -v node &>/dev/null; then
    curl -fsSL https://deb.nodesource.com/setup_${NODE_VERSION}.x | sudo -E bash - > /dev/null 2>&1
    sudo apt-get install -y -qq nodejs > /dev/null 2>&1
fi
command -v pnpm &>/dev/null || sudo npm i -g pnpm > /dev/null 2>&1
npm config set registry https://registry.npmmirror.com 2>/dev/null
pnpm config set registry https://registry.npmmirror.com 2>/dev/null || true
ok "Node $(node -v), pnpm $(pnpm -v 2>/dev/null)"

# ---------- 5. Docker ----------
info "5/8  安装 Docker..."
if ! command -v docker &>/dev/null; then
    curl -fsSL https://get.docker.com | sudo sh > /dev/null 2>&1
    sudo usermod -aG docker "$USER"
fi
ok "Docker $(docker --version 2>/dev/null | awk '{print $3}' | tr -d ',')"

# ---------- 6. openapi-generator (吸收实践: API客户端自动生成) ----------
info "6/8  安装 openapi-generator-cli..."
command -v openapi-generator-cli &>/dev/null || sudo npm i -g @openapitools/openapi-generator-cli > /dev/null 2>&1
ok "openapi-generator-cli  ← [吸收实践]"

# ---------- 7. Playwright deps (吸收实践: E2E测试) ----------
info "7/8  安装 Playwright 系统依赖..."
sudo npx -y playwright install-deps > /dev/null 2>&1 || warn "Playwright依赖安装失败，可后续手动安装"
ok "Playwright 系统依赖  ← [吸收实践]"

# ---------- 8. 辅助工具 ----------
info "8/8  安装辅助工具..."
sudo apt-get install -y -qq redis-tools mysql-client > /dev/null 2>&1 || true
ok "redis-cli, mysql-client"

echo ""
echo "  ╔══════════════════════════════════════════╗"
echo -e "  ║  ${GREEN}✓ 环境初始化完成${NC}                         ║"
echo "  ╠══════════════════════════════════════════╣"
echo "  ║  吸收的优秀实践:                          ║"
echo "  ║    ✓ openapi-generator (API客户端生成)     ║"
echo "  ║    ✓ Playwright (E2E自动化测试)            ║"
echo "  ║    ✓ Docker Compose+Traefik (开发部署)     ║"
echo "  ╚══════════════════════════════════════════╝"
echo ""
echo -e "  ${YELLOW}下一步: source ~/.bashrc && cd docker/dev && docker compose up -d${NC}"
echo ""
