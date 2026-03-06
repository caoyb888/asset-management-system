---
category: "[[Daily Notes]]"
title: SKills
description: "[]"
creation_date: 2026-02-04
published:
---
## 📋 项目概述

**项目名称：** 产城公司资产管理系统  
**项目类型：** 企业级SaaS资产管理平台  
**开发模式：** AI辅助全栈开发  
**预期周期：** 16周

---

## 🎯 核心功能模块（8大模块）

### 1️⃣ 基础数据管理

管理园区的项目、楼栋、楼层、商铺、品牌、商家等基础资产信息

- 项目管理：一企一档，维护项目基本信息、财务信息、合同信息
- 楼栋管理：楼栋信息、建筑面积、营业面积、楼层数
- 楼层管理：楼层信息、面积统计、楼层图
- 商铺管理：商铺基本信息、拆分合并、业态管理、导入导出
- 品牌管理：品牌档案、联系人、业态分类
- 商家管理：商家档案、评级、诚信记录、开票信息

### 2️⃣ 招商管理

从意向客户到签约的完整招商流程管理

- 意向协议：意向登记、计租方案配置、收费规则设置、审批流程
- 招商合同：意向转合同、新增合同、合同模板、电子签章
- 开业审批：开业申请、审批流程、材料管理
- 租决政策：租金策略、优惠政策、审批管理
- 租金分解：按楼层/商铺分解租金、单价计算

### 3️⃣ 营运管理

合同执行期的全生命周期管理

- 合同台账：台账生成、双签管理、应收推送、一次性首款
- 合同变更：主体变更、品牌变更、租金调整、面积变更、租期变更
- 营收填报：营业额录入、浮动租金计算、导入导出
- 合同解约：到期解约、提前解约、解约审批、费用结算

### 4️⃣ 财务管理

完整的收支管理和财务核算体系

- 收款管理：收款单、核销单、负数核销、余额处理
- 应收管理：应收明细、应收汇总、账单打印、欠费统计、减免调整
- 凭证管理：生成凭证、审核上传、与财务系统对接
- 保证金管理：余额查询、申请冲抵、退款、罚没
- 预收款管理：暂存款项管理、抵冲、退款

### 5️⃣ 资产一张图（可视化）

基于GIS和3D技术的资产可视化大屏

- GIS地图展示项目分布、位置信息
- 3D楼栋模型展示（Three.js）
- 实时数据大屏：租赁率、空置率、收益统计
- 多维度数据钻取：公司→项目→楼栋→商铺
- 动态图表分析（Echarts）

### 6️⃣ 报表管理

多维度数据分析和报表输出

- 资产类报表：租赁情况、空置率、业态分布
- 招商类报表：意向统计、签约进度、客户漏斗
- 营运类报表：合同变更、营收分析、客流分析
- 财务类报表：应收明细、欠款统计、回款分析

### 7️⃣ 系统管理

用户权限和系统配置管理

- 用户管理：用户CRUD、角色分配
- 机构管理：组织架构树、部门管理
- 岗位管理：岗位配置、权限关联
- 权限管理：RBAC权限模型、菜单权限、数据权限
- 业务字典：数据定义、分类编码、租费算法

### 8️⃣ 移动端（小程序/H5）

移动化办公和商家服务

- 消息通知：流程审批、资产到期、变动提醒
- 资讯管理：资讯发布、阅读、收藏、评论
- 合同管理：合同查询、移动审批、进度查看
- 个人中心：收藏、提醒、设置

---

## 🛠️ 技术架构要求

### 后端技术栈

```
核心框架：Spring Boot 3.x
数据库：MySQL 8.0 
ORM框架：MyBatis-Plus 3.5.6+
缓存：Redis 7.x
认证：JWT + Spring Security
API文档：Knife4j
定时任务：XXL-Job
消息队列：RabbitMQ（可选）
E2E测试:Playwright for Java
```

### 前端技术栈

```
核心框架：Vue 3 + Vite
UI组件库：Element Plus
状态管理：Pinia
路由：Vue Router 4
HTTP库：Axios
图表：Echarts 5
3D可视化：Three.js
地图：高德地图API
构建工具：Vite
```

### 移动端技术栈

```
框架：UniApp
适配平台：微信小程序、H5、APP
UI组件：uni-ui
```

### 部署架构

```
容器化：Docker
编排：Kubernetes（可选）
CI/CD：Jenkins / GitLab CI
监控：Prometheus + Grafana
日志：ELK Stack
```

---

## 📊 核心业务流程

### 流程1：招商签约流程

