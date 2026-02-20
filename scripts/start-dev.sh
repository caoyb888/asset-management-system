#!/bin/bash
#=============================================================================
# 资产管理系统 - 一键启动开发环境
# 前提: 已执行 scripts/init-env.sh 完成环境初始化
#=============================================================================
set -euo pipefail

GREEN='\033[0;32m'; BLUE='\033[0;34m'; YELLOW='\033[1;33m'; NC='\033[0m'
info() { echo -e "${BLUE}[INFO]${NC} $1"; }
ok()   { echo -e "${GREEN}[ OK ]${NC} $1"; }

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo ""
echo "  ╔═══════════════════════════════════════╗"
echo "  ║   资产管理系统 - 一键启动开发环境       ║"
echo "  ╚═══════════════════════════════════════╝"
echo ""

# Step 1: 启动基础设施
info "1/4  启动 Docker 基础设施..."
cd "${PROJECT_DIR}/docker/dev"
docker compose up -d
ok "MySQL / Redis / Nacos / MinIO / RabbitMQ / ES / XXL-Job"

# Step 2: 等待MySQL就绪
info "2/4  等待 MySQL 就绪..."
until docker exec asset-mysql mysqladmin ping -h localhost --silent 2>/dev/null; do
    sleep 2
done
ok "MySQL 已就绪"

# Step 3: 构建后端
info "3/4  构建后端 (首次较慢)..."
cd "${PROJECT_DIR}/backend/asset-parent"
mvn clean install -DskipTests -q 2>/dev/null && ok "后端构建完成" || {
    echo -e "${YELLOW}[WARN] 后端构建失败，请检查Maven配置${NC}"
}

# Step 4: 安装前端依赖
info "4/4  安装前端依赖..."
cd "${PROJECT_DIR}/frontend"
pnpm install --frozen-lockfile 2>/dev/null || pnpm install
ok "前端依赖安装完成"

echo ""
echo "  ╔═══════════════════════════════════════════╗"
echo -e "  ║  ${GREEN}✓ 开发环境就绪${NC}                            ║"
echo "  ╠═══════════════════════════════════════════╣"
echo "  ║  基础设施:                                 ║"
echo "  ║    MySQL        → localhost:3306           ║"
echo "  ║    Redis        → localhost:6379           ║"
echo "  ║    Nacos        → http://nacos.localhost    ║"
echo "  ║    MinIO        → http://minio.localhost    ║"
echo "  ║    RabbitMQ     → http://mq.localhost       ║"
echo "  ║    XXL-Job      → http://localhost:8180     ║"
echo "  ║    ES           → http://localhost:9200     ║"
echo "  ║    Traefik      → http://localhost:8080     ║"
echo "  ╠═══════════════════════════════════════════╣"
echo "  ║  启动服务:                                 ║"
echo "  ║    后端: cd backend/asset-base             ║"
echo "  ║          mvn spring-boot:run               ║"
echo "  ║    前端: cd frontend && pnpm dev           ║"
echo "  ╚═══════════════════════════════════════════╝"
echo ""
