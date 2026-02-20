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
