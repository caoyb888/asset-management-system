# 产城公司资产管理系统 — 开发指南

> 企业级 SaaS 资产管理平台 | Spring Boot 3 + Vue 3 微服务架构

---

## 项目概览

- **项目类型：** 企业级资产管理平台（园区项目、商铺、招商、合同、财务全流程）
- **架构模式：** 微服务（Spring Cloud Alibaba + Nacos 注册中心）
- **开发语言：** Java 21 + TypeScript
- **默认账号：** admin / admin123

---

## 微服务模块一览

| 模块 | 目录 | 端口 | 包名前缀 | 数据库 | 控制器前缀 | 状态 |
|------|------|------|----------|--------|-----------|------|
| 基础数据管理 | asset-base | 8001 | com.asset.base | asset_db | /base/* | 已完成 |
| 招商管理 | asset-investment | 8002 | com.asset.investment | asset_db | /inv/* | 已完成 |
| 营运管理 | asset-operation | 8003 | com.asset.operation | asset_db | /opr/* | 已完成 |
| 财务管理 | asset-finance | 8004 | com.asset.finance | asset_db | /fin/* | 已完成 |
| 报表管理 | asset-report | 8005 | com.asset.report | asset_report | /rpt/* | 已完成 |
| 系统管理 | asset-system | 8006 | com.asset.system | asset_db | /sys/* | 已完成 |
| GIS 可视化 | asset-gis | 8007 | com.asset.gis | - | /gis/* | 骨架 |
| 工作流 | asset-workflow | 8010 | com.asset.workflow | - | /wf/* | 骨架 |
| 消息服务 | asset-message | 8011 | com.asset.message | - | /msg/* | 骨架 |
| 文件服务 | asset-file | 8012 | com.asset.file | - | /file/* | 骨架 |
| 支付服务 | asset-payment | 8013 | com.asset.payment | - | /pay/* | 骨架 |
| API 网关 | asset-gateway | 9000 | com.asset.gateway | - | - | 骨架 |
| **前端** | asset-ui | 3100 | - | - | - | 已完成 |

### 公共模块

| 模块 | 说明 |
|------|------|
| asset-common | 基础工具：R\<T\> 统一返回、BaseEntity、PageQuery、国密工具 |
| asset-common-mybatis | MybatisPlusConfig、AuditMetaObjectHandler、分页/乐观锁插件 |
| asset-common-security | JWT 认证：JwtUtil、JwtAuthFilter、SecurityAutoConfiguration |
| asset-common-redis | Redis 配置、Redisson 分布式锁 |
| asset-common-log | 操作日志 AOP |
| asset-api-base | 基础服务 Feign 接口 |
| asset-api-system | 系统服务 Feign 接口 |

---

## 技术栈

### 后端

| 类别 | 技术 | 版本 |
|------|------|------|
| 核心框架 | Spring Boot | 3.3.6 |
| 微服务 | Spring Cloud + Alibaba | 2023.0.3 / 2023.0.1.2 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL | 8.0 |
| 连接池 | Druid | 1.2.23 |
| 缓存 | Redis 7.x + Redisson | - |
| 认证 | JWT (jjwt 0.12.6) + Spring Security | - |
| 国密 | SM2（密码传输）/ SM3（密码存储）/ SM4（身份证加密） | - |
| API 文档 | Knife4j (Springdoc OpenAPI) | - |
| 导入导出 | EasyExcel | - |

### 前端

| 类别 | 技术 |
|------|------|
| 核心框架 | Vue 3.5 + Vite 6 + TypeScript |
| UI 组件 | Element Plus |
| 状态管理 | Pinia |
| HTTP | Axios（baseURL=/api，响应拦截器自动解包） |
| 图表 | ECharts 5 |
| 3D 可视化 | Three.js |

---

## 数据库

- **连接信息：** `root / root123 @ 127.0.0.1:3306`
- **主库：** `asset_db`（基础/招商/营运/财务/系统模块共用）
- **报表库：** `asset_report`（报表模块独立库）
- **运行方式：** Docker 容器 `asset-mysql-tmp`

### 初始化脚本（sql/ 目录，按顺序执行）

| 脚本 | 说明 |
|------|------|
| init.sql | 基础数据 20 张表 + admin 种子数据 |
| investment_init.sql | 招商管理 19 张表 + 基础数据 |
| operation_init.sql | 营运管理 14 张表 opr_* |
| finance_v2_init.sql | 财务管理 13 张表 fin_*（旧 finance_init.sql 已废弃） |
| system_init.sql | 系统管理表 |
| system_patch_v2.sql | 系统管理补丁 |
| report_init.sql → report_v2_favorite.sql → report_v3_schedule.sql | 报表管理（按序执行） |
| alter_task7.sql | 招商模块补丁（租金分解快照） |
| test-data-*.sql | 各模块测试数据 |

### 表命名约定

| 模块 | 表前缀 | 示例 |
|------|--------|------|
| 基础数据 | biz_ | biz_project, biz_shop, biz_brand, biz_merchant |
| 招商管理 | inv_, cfg_ | inv_intention, inv_lease_contract, cfg_rent_scheme |
| 营运管理 | opr_ | opr_contract_ledger, opr_contract_change |
| 财务管理 | fin_ | fin_receivable, fin_receipt, fin_write_off |
| 系统管理 | sys_ | sys_user, sys_role, sys_menu, sys_dict_type |
| 报表管理 | rpt_ | rpt_etl_task, rpt_user_favorite |

### 通用约定

- 逻辑删除：`is_deleted=0/1`，MyBatis-Plus `@TableLogic` 自动过滤
- 审计字段：`created_by / created_at / updated_by / updated_at`，由 `AuditMetaObjectHandler` 自动注入
- 乐观锁：`version` 字段，`@Version` 注解

---

## 前端 Vite 代理规则

前端 Axios 配置 `baseURL: '/api'`，Vite 按匹配顺序代理：

| 代理规则 | 目标服务 | 端口 |
|----------|----------|------|
| /file/* | asset-file | 8012 |
| /api/auth/* | asset-base | 8001 |
| /api/sys/* | asset-base | 8001 |
| /api/fin/* | asset-finance | 8004 |
| /api/opr/* | asset-operation | 8003 |
| /api/inv/* | asset-investment | 8002 |
| /api/rpt/* | asset-report | 8005 |
| /api/* (兜底) | asset-base | 8001 |

所有代理均 rewrite 去掉 `/api` 前缀。

---

## 开发规范

### 后端规范

- **统一返回：** `R<T> { code, msg, data }`
- **分页：** `PageQuery` 基类（pageNum/pageSize/orderBy/orderDirection）+ MyBatis-Plus `IPage`
- **枚举名称：** Service 层手动 `fillEnumNames()`，不存数据库
- **自定义联表查询：** 写 Mapper XML；简单单表用 `BaseMapper + LambdaWrapper`
- **控制器路径：** 不带 `/api` 前缀（如 `/base/projects`，由网关/代理加前缀）
- **代码风格：** 遵循阿里巴巴 Java 开发手册，统一中文注释

### 前端规范

- **API 文件路径：** `src/api/模块/功能.ts`（如 `src/api/fin/receivable.ts`）
- **视图路径：** `src/views/模块/功能/index.vue`（如 `src/views/fin/receivable/index.vue`）
- **组件命名：** PascalCase（如 `ContractSelector.vue`）
- **路由：** `/模块/功能`（如 `/fin/receivable`、`/inv/contracts`）
- **Store：** `src/store/modules/模块/useXxxStore.ts`

### Git 规范

- 分支策略：`master`(生产) / `main`(主开发) / `feature/*`(功能)
- 提交格式：`feat/fix/docs/style/refactor/test/chore: 描述`

### 安全

- SM2 私钥（后端）：`BF504AF9D74A8E072F09D4A06376E3337EA5562E12056D3B1410B53214E9E27A`
- SM2 公钥（前端）：`04FCF0FF361BB00B2391CC9A77F72CD42F2D9DCAD872CDBA8420C44746EBC0D4A2FD59544B0539DF7AEFA2BEBB5CABC543BA40BA920A0B037C015A684700A14CB5`
- 公开路径白名单：`/auth/login`, `/auth/publicKey`, `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/**`
- RBAC 权限模型：菜单权限 + 按钮权限 + 数据权限（`@DataScope` AOP）

---

## 环境依赖

| 服务 | 容器名 | 端口 | 备注 |
|------|--------|------|------|
| MySQL 8.0 | asset-mysql-tmp | 3306 | root/root123 |
| Redis 7.x | asset-redis | 6379 | 无密码（Redisson 不设 password 字段） |
| Nacos | asset-nacos | 8848 | 启动较慢，需等待约 15 秒 |

启动命令：`docker start asset-mysql-tmp asset-redis asset-nacos`

详细启动流程参见 [docs/系统启动手册.md](docs/系统启动手册.md)

---

## 当前开发进度

- [x] 数据库设计（全模块）
- [x] 基础数据管理（asset-base）— 全栈完成，已测试
- [x] 招商管理（asset-investment）— 全栈完成，已测试
- [x] 营运管理（asset-operation）— 全栈完成，已测试
- [x] 财务管理（asset-finance）— 全栈完成，已测试
- [x] 报表管理（asset-report）— 后端完成
- [x] 系统管理（asset-system）— 全栈完成
- [ ] GIS 可视化（asset-gis）— 骨架
- [ ] 工作流（asset-workflow）— 骨架
- [ ] 消息服务（asset-message）— 骨架
- [ ] 移动端（UniApp）— 未开始

---

## 参考文档（docs/ 目录）

### API 契约文档
- [基础数据管理 API 契约](docs/03_API契约文档.md)

### 数据库设计
- [基础数据管理](docs/基础数据管理模块数据库设计.md)
- [招商管理](docs/招商管理模块-数据库设计_修订版.md)
- [营运管理](docs/营运管理模块_数据库设计.md)
- [财务管理](docs/数据库设计-财务管理模块.md) / [财务管理v2](docs/财务管理模块-数据库设计.md)
- [报表管理](docs/报表管理-数据库设计.md)
- [系统管理](docs/系统管理模块-数据库设计.md)

### 技术分析报告
- [基础数据管理](docs/基础数据管理模块_技术分析报告.docx)
- [招商管理](docs/招商管理模块_技术分析报告_修订版.docx)
- [营运管理](docs/营运管理模块_技术分析报告.docx)
- [财务管理](docs/财务管理模块_技术分析报告.docx)
- [报表管理](docs/报表管理模块_技术分析报告.docx)
- [系统管理](docs/系统管理模块_技术分析报告.docx)

### 任务分解与开发计划
- [招商管理](docs/招商管理模块_开发任务分解.md)
- [营运管理](docs/营运管理模块_开发步骤与任务分解.md)
- [财务管理](docs/财务管理模块-任务分解及开发计划v2.md)
- [报表管理](docs/报表管理模块_任务分解与开发计划.md)
- [系统管理](docs/系统管理模块_任务分解及开发计划.md)

### 测试计划
- [基础数据管理](docs/基础数据管理模块_单元测试计划.md)
- [招商管理](docs/招商管理模块_单元测试计划.md)
- [营运管理](docs/营运管理模块_单元测试计划.md)
- [财务管理](docs/财务管理模块_单元测试计划.md)

### 其他
- [系统启动手册](docs/系统启动手册.md)
- [部署手册](docs/部署手册.md)
- [项目代码规模统计](docs/项目代码规模统计.md)
- [问题反馈表](docs/问题反馈表.md)
- [基础数据管理未完成功能清单](docs/基础数据管理模块_未完成功能清单.md)

---

## 已知 Bug 与踩坑记录

> 以下问题均已修复，记录在此防止重犯。

### 1. 前端 API 路径禁止重复 /api 前缀

`request.ts` 已配置 `baseURL: '/api'`，API 调用路径直接从模块前缀开始：

```ts
// 正确              // 错误
'/opr/xxx'          '/api/opr/xxx'  → 变成 /api/api/opr/xxx
'/fin/xxx'          '/api/fin/xxx'
'/inv/xxx'          '/api/inv/xxx'
```

### 2. 前端 API 响应已自动解包，禁止重复 res.data

`request.ts` 拦截器在 `code===200` 时直接 `return res.data`，调用方拿到的已是 `data`：

```ts
// 正确                              // 错误
const res = await api.page(query)    const res = await api.page(query)
res.records                          res.data?.records  // undefined
res.total                            res.data?.total    // undefined
```

### 3. biz_brand 品牌名称字段为 brand_name_cn

SQL JOIN `biz_brand` 取品牌名必须用 `bb.brand_name_cn AS brand_name`，不是 `bb.brand_name`。

### 4. 登录 Token 字段名

后端返回 `token`（非 `accessToken`），前端已兼容：`result.token || result.accessToken`。

### 5. router-view 禁止 transition mode="out-in"

复杂页面会导致 `transitionend` 未触发，路由切换后工作区空白。已改为直接渲染 + `:key="route.fullPath"`。

### 6. el-tabs + el-table 表单页返回后空白

三重修复（缺一不可）：
1. 单根节点（不用 Fragment）
2. 返回前先 `v-if` 卸载复杂子树 → `await nextTick()` → `router.push()`
3. async 函数必须 `try/catch`

### 7. 新增微服务必须配置 mybatis-plus

每个模块的 `application-dev.yml` 必须包含 `mybatis-plus` 配置块（`map-underscore-to-camel-case: true`、逻辑删除等），否则字段映射全部失败。

### 8. JdbcTemplate.query() Lambda 歧义

无返回值的 lambda 必须显式转型 `(RowCallbackHandler) rs -> ...`。

### 9. 流水表格不能放在 v-if="account" 内部

保证金/预收款等"先查账户再看流水"的页面，流水表格要独立在 `v-if` 外部始终渲染。

### 10. Redisson 无密码 Redis

不设置 `password` 字段（空字符串会触发 AUTH 报错）。