```
1. 录入意向协议
   ├─ 选择商铺
   ├─ 配置计租方案（固定/提成/阶梯）
   ├─ 设置收费规则
   ├─ 填写租金单价
   ├─ 生成费用明细
   └─ 发起审批

2. 审批通过后转租赁合同
   ├─ 补录合同信息
   ├─ 生成合同文本
   ├─ 电子签章
   └─ 进入合同台账

3. 合同台账生成应收
   ├─ 自动生成应收计划
   ├─ 推送财务系统
   └─ 开始收款流程
```

### 流程2：收款核销流程

```
1. 商家付款
   ├─ 线上支付（小程序）
   └─ 线下转账

2. 财务录入收款单
   ├─ 选择合同
   ├─ 录入收款金额
   ├─ 选择收款方式
   └─ 保存收款单

3. 核销应收
   ├─ 匹配应收账期
   ├─ 按费项核销
   ├─ 超额部分转预存款
   └─ 生成凭证
```

### 流程3：合同变更流程

```
1. 发起变更申请
   ├─ 选择变更类型（主体/品牌/租金/面积/租期）
   ├─ 填写变更内容
   ├─ 生成补充协议
   └─ 提交审批

2. 审批通过后
   ├─ 更新合同信息
   ├─ 调整应收计划
   ├─ 重新生成费用
   └─ 归档变更记录
```

---

## 🎨 UI/UX设计要求

### 整体风格

- 现代简约、专业商务
- 主色调：蓝色系（#2E75B6）
- 响应式设计，适配PC、平板、手机
- 大屏展示支持1920x1080分辨率

### 关键页面

1. **工作台首页**：待办事项、统计卡片、快捷入口、消息通知
2. **资产地图**：GIS地图 + 3D模型 + 数据大屏
3. **合同管理**：列表 + 详情 + 审批流程展示
4. **财务看板**：应收统计、收款趋势、欠费预警
5. **移动端首页**：卡片式布局、消息推送、快捷功能

---

## 💾 核心数据结构

### 1. 项目表 (biz_project)

```sql
主键：id
字段：project_code, project_name, company_id, province, city, 
      property_type, business_type, building_area, operating_area,
      operation_status, opening_date, manager_id, address, images_urls
```

### 2. 商铺表 (biz_shop)

```sql
主键：id
字段：project_id, building_id, floor_id, shop_code, shop_type,
      rent_area, actual_area, building_area,operating_area,
      shop_status, plan_format, contract_format
```

### 3. 合同表 (inv_lease_contract)

```sql
主键：id
字段：contract_code, contract_name, project_id, merchant_id, brand_id,
      contract_type, status, contract_start, contract_end,
      delivery_date, decoration_start,decoration_end, opening_date,
      rent_scheme_id, payment_cycle, billing_mode
```

### 4. 应收表 (fin_receivable)

```sql
主键：id
字段：contract_id, project_id, merchant_id, fee_item_id, 
      billing_start, billing_end, actual_amount, received_amount,
      original_amount, status,accrual_month
```

### 5. 收款表 (fin_receipt)

```sql
主键：id
字段：receipt_code, contract_id, merchant_id, total_amount,
      payment_method,receipt_date, bank_serial_no, payer_name,
      bank_name, status
```

---

## 🔐 安全与合规要求

### 数据安全

- 敏感数据加密存储（国产加密算法）
- 用户密码加密（BCrypt）
- 数据传输HTTPS
- SQL注入防护
- XSS/CSRF防护

### 权限控制

- RBAC角色权限模型
- 菜单级权限控制
- 按钮级权限控制
- 数据级权限控制（只能看到自己权限范围内的项目数据）
- 操作日志记录

### 合规要求

- 国产化适配（支持国产数据库、操作系统）
- 数据备份策略
- 审计日志
- 隐私保护

---

## 📝 开发规范

### 代码规范

- 后端遵循阿里巴巴Java开发手册
- 前端遵循Vue官方风格指南
- 统一使用中文注释
- 接口遵循RESTful规范
- 统一返回格式：`{code, message, data}`

### 命名规范

- 数据库表：以数据库设计文档为准（如 tb_contract_info）
- 接口路径：/api/模块/资源（如 /api/contract/list）
- 前端路由：/模块/功能（如 /contract/list）
- 组件命名：PascalCase（如 ContractList.vue）

### Git规范

- 分支策略：master(生产) / develop(开发) / feature(功能)
- 提交信息：feat/fix/docs/style/refactor/test/chore
- 代码审查：PR合并前必须Review


## 当前进度
- [x] 技术分析报告
- [x] 数据库设计
- [ ] 编码阶段

## 参考文档
- 数据库设计：docs/基础数据管理模块数据库设计.md
- 技术分析报告：docs/基础数据管理模块_技术分析报告.docx

---

## ⚠️ 已知 Bug 与规范

### 前端 API 路径规范：禁止在请求路径中重复 /api 前缀

