#!/bin/bash
#====================================================================
# 资产管理系统 - Ubuntu 24.04 环境初始化脚本
# 技术栈: Spring Boot + Vue3 + UniApp + Docker/K8s
# 用法: sudo bash init-env.sh [--dev|--prod]
#====================================================================

set -euo pipefail

# ======================== 颜色输出 ========================
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
log_info()  { echo -e "${GREEN}[INFO]${NC}  $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "\n${BLUE}===== $1 =====${NC}"; }

# ======================== 参数解析 ========================
ENV_MODE="${1:---dev}"
if [[ "$ENV_MODE" != "--dev" && "$ENV_MODE" != "--prod" ]]; then
    echo "用法: sudo bash init-env.sh [--dev|--prod]"
    echo "  --dev   开发环境(默认): 安装全部开发工具"
    echo "  --prod  生产环境: 仅安装运行时依赖"
    exit 1
fi
log_info "环境模式: $ENV_MODE"

# ======================== 系统检查 ========================
log_step "1/8 系统检查"
if [[ $EUID -ne 0 ]]; then
    log_error "请使用 sudo 运行此脚本"
    exit 1
fi
source /etc/os-release
if [[ "$VERSION_ID" != "24.04" ]]; then
    log_warn "当前系统: $PRETTY_NAME, 推荐 Ubuntu 24.04 LTS"
fi
log_info "系统: $PRETTY_NAME | 内核: $(uname -r) | 架构: $(uname -m)"

# ======================== 基础包 ========================
log_step "2/8 安装基础依赖"
apt-get update -qq
apt-get install -y -qq \
    curl wget git vim unzip zip \
    ca-certificates gnupg lsb-release \
    build-essential software-properties-common \
    apt-transport-https \
    net-tools htop tree jq \
    fontconfig locales \
    > /dev/null 2>&1
locale-gen zh_CN.UTF-8 > /dev/null 2>&1
log_info "基础依赖安装完成"

# ======================== JDK 21 ========================
log_step "3/8 安装 JDK 21 (Eclipse Temurin)"
if ! java -version 2>&1 | grep -q "21"; then
    wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /usr/share/keyrings/adoptium.gpg 2>/dev/null
    echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" > /etc/apt/sources.list.d/adoptium.list
    apt-get update -qq
    apt-get install -y -qq temurin-21-jdk > /dev/null 2>&1
    log_info "JDK 21 安装完成: $(java -version 2>&1 | head -1)"
else
    log_info "JDK 21 已安装: $(java -version 2>&1 | head -1)"
fi

# 设置 JAVA_HOME
cat > /etc/profile.d/java.sh << 'EOF'
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
EOF
source /etc/profile.d/java.sh

# ======================== Maven 3.9 ========================
log_step "4/8 安装 Maven 3.9"
MAVEN_VERSION="3.9.9"
if ! mvn -version 2>&1 | grep -q "$MAVEN_VERSION"; then
	wget "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" -O /tmp/maven.tar.gz
    tar -xzf /tmp/maven.tar.gz -C /opt/
    ln -sf /opt/apache-maven-${MAVEN_VERSION} /opt/maven
    cat > /etc/profile.d/maven.sh << 'EOF'
export MAVEN_HOME=/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH
EOF
    source /etc/profile.d/maven.sh
    rm -f /tmp/maven.tar.gz
    log_info "Maven 安装完成: $(mvn -version 2>&1 | head -1)"
else
    log_info "Maven 已安装: $(mvn -version 2>&1 | head -1)"
fi

# ======================== Node.js 20 LTS ========================
log_step "5/8 安装 Node.js 20 LTS"
if ! node -v 2>&1 | grep -q "v20"; then
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash - > /dev/null 2>&1
    apt-get install -y -qq nodejs > /dev/null 2>&1
    # 配置npm镜像
    npm config set registry https://registry.npmmirror.com
    # 安装全局工具
    npm install -g pnpm@latest > /dev/null 2>&1
    pnpm config set registry https://registry.npmmirror.com
    log_info "Node.js 安装完成: $(node -v) | pnpm: $(pnpm -v)"
else
    log_info "Node.js 已安装: $(node -v)"
fi

# ======================== Docker + Compose ========================
log_step "6/8 安装 Docker"
if ! docker -v &>/dev/null; then
    install -m 0755 -d /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    chmod a+r /etc/apt/keyrings/docker.asc
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list
    apt-get update -qq
    apt-get install -y -qq docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin > /dev/null 2>&1
    systemctl enable docker
    systemctl start docker
    # 添加当前用户到docker组
    REAL_USER="${SUDO_USER:-$USER}"
    usermod -aG docker "$REAL_USER" 2>/dev/null || true
    log_info "Docker 安装完成: $(docker -v)"
else
    log_info "Docker 已安装: $(docker -v)"
fi

# ======================== 开发环境额外工具 ========================
if [[ "$ENV_MODE" == "--dev" ]]; then
    log_step "7/8 安装开发环境工具"

    # openapi-generator-cli (API客户端自动生成)
    npm install -g @openapitools/openapi-generator-cli > /dev/null 2>&1
    log_info "openapi-generator-cli 已安装"

    # Playwright (E2E测试)
    npx -y playwright install-deps > /dev/null 2>&1
    log_info "Playwright 系统依赖已安装"

    # kubectl (K8s CLI)
    if ! kubectl version --client &>/dev/null; then
        curl -fsSLo /usr/local/bin/kubectl "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
        chmod +x /usr/local/bin/kubectl
        log_info "kubectl 已安装: $(kubectl version --client --short 2>/dev/null || echo 'installed')"
    fi

    # Helm
    if ! helm version &>/dev/null; then
        curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash > /dev/null 2>&1
        log_info "Helm 已安装: $(helm version --short 2>/dev/null)"
    fi
else
    log_step "7/8 生产环境 - 跳过开发工具安装"
fi

# ======================== 系统优化 ========================
log_step "8/8 系统优化"

# 文件描述符限制
cat > /etc/security/limits.d/asset-system.conf << 'EOF'
*       soft    nofile      65535
*       hard    nofile      65535
*       soft    nproc       65535
*       hard    nproc       65535
EOF

# 内核参数优化
cat > /etc/sysctl.d/99-asset-system.conf << 'EOF'
# TCP优化
net.core.somaxconn = 32768
net.ipv4.tcp_max_syn_backlog = 32768
net.ipv4.tcp_fin_timeout = 15
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_keepalive_time = 300
net.ipv4.tcp_keepalive_probes = 3
net.ipv4.tcp_keepalive_intvl = 30
# 内存优化
vm.swappiness = 10
vm.max_map_count = 262144
# 文件描述符
fs.file-max = 655350
fs.inotify.max_user_watches = 524288
EOF
sysctl --system > /dev/null 2>&1
log_info "系统参数优化完成"

# ======================== 环境验证 ========================
echo ""
echo "============================================"
echo "  资产管理系统 - 环境安装完成!"
echo "============================================"
echo ""
echo "  Java:    $(java -version 2>&1 | head -1)"
echo "  Maven:   $(mvn -version 2>&1 | head -1)"
echo "  Node.js: $(node -v 2>/dev/null || echo '未安装')"
echo "  pnpm:    $(pnpm -v 2>/dev/null || echo '未安装')"
echo "  Docker:  $(docker -v 2>/dev/null || echo '未安装')"
echo ""
echo "  下一步: cd asset-management-system && bash init-project.sh"
echo "============================================"
