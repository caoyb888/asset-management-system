#!/bin/bash
# =============================================================================
#  资产管理系统 — 一键启动脚本
#  用法：./start.sh [选项]
#    --build      启动前重新构建所有后端模块（默认跳过构建）
#    --no-ui      不启动前端
#    --stop       停止所有服务（后端 + 前端 + 容器）
# =============================================================================

set -uo pipefail

# ─── 颜色 ─────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
ok()      { echo -e "${GREEN}[ OK ]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERR ]${NC}  $*"; }
step()    { echo -e "\n${BOLD}${CYAN}━━━  $*  ━━━${NC}"; }

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

# ─── 服务定义 ─────────────────────────────────────────────────────────────────
SERVICES=(
  "asset-base:8001:BaseApplication"
  "asset-investment:8002:InvestmentApplication"
  "asset-operation:8003:OperationApplication"
  "asset-finance:8004:FinanceApplication"
  "asset-report:8005:ReportApplication"
  "asset-system:8006:SystemApplication"
  "asset-workflow:8010:WorkflowApplication"
)

# ─── 参数解析 ─────────────────────────────────────────────────────────────────
OPT_BUILD=false
OPT_NO_UI=false
OPT_STOP=false

for arg in "$@"; do
  case $arg in
    --build)  OPT_BUILD=true ;;
    --no-ui)  OPT_NO_UI=true ;;
    --stop)   OPT_STOP=true  ;;
    *) echo "未知参数: $arg"; echo "用法: $0 [--build] [--no-ui] [--stop]"; exit 1 ;;
  esac
done

# =============================================================================
#  --stop 模式：停止所有服务
# =============================================================================
if $OPT_STOP; then
  step "停止所有服务"

  # 停止 Java 后端
  JAVA_PIDS=$(pgrep -f "asset-.*SNAPSHOT.jar" 2>/dev/null || true)
  if [ -n "$JAVA_PIDS" ]; then
    kill $JAVA_PIDS 2>/dev/null && ok "后端 Java 服务已停止" || warn "部分 Java 进程无法停止（可能属于其他用户）"
  else
    info "无运行中的后端 Java 进程"
  fi

  # 停止前端
  UI_PIDS=$(pgrep -f "vite.*asset-ui\|asset-ui.*vite" 2>/dev/null || true)
  [ -n "$UI_PIDS" ] && kill $UI_PIDS 2>/dev/null && ok "前端已停止" || info "无运行中的前端进程"

  # 停止容器
  CONTAINERS="asset-mysql-tmp asset-nacos asset-redis asset-xxljob"
  docker stop $CONTAINERS 2>/dev/null && ok "Docker 容器已停止" || warn "部分容器停止失败"

  echo ""
  ok "所有服务已停止。"
  exit 0
fi

# =============================================================================
#  启动模式
# =============================================================================
echo ""
echo -e "${BOLD}${CYAN}"
echo "  ╔══════════════════════════════════════════════╗"
echo "  ║        资产管理系统 — 一键启动               ║"
echo "  ╚══════════════════════════════════════════════╝"
echo -e "${NC}"

# ─── Step 1：端口冲突检测 ─────────────────────────────────────────────────────
step "Step 1  端口冲突检测"

CONFLICT_PORTS=""
for svc in "${SERVICES[@]}"; do
  port=$(echo "$svc" | cut -d: -f2)
  if ss -tlnp 2>/dev/null | grep -q ":${port} "; then
    pid=$(ss -tlnp 2>/dev/null | grep ":${port} " | grep -o 'pid=[0-9]*' | head -1 | cut -d= -f2)
    pname=$(ps -p "$pid" -o comm= 2>/dev/null || echo "unknown")
    warn "端口 $port 已被占用 (PID=$pid, $pname)"
    CONFLICT_PORTS="$CONFLICT_PORTS $port"
  fi
done

if [ -n "$CONFLICT_PORTS" ]; then
  warn "以下端口已有服务运行：$CONFLICT_PORTS"
  warn "如需重启，请先运行：$0 --stop"
  read -rp "  是否继续启动（仍会跳过已占用端口的服务）？[y/N] " ans
  [[ "$ans" =~ ^[Yy]$ ]] || exit 0
fi

ok "端口检测完成"

# ─── Step 2：启动基础容器 ──────────────────────────────────────────────────────
step "Step 2  启动基础容器"