**问题描述：** `src/api/request.ts` 已配置 `baseURL: '/api'`，Axios 会将 baseURL 与请求路径拼接。若请求路径再次包含 `/api/` 前缀，最终 URL 会变成 `/api/api/opr/...`，导致 Vite 代理无法正确匹配具体微服务规则，请求错误路由到 `asset-base`（端口 8001），返回 500 错误。

**曾出错的模块：** 招商管理（inv）、营运管理（opr）、财务管理（fin）

**错误写法：**
```ts
// ❌ 错误：baseURL 已是 /api，路径不能再加 /api/
request.get('/api/opr/contract-changes', { params })
request.get('/api/fin/receivables', { params })
request.get('/api/inv/contracts', { params })
```

**正确写法：**
```ts
// ✅ 正确：路径直接从模块前缀开始，不含 /api
request.get('/opr/contract-changes', { params })
request.get('/fin/receivables', { params })
request.get('/inv/contracts', { params })
```

**Vite 代理规则（vite.config.ts）：**
- `/api/opr/*` → `asset-operation`（端口 8003）
- `/api/fin/*` → `asset-finance`（端口 8004）
- `/api/inv/*` → `asset-investment`（端口 8002）
- `/api/*`     → `asset-base`（端口 8001）

**新增 API 文件时必须检查：** 路径以 `/opr/`、`/fin/`、`/inv/` 或其他模块前缀开头，而非 `/api/opr/` 等。

---

### biz_brand 表品牌名称字段为 brand_name_cn，非 brand_name

**问题描述：** `biz_brand` 表中品牌名称列名是 `brand_name_cn`，而非直觉上的 `brand_name`。凡涉及 JOIN `biz_brand` 取品牌名称的 SQL，必须使用 `bb.brand_name_cn`，并用 `AS brand_name` 别名映射到实体字段。

**错误写法：**
```sql
-- ❌ 错误：biz_brand 没有 brand_name 列，会报 Unknown column 'bb.brand_name'
LEFT JOIN biz_brand bb ON bb.id = ii.brand_id
SELECT bb.brand_name
```

**正确写法：**
```sql
-- ✅ 正确：使用 brand_name_cn 并起别名
LEFT JOIN biz_brand bb ON bb.id = ii.brand_id AND bb.is_deleted = 0
SELECT bb.brand_name_cn AS brand_name
```

**受影响文件：** `asset-investment/src/main/resources/mapper/InvIntentionMapper.xml`（已修正）

---

### 路由切换后工作区空白：禁止在 router-view 上使用 transition mode="out-in"

**问题描述：** `BasicLayout.vue` 的 `<router-view>` 原来包裹了 `<transition name="fade" mode="out-in">`。`mode="out-in"` 依赖旧组件触发 `transitionend` 事件后才渲染新组件。当离开页面是复杂组件（如含 `el-loading`、大量子组件的向导表单）时，`transitionend` 有时无法正常触发，导致 Vue 永久等待 leave 结束，新页面始终不渲染，工作区呈现空白，只有强制刷新才能恢复。

**错误写法：**
```html
<!-- ❌ 错误：mode="out-in" 在复杂页面下会导致路由切换后工作区空白 -->
<router-view v-slot="{ Component }">
  <transition name="fade" mode="out-in">
    <component :is="Component" />
  </transition>
</router-view>
```

**正确写法：**
```html
<!-- ✅ 正确：去掉 transition，保留 :key 强制刷新组件实例 -->
<router-view v-slot="{ Component, route }">
  <component :is="Component" :key="route.fullPath" />
</router-view>
```

**受影响文件：** `asset-ui/src/layouts/BasicLayout.vue`（已修正）

---

### 含 el-tabs/el-table 的表单页返回后工作区空白且菜单无响应

**问题描述：** 在含有 `el-tabs` + `el-table`（内嵌 `el-input`/`el-input-number` 可编辑单元格）的表单页（如租金分解 `rent-decomp/form.vue`），点击返回按钮后，工作区变为空白且点击左侧菜单无任何反应，只有强制刷新才能恢复。根本原因是 `el-tabs` 相关的 `ResizeObserver`、`useWindowFocus`、`useDocumentVisibility` 等 Element Plus 内部观察者，在组件一次性卸载时未能完全清理，导致遗留状态阻断后续渲染或事件响应。

**三重修复方案（缺一不可）：**

**修复 1 — 消除 Fragment 根节点：** 将模板中作为第二根节点的 `<ApprovalDialog>` 移入主 `<div>` 内，使组件变为单根节点。Fragment 组件在 `:key` 切换时，Vue 对多根节点的并发卸载存在潜在清理不完整的风险。

```html
<!-- ❌ 错误：Fragment 结构（两个根节点） -->
<template>
  <div class="form-page">...</div>
  <ApprovalDialog ... />
</template>

<!-- ✅ 正确：单根节点 -->
<template>
  <div class="form-page">
    ...
    <ApprovalDialog ... />
  </div>
</template>
```

