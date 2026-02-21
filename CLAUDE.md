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