# 处理 MySQL 端口 3306 冲突（lgdoc-mysql 等其他容器）
MYSQL_CONFLICT=$(docker ps --format "{{.Names}}" | grep -v "asset-mysql-tmp" | \
  xargs -I{} docker inspect {} --format "{{.Name}} {{json .HostConfig.PortBindings}}" 2>/dev/null | \
  grep '"3306/tcp"' | grep -v "asset-mysql-tmp" | awk '{print $1}' | tr -d '/' || true)

if [ -n "$MYSQL_CONFLICT" ]; then
  warn "检测到 3306 端口冲突容器：$MYSQL_CONFLICT，先停止..."
  docker stop $MYSQL_CONFLICT >/dev/null 2>&1 && ok "已停止冲突容器：$MYSQL_CONFLICT"
fi

info "启动 Docker 容器..."
docker start asset-mysql-tmp asset-redis asset-nacos asset-xxljob 2>/dev/null
ok "容器启动指令已发出：asset-mysql-tmp / asset-redis / asset-nacos / asset-xxljob"

# ─── Step 3：等待基础服务就绪 ─────────────────────────────────────────────────
step "Step 3  等待基础服务就绪"

# 等待 MySQL
info "等待 MySQL 就绪..."
TIMEOUT=30; ELAPSED=0
until docker exec asset-mysql-tmp mysqladmin ping -uroot -proot123 --silent 2>/dev/null; do
  sleep 2; ELAPSED=$((ELAPSED+2))
  [ $ELAPSED -ge $TIMEOUT ] && { error "MySQL 启动超时（${TIMEOUT}s）"; exit 1; }
  echo -n "."
done
echo ""; ok "MySQL 就绪"

# 等待 Nacos（最多 60 秒）
info "等待 Nacos 就绪（最多 60 秒）..."
TIMEOUT=60; ELAPSED=0
until curl -sf "http://127.0.0.1:8848/nacos/v1/console/health/readiness" >/dev/null 2>&1; do
  sleep 3; ELAPSED=$((ELAPSED+3))
  [ $ELAPSED -ge $TIMEOUT ] && { error "Nacos 启动超时（${TIMEOUT}s）"; exit 1; }
  echo -n "."
done
echo ""; ok "Nacos 就绪"

# ─── Step 4：构建（可选）────────────────────────────────────────────────────────
if $OPT_BUILD; then
  step "Step 4  构建后端模块"
  info "执行 mvn clean package -DskipTests ..."
  cd "$BASE_DIR"
  if mvn clean package -DskipTests -q 2>&1; then
    ok "后端构建完成"
  else
    error "构建失败，请检查错误日志后重试"
    exit 1
  fi
else
  step "Step 4  跳过构建（使用已有 JAR）"
  info "如需重新构建，请使用：$0 --build"
  # 检查 JAR 是否存在
  MISSING_JAR=false
  for svc in "${SERVICES[@]}"; do
    name=$(echo "$svc" | cut -d: -f1)
    jar="$BASE_DIR/$name/target/${name}-1.0.0-SNAPSHOT.jar"
    if [ ! -f "$jar" ]; then
      error "找不到 JAR：$jar"
      MISSING_JAR=true
    fi
  done
  if $MISSING_JAR; then
    error "部分模块未构建，请先运行：$0 --build"
    exit 1
  fi
  ok "所有 JAR 文件均存在"
fi

# ─── Step 5：启动后端服务 ──────────────────────────────────────────────────────
step "Step 5  启动后端服务"

START_PIDS=()
for svc in "${SERVICES[@]}"; do
  name=$(echo "$svc" | cut -d: -f1)
  port=$(echo "$svc" | cut -d: -f2)
  jar="$BASE_DIR/$name/target/${name}-1.0.0-SNAPSHOT.jar"
  log="/tmp/${name}.log"

  # 已在运行则跳过
  if ss -tlnp 2>/dev/null | grep -q ":${port} "; then
    warn "$name (端口 $port) 已在运行，跳过"
    continue
  fi

  info "启动 $name → 端口 $port，日志 $log"
  nohup java -jar "$jar" --spring.profiles.active=dev > "$log" 2>&1 &
  START_PIDS+=($!)
done

# ─── Step 6：等待后端健康检查 ─────────────────────────────────────────────────
step "Step 6  等待后端服务就绪"