**修复 2 — 返回前先卸载复杂子树：** 不要直接调用 `router.push`，而是先将控制 `el-tabs` 显示的响应式变量置为触发 `v-if` 隐藏的值，`await nextTick()` 让 Vue 完成 DOM 更新（即先卸载 `el-tabs`/`el-table`），再执行路由跳转。

```typescript
// ❌ 错误：直接跳转，el-tabs + el-table 和 router-view 切换同时发生
router.push('/inv/rent-decomps')

// ✅ 正确：先收起复杂子树，再跳转
async function handleReturn() {
  recordId.value = null  // 触发 v-if，卸载 el-tabs / el-table
  await nextTick()       // 等 Vue 完成 DOM 更新
  router.push('/inv/rent-decomps')
}
```

**修复 3 — async 生命周期钩子加 try/catch：** `onMounted` 和 `loadData` 中的异步调用必须包裹 `try/catch`，防止未处理的 Promise rejection 向上传播干扰 Vue 内部错误处理机制。

```typescript
// ❌ 错误：无 try/catch，async 错误向上传播
async function loadData(id: number) {
  const [detail, details] = await Promise.all([...])
  ...
}

// ✅ 正确：捕获错误，防止传播
async function loadData(id: number) {
  try {
    const [detail, details] = await Promise.all([...])
    ...
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '加载数据失败')
  }
}
```

**受影响文件：** `asset-ui/src/views/inv/rent-decomp/form.vue`（已修正）

**适用范围：** 凡是表单页包含 `el-tabs` + `el-table`（内嵌可编辑单元格）且点击返回后出现空白页/菜单无响应的场景，均应应用以上三重修复。

---

### 新增微服务模块必须在 application-dev.yml 中配置 mybatis-plus

**问题描述：** `asset-operation` 模块的 `application-dev.yml` 缺少 `mybatis-plus` 配置块，导致 `map-underscore-to-camel-case` 未启用。MyBatis-Plus 无法将数据库 snake_case 列名（如 `double_sign_status`、`ledger_code`、`receivable_status`）映射到 Java 实体的 camelCase 字段，所有查询返回的实体对象字段值均为 `null`，前台列表页看不到任何数据。

**曾出错的模块：** 营运管理（asset-operation）

**错误写法：**
```yaml
# ❌ 错误：application-dev.yml 中缺少 mybatis-plus 配置，字段映射全部失败
spring:
  datasource:
    ...
logging:
  ...
# 没有 mybatis-plus 配置块！
```

**正确写法（每个微服务模块必须包含）：**
```yaml
# ✅ 正确：所有微服务模块的 application-dev.yml 必须包含以下配置
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

**新增微服务模块时必须检查：** `application-dev.yml` 和 `application-test.yml` 均包含完整的 `mybatis-plus` 配置块，与 `asset-base` 等已有模块保持一致。

**受影响文件：** `asset-operation/src/main/resources/application-dev.yml`（已修正）

---

### 前端 API 响应已自动解包：禁止重复访问 res.data

**问题描述：** `src/api/request.ts` 的响应拦截器已对后端统一返回格式 `{code, msg, data}` 做了解包处理——当 `code === 200` 时直接 `return res.data`，即调用方拿到的 `res` 已经是 `data` 本身。如果 Vue 组件中再写 `res.data?.records`，实际等价于 `原始响应.data.data.records`，结果为 `undefined`，导致列表页无数据显示但不报错（静默失败）。

**曾出错的模块：** 营运管理（opr）——13 个 Vue 页面全部存在此问题

**错误写法：**
```ts
// ❌ 错误：res 已经是解包后的 data，再访问 .data 得到 undefined
const res = await api.page(query)
tableData.value = res.data?.records || []   // undefined?.records → undefined → []
total.value     = res.data?.total || 0      // undefined?.total → undefined → 0
```

**正确写法：**
```ts
// ✅ 正确：res 就是 data，直接访问 records/total
const res = await api.page(query) as any
tableData.value = res.records || []
total.value     = res.total || 0
```

**request.ts 拦截器逻辑（关键代码）：**
```ts
service.interceptors.response.use((response) => {
  const res = response.data          // 后端返回 {code, msg, data}
  if (res.code === 200) return res.data  // ← 已解包，调用方拿到的就是 data
  // ... 错误处理
})
```

**新增 API 接口或 Vue 页面时必须检查：** 分页接口返回值直接使用 `res.records` 和 `res.total`，详情接口直接使用 `res` 作为实体对象，不要再套一层 `.data`。

**受影响文件：** `asset-ui/src/views/opr/` 下 13 个 Vue 文件（已全部修正）