FAILED_SERVICES=""
for svc in "${SERVICES[@]}"; do
  name=$(echo "$svc" | cut -d: -f1)
  port=$(echo "$svc" | cut -d: -f2)
  app_class=$(echo "$svc" | cut -d: -f3)
  log="/tmp/${name}.log"

  # 已跳过（端口已占用）则不再等待
  printf "  等待 %-30s" "$name..."

  TIMEOUT=120; ELAPSED=0; READY=false
  while [ $ELAPSED -lt $TIMEOUT ]; do
    # 检查进程是否崩溃
    if grep -q "Application run failed\|BUILD FAILURE" "$log" 2>/dev/null; then
      break
    fi
    # 检查启动成功日志
    if grep -q "Started ${app_class}" "$log" 2>/dev/null; then
      READY=true; break
    fi
    sleep 3; ELAPSED=$((ELAPSED+3)); echo -n "."
  done

  if $READY; then
    echo -e " ${GREEN}就绪${NC}"
  else
    echo -e " ${RED}超时/失败${NC}"
    FAILED_SERVICES="$FAILED_SERVICES $name"
    warn "查看日志：tail -50 $log"
  fi
done

# ─── Step 7：启动前端 ─────────────────────────────────────────────────────────
if ! $OPT_NO_UI; then
  step "Step 7  启动前端"

  if ss -tlnp 2>/dev/null | grep -q ":3100 "; then
    warn "前端 (端口 3100) 已在运行，跳过"
  else
    cd "$BASE_DIR/asset-ui"
    # 检查依赖
    if [ ! -d "node_modules" ]; then
      info "首次启动，安装前端依赖..."
      npm install --silent
    fi
    nohup npm run dev > /tmp/asset-ui.log 2>&1 &
    UI_PID=$!

    # 等待前端端口就绪
    printf "  等待 %-30s" "asset-ui..."
    TIMEOUT=60; ELAPSED=0; READY=false
    while [ $ELAPSED -lt $TIMEOUT ]; do
      if ss -tlnp 2>/dev/null | grep -q ":3100 "; then
        READY=true; break
      fi
      sleep 2; ELAPSED=$((ELAPSED+2)); echo -n "."
    done
    $READY && echo -e " ${GREEN}就绪${NC}" || echo -e " ${YELLOW}等待中（日志：/tmp/asset-ui.log）${NC}"
  fi
fi

# ─── 最终状态汇总 ──────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}${CYAN}━━━  启动结果汇总  ━━━${NC}"
echo ""

# 容器状态
printf "  %-25s" "Docker 容器"
for cname in asset-mysql-tmp asset-redis asset-nacos asset-xxljob; do
  STATUS=$(docker inspect --format='{{.State.Status}}' "$cname" 2>/dev/null || echo "not found")
  [ "$STATUS" = "running" ] && echo -ne " ${GREEN}${cname}✓${NC}" || echo -ne " ${RED}${cname}✗${NC}"
done
echo ""

# 后端服务状态
echo ""
printf "  %-25s %-8s %s\n" "服务" "端口" "状态"
printf "  %-25s %-8s %s\n" "─────────────────────" "──────" "──────"
for svc in "${SERVICES[@]}"; do
  name=$(echo "$svc" | cut -d: -f1)
  port=$(echo "$svc" | cut -d: -f2)
  if ss -tlnp 2>/dev/null | grep -q ":${port} "; then
    printf "  %-25s %-8s ${GREEN}%s${NC}\n" "$name" "$port" "运行中 ✓"
  else
    printf "  %-25s %-8s ${RED}%s${NC}\n" "$name" "$port" "未运行 ✗"
  fi
done

# 前端状态
if ! $OPT_NO_UI; then
  if ss -tlnp 2>/dev/null | grep -q ":3100 "; then
    printf "  %-25s %-8s ${GREEN}%s${NC}\n" "asset-ui (前端)" "3100" "运行中 ✓"
  else
    printf "  %-25s %-8s ${YELLOW}%s${NC}\n" "asset-ui (前端)" "3100" "启动中..."
  fi
fi

echo ""
if [ -z "$FAILED_SERVICES" ]; then
  echo -e "  ${GREEN}${BOLD}所有服务启动成功！${NC}"
  echo ""
  echo -e "  访问地址：${CYAN}http://$(hostname -I | awk '{print $1}'):3100${NC}"
  echo -e "  默认账号：${CYAN}admin / admin123${NC}"
else
  warn "以下服务启动失败：$FAILED_SERVICES"
  echo "  请检查对应日志文件排查原因："
  for name in $FAILED_SERVICES; do
    echo "    tail -100 /tmp/${name}.log"
  done
fi

echo ""
echo -e "  停止所有服务：${YELLOW}$0 --stop${NC}"
echo ""
