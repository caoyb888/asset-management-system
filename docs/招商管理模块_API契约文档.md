# 招商管理模块 — REST API 契约文档

> **生成日期：** 2026-03-19
> **数据来源：** `asset-investment` 模块源码 + `docs/招商管理模块_技术分析报告_修订版.docx` + `docs/招商管理模块-数据库设计_修订版.md`
> **服务端口：** 8002
> **认证方式：** JWT Token（Header: `Authorization: Bearer <token>`）

---

## 目录

- [统一响应格式](#统一响应格式)
- [通用分页参数](#通用分页参数)
- [错误码](#错误码)
- [状态枚举字典](#状态枚举字典)
- [业务类型字典](#业务类型字典)
- [1. 计租方案配置 /inv/config/rent-schemes](#1-计租方案配置--cfgrentschemecontroller-invconfigrent-schemes)
- [2. 收款项目配置 /inv/config/fee-items](#2-收款项目配置--cfgfeeitemcontroller-invconfigfee-items)
- [3. 意向协议管理 /inv/intentions](#3-意向协议管理--invintentioncontroller-invintentions)
- [4. 招商合同管理 /inv/contracts](#4-招商合同管理--invleasecontractcontroller-invcontracts)
- [5. 开业审批管理 /inv/opening-approvals](#5-开业审批管理--invopeningapprovalcontroller-invopening-approvals)
- [6. 租决政策管理 /inv/rent-policies](#6-租决政策管理--invrentpolicycontroller-invrent-policies)
- [7. 租金分解管理 /inv/rent-decomps](#7-租金分解管理--invrentdecompositioncontroller-invrent-decomps)
- [汇总统计](#汇总统计)
- [代码 vs 技术分析报告差异](#代码-vs-技术分析报告差异)

---

## 统一响应格式

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

| 字段 | 类型 | 含义 |
|------|------|------|
| code | int | 状态码（200=成功，其他见错误码表） |
| msg | String | 提示信息 |
| data | T | 业务数据（泛型） |

**分页响应 `data` 结构：**

| 字段 | 类型 | 含义 |
|------|------|------|
| records | `T[]` | 当前页数据列表 |
| total | long | 总记录数 |
| size | long | 每页条数 |
| current | long | 当前页码 |
| pages | long | 总页数 |

---

## 通用分页参数

所有分页查询接口均以 Query String 方式传递：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 20 | 每页条数 |

---

## 错误码

| code | 含义 | 典型场景 |
|------|------|----------|
| 200 | 成功 | 所有成功请求 |
| 400 | 参数校验失败 | @Valid 校验不通过 |
| 401 | 未认证 | JWT Token 缺失或过期 |
| 403 | 无权限 | 角色/数据权限不满足 |
| 404 | 资源不存在 | 按 ID 查询不到数据 |
| 409 | 业务冲突 | 状态不允许操作、有关联数据不可删除等 |
| 500 | 服务器内部错误 | 未捕获异常 |

> **路径说明：** 控制器注解路径为 `/inv/*`。前端通过 Vite 代理 `/api/inv/*` → `localhost:8002` 并 rewrite 去掉 `/api`。技术分析报告中设计的 `/api/v1/investment/*` 前缀未在代码中使用，本文档以代码实际实现为准。

---

## 状态枚举字典

| 适用实体 | 值 | 含义 |
|----------|---|------|
| **意向协议 inv_intention** | 0 | 草稿 |
| | 1 | 审批中 |
| | 2 | 审批通过 |
| | 3 | 驳回 |
| | 4 | 已转合同 |
| **招商合同 inv_lease_contract** | 0 | 草稿 |
| | 1 | 审批中 |
| | 2 | 生效 |
| | 3 | 到期 |
| | 4 | 终止 |
| **开业审批 inv_opening_approval** | 0 | 待提交 |
| | 1 | 审批中 |
| | 2 | 通过 |
| | 3 | 驳回 |
| **租决政策 inv_rent_policy** | 0 | 草稿 |
| | 1 | 审批中 |
| | 2 | 通过 |
| | 3 | 驳回 |
| **租金分解 inv_rent_decomposition** | 0 | 草稿 |
| | 1 | 审批中 |
| | 2 | 通过 |
| | 3 | 驳回 |

---

## 业务类型字典

| 字段名 | 值 | 含义 | 适用场景 |
|--------|---|------|----------|
| **chargeType** 收费方式 | 1 | 固定租金 | cfg_rent_scheme / 意向费项 / 合同费项 |
| | 2 | 固定提成 | |
| | 3 | 阶梯提成 | |
| | 4 | 两者取高 | |
| | 5 | 一次性收费 | |
| **paymentCycle** 支付周期 | 1 | 月付 | 意向/合同/计租方案 |
| | 2 | 两月付 | |
| | 3 | 季付 | |
| | 4 | 四月付 | |
| | 5 | 半年付 | |
| | 6 | 年付 | |
| **billingMode** 账期模式 | 1 | 预付 | 意向/合同 |
| | 2 | 当期 | |
| | 3 | 后付 | |
| **shopCategory** 商铺类别 | 1 | 主力店 | 租决指标 / 租金分解明细 |
| | 2 | 次主力店 | |
| | 3 | 一般商铺 | |
| **contractType** 合同类型 | 1 | 标准租赁合同 | inv_lease_contract |
| | 2 | 临时租赁合同 | |
| | 3 | 补充协议 | |
| **itemType** 费项类型 | 1 | 租金类 | cfg_fee_item |
| | 2 | 保证金类 | |
| | 3 | 服务费类 | |

---

## 1. 计租方案配置 — CfgRentSchemeController `/inv/config/rent-schemes`

### RS-01 查询计租方案列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/config/rent-schemes` |
| **响应** | `R<List<CfgRentScheme>>` |

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| showAll | boolean | 否 | false | true 返回全部（含停用），管理页使用；默认只返回启用方案 |

### RS-02 查询计租方案详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/config/rent-schemes/{id}` |
| **路径参数** | id — Long，方案ID |
| **响应** | `R<CfgRentScheme>` |

### RS-03 新增计租方案

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/config/rent-schemes` |
| **响应** | `R<Long>` — 新建方案ID |

**请求体 CfgRentScheme：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| schemeCode | String | 是 | 方案编码 |
| schemeName | String | 是 | 方案名称 |
| chargeType | Integer | 否 | 默认收费方式（1固定/2固定提成/3阶梯提成/4取高/5一次性） |
| paymentCycle | Integer | 否 | 默认支付周期（1月付/2两月付/3季付/4四月付/5半年付/6年付） |
| billingMode | Integer | 否 | 默认账期模式（1预付/2当期/3后付） |
| formulaJson | JSON | 否 | 租金计算公式配置（JSON 格式） |
| strategyBeanName | String | 否 | 策略 Bean 名称（用于 Spring 策略路由） |
| status | Integer | 否 | 状态（1启用/0停用），默认1 |
| description | String | 否 | 方案说明 |

### RS-04 编辑计租方案

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/config/rent-schemes/{id}` |
| **路径参数** | id — Long，方案ID |
| **请求体** | 同 RS-03 |
| **响应** | `R<Void>` |

### RS-05 启用/停用计租方案

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/config/rent-schemes/{id}/status` |
| **路径参数** | id — Long，方案ID |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | **是** | 0停用 / 1启用 |

### RS-06 删除计租方案

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/inv/config/rent-schemes/{id}` |
| **路径参数** | id — Long，方案ID |
| **响应** | `R<Void>` |

### CfgRentScheme 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| schemeCode | String | 方案编码 |
| schemeName | String | 方案名称 |
| chargeType | Integer | 默认收费方式 |
| paymentCycle | Integer | 默认支付周期 |
| billingMode | Integer | 默认账期模式 |
| formulaJson | JSON | 计算公式配置 |
| strategyBeanName | String | 策略 Bean 名称 |
| status | Integer | 状态（1启用/0停用） |
| description | String | 方案说明 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

## 2. 收款项目配置 — CfgFeeItemController `/inv/config/fee-items`

### FI-01 查询收款项目列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/config/fee-items` |
| **响应** | `R<List<CfgFeeItem>>` — 按 sort_order 升序 |

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| showAll | boolean | 否 | false | true 返回全部（含停用）；默认只返回启用项目 |

### FI-02 新增收款项目

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/config/fee-items` |
| **响应** | `R<Long>` — 新建项目ID |

**请求体 CfgFeeItem：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| itemCode | String | 是 | 项目编码 |
| itemName | String | 是 | 项目名称（租金/保证金/物管费等） |
| itemType | Integer | 否 | 类型（1租金类/2保证金类/3服务费类） |
| isRequired | Integer | 否 | 是否必填（0否/1是），租金类自动强制为1 |
| sortOrder | Integer | 否 | 排序序号 |
| status | Integer | 否 | 状态（1启用/0停用），默认1 |

> **业务规则：** itemType=1（租金类）时自动强制 isRequired=1。

### FI-03 编辑收款项目

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/config/fee-items/{id}` |
| **路径参数** | id — Long |
| **请求体** | 同 FI-02 |
| **响应** | `R<Void>` |

### FI-04 启用/停用收款项目

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/config/fee-items/{id}/status` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | **是** | 0停用 / 1启用 |

### FI-05 批量更新排序

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/config/fee-items/sort` |
| **响应** | `R<Void>` |

**请求体 `List<SortItem>`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Long | **是** | 收款项目ID |
| sortOrder | Integer | **是** | 新排序序号 |

### FI-06 删除收款项目

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/inv/config/fee-items/{id}` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |

### CfgFeeItem 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| itemCode | String | 项目编码 |
| itemName | String | 项目名称 |
| itemType | Integer | 类型（1租金/2保证金/3服务费） |
| isRequired | Integer | 是否必填 |
| sortOrder | Integer | 排序序号 |
| status | Integer | 状态（1启用/0停用） |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

## 3. 意向协议管理 — InvIntentionController `/inv/intentions`

### 状态流转

```
草稿(0) ──[发起审批]──► 审批中(1)
审批中(1) ──[通过回调]──► 审批通过(2)
审批中(1) ──[驳回回调]──► 驳回(3)
驳回(3) ──[重新发起]──► 审批中(1)
审批通过(2) ──[转合同]──► 已转合同(4)
```

### IA-01 分页查询意向协议列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/intentions` |
| **响应** | `R<IPage<InvIntention>>` |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20 |
| projectId | Long | 否 | 项目ID |
| status | Integer | 否 | 状态（0草稿/1审批中/2通过/3驳回/4已转合同） |
| merchantId | Long | 否 | 商家ID |
| brandId | Long | 否 | 品牌ID |
| buildingId | Long | 否 | 楼栋ID（联表 inv_intention_shop 筛选） |
| floorId | Long | 否 | 楼层ID（联表筛选） |
| shopId | Long | 否 | 商铺ID（联表筛选） |
| formatType | String | 否 | 业态（联表筛选） |
| keyword | String | 否 | 关键词（意向名称/编号模糊搜索） |

> **索引命中：** `idx_intention_multi(project_id, status, is_deleted, created_at)`

### IA-02 查询意向协议详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/intentions/{id}` |
| **路径参数** | id — Long，意向协议ID |
| **响应** | `R<InvIntention>` |

### IA-03 新增意向协议（草稿）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/intentions` |
| **响应** | `R<Long>` — 新建意向ID |

**请求体 IntentionSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| intentionName | String | **是** | @NotBlank | 意向协议名称 |
| projectId | Long | **是** | @NotNull | 所属项目ID |
| merchantId | Long | 否 | - | 商家ID |
| brandId | Long | 否 | - | 意向品牌ID |
| signingEntity | String | 否 | - | 签约主体 |
| rentSchemeId | Long | 否 | - | 计租方案ID |
| deliveryDate | LocalDate | 否 | - | 交付日 |
| decorationStart | LocalDate | 否 | - | 装修开始日期 |
| decorationEnd | LocalDate | 否 | - | 装修结束日期 |
| openingDate | LocalDate | 否 | - | 开业日 |
| contractStart | LocalDate | 否 | - | 合同开始日期 |
| contractEnd | LocalDate | 否 | - | 合同结束日期 |
| paymentCycle | Integer | 否 | - | 支付周期（1~6） |
| billingMode | Integer | 否 | - | 账期模式（1~3） |
| agreementText | String | 否 | - | 协议文本内容 |

> **业务规则：** 状态自动置为草稿(0)，系统自动生成意向编号。

### IA-04 编辑意向协议

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/intentions/{id}` |
| **路径参数** | id — Long |
| **请求体** | 同 IntentionSaveDTO |
| **响应** | `R<Void>` |
| **业务约束** | 仅草稿(0)/驳回(3)状态可修改 |

### IA-05 删除意向协议

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/inv/intentions/{id}` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **业务约束** | 审批中(1)和已转合同(4)不可删除 |

### IA-06 暂存意向协议

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/intentions/{id}/draft` |
| **路径参数** | id — Long |
| **请求体** | 同 IntentionSaveDTO |
| **响应** | `R<Void>` |
| **业务约束** | 仅草稿(0)/驳回(3)状态可暂存 |

### IA-07 发起审批

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/intentions/{id}/submit-approval` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<Void>` |
| **状态变更** | 草稿(0)/驳回(3) → 审批中(1) |

### IA-08 审批回调

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/intentions/{id}/approval-callback` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **状态变更** | 审批中(1) → 审批通过(2) 或 驳回(3) |

**请求体 ApprovalCallbackDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| approved | Boolean | **是** | @NotNull | true=通过，false=驳回 |
| approvalId | String | 否 | - | 审批流程实例ID |
| comment | String | 否 | - | 审批意见/驳回原因 |

### IA-09 批量保存商铺关联（全量替换）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/intentions/{id}/shops` |
| **路径参数** | id — Long，意向协议ID |
| **响应** | `R<Void>` |
| **业务约束** | 仅草稿(0)/驳回(3)状态可操作 |

**请求体 `List<IntentionShopItemDTO>`：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| shopId | Long | **是** | @NotNull | 商铺ID |
| buildingId | Long | 否 | - | 楼栋ID |
| floorId | Long | 否 | - | 楼层ID |
| formatType | String | 否 | - | 业态 |
| area | BigDecimal | 否 | - | 租赁面积(㎡) |

### IA-10 查询关联商铺列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/intentions/{id}/shops` |
| **路径参数** | id — Long |
| **响应** | `R<List<InvIntentionShop>>` |

### IA-11 批量保存费项配置（全量替换）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/intentions/{id}/fees` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **业务约束** | 仅草稿(0)/驳回(3)状态可操作；同时级联删除旧阶段数据 |

**请求体 `List<IntentionFeeItemDTO>`：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| feeItemId | Long | 否 | - | 收款项目ID（对应 cfg_fee_item.id） |
| feeName | String | 否 | - | 费项名称（可覆盖收款项目名称） |
| chargeType | Integer | **是** | @NotNull | 收费方式（1固定/2提成/3阶梯/4取高/5一次性） |
| unitPrice | BigDecimal | 否 | - | 单价(元/㎡/月)，固定/取高时必填 |
| area | BigDecimal | 否 | - | 面积(㎡)，固定/取高时必填 |
| startDate | LocalDate | 否 | - | 费项开始日期（不填则用合同开始日期） |
| endDate | LocalDate | 否 | - | 费项结束日期（不填则用合同结束日期） |
| periodIndex | Integer | 否 | - | 租期阶段序号（分段租期递增） |
| formulaParams | JSON | 否 | - | 计算公式参数（见下方示例） |

**formulaParams 参考格式：**

```json
// 固定提成
{"commission_rate": 5.0, "min_commission_amount": 10000}
// 一次性
{"amount": 50000}
// 阶梯提成
{"stages": [{"commission_rate": 5, "min_commission_amount": 8000}, ...]}
```

### IA-12 查询费项列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/intentions/{id}/fees` |
| **路径参数** | id — Long |
| **响应** | `R<List<InvIntentionFee>>` |

### IA-13 批量保存分铺计租阶段（全量替换）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/intentions/{id}/fee-stages` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **业务约束** | 仅草稿(0)/驳回(3)状态可操作 |

**请求体 `List<IntentionFeeStageItemDTO>`：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| intentionFeeId | Long | **是** | @NotNull | 所属费项ID（inv_intention_fee.id） |
| shopId | Long | 否 | - | 商铺ID（分铺计租时填写；整体计租可为空） |
| stageStart | LocalDate | **是** | @NotNull | 阶段开始日期 |
| stageEnd | LocalDate | **是** | @NotNull | 阶段结束日期 |
| unitPrice | BigDecimal | 否 | - | 该阶段单价(元/㎡/月) |
| commissionRate | BigDecimal | 否 | - | 提成比例(%) |
| minCommissionAmount | BigDecimal | 否 | - | 最低提成金额（保底） |

### IA-14 查询分铺计租阶段列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/intentions/{id}/fee-stages` |
| **路径参数** | id — Long |
| **响应** | `R<List<InvIntentionFeeStage>>` |

### IA-15 生成费用明细

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/intentions/{id}/generate-cost` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<Map<String, Object>>` |

> **业务逻辑：** 调用租金计算引擎，按 chargeType 计算各费项金额，汇总写入 `inv_intention.total_amount`。返回各费项明细和总金额。

### IA-16 生成账期

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/intentions/{id}/billing` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<List<InvIntentionBilling>>` — 全量替换后返回新账期列表 |

> **业务逻辑：** 按 paymentCycle + billingMode 拆分账期，写入 `inv_intention_billing`。首账期标记 `billingType=1`，全量替换旧账期。

### IA-17 查询账期列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/intentions/{id}/billing` |
| **路径参数** | id — Long |
| **响应** | `R<List<InvIntentionBilling>>` — 按 billing_start 升序 |

### InvIntentionBilling 字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| intentionId | Long | 意向协议ID |
| feeItemId | Long | 收款项目ID |
| billingStart | LocalDate | 账期开始 |
| billingEnd | LocalDate | 账期结束 |
| dueDate | LocalDate | 应收日期 |
| amount | BigDecimal | 应收金额 |
| billingType | Integer | 账期类型（1首账期/2正常账期） |
| status | Integer | 收款状态（0未收/1部分/2已收） |

---

## 4. 招商合同管理 — InvLeaseContractController `/inv/contracts`

### 状态流转

```
草稿(0) ──[发起审批]──► 审批中(1)
审批中(1) ──[通过回调]──► 生效(2)
审批中(1) ──[驳回回调]──► 草稿(0)（重新编辑）
生效(2) ──[到期]──► 到期(3)（定时任务或手动）
生效(2)/到期(3) ──[终止]──► 终止(4)
```

### LC-01 分页查询合同列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/contracts` |
| **响应** | `R<IPage<InvLeaseContract>>` |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20 |
| projectId | Long | 否 | 项目ID |
| status | Integer | 否 | 状态（0草稿/1审批中/2生效/3到期/4终止） |
| merchantId | Long | 否 | 商家ID |
| keyword | String | 否 | 关键词（合同名称/编号模糊搜索） |

> **索引命中：** `idx_project_status(project_id, status, is_current, is_deleted)`；到期预警使用 `idx_contract_end(contract_end, status, is_current, is_deleted)`

### LC-02 查询合同详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/contracts/{id}` |
| **路径参数** | id — Long，合同ID |
| **响应** | `R<InvLeaseContract>` |

### LC-03 新增合同（草稿）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/contracts` |
| **响应** | `R<Long>` — 新建合同ID |

**请求体 ContractSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractName | String | **是** | @NotBlank | 合同名称 |
| contractType | Integer | **是** | @NotNull | 合同类型（1标准租赁/2临时租赁/3补充协议） |
| projectId | Long | **是** | @NotNull | 所属项目ID |
| merchantId | Long | 否 | - | 商家ID |
| brandId | Long | 否 | - | 品牌ID |
| signingEntity | String | 否 | - | 签约主体（乙方名称） |
| rentSchemeId | Long | 否 | - | 计租方案ID |
| deliveryDate | LocalDate | 否 | - | 交付日 |
| decorationStart | LocalDate | 否 | - | 装修开始日期 |
| decorationEnd | LocalDate | 否 | - | 装修结束日期 |
| openingDate | LocalDate | 否 | - | 开业日 |
| contractStart | LocalDate | 否 | - | 合同开始日期 |
| contractEnd | LocalDate | 否 | - | 合同结束日期 |
| paymentCycle | Integer | 否 | - | 支付周期（1~6） |
| billingMode | Integer | 否 | - | 账期模式（1~3） |
| contractText | String | 否 | - | 合同文本 |

### LC-04 编辑合同

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/contracts/{id}` |
| **路径参数** | id — Long |
| **请求体** | 同 ContractSaveDTO |
| **响应** | `R<Void>` |
| **业务约束** | 仅草稿(0)状态可修改 |

### LC-05 删除合同

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/inv/contracts/{id}` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **业务约束** | 审批中(1)/生效(2)状态不可删除 |

### LC-06 意向协议转合同

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/contracts/from-intention/{intentionId}` |
| **路径参数** | intentionId — Long，意向协议ID |
| **请求体** | ContractSaveDTO（补录合同专属信息，如 contractType） |
| **响应** | `R<Long>` — 新建合同ID |

> **核心逻辑（事务 + 分布式锁）：**
> 1. 校验意向状态必须为"审批通过"(2)
> 2. 校验商铺未被其他有效合同占用
> 3. 获取 Redis 分布式锁（key: `shop:contract:{shopId}`，TTL 30s）
> 4. 双重检查锁后迁移数据：intention → contract（含 shop/fee/fee_stage/billing）
> 5. 更新意向状态为"已转合同"(4)，更新商铺状态为"已签约"
> 6. 写合同版本快照至 inv_lease_contract_version
> 7. 释放分布式锁

### LC-07 发起审批

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/contracts/{id}/submit-approval` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<Void>` |
| **状态变更** | 草稿(0) → 审批中(1) |

### LC-08 审批回调

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/contracts/{id}/approval-callback` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **状态变更** | 审批中(1) → 生效(2) 或 草稿(0) |

**请求体 ContractApprovalCallbackDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| approved | Boolean | **是** | @NotNull | true=通过(→生效)，false=驳回(→草稿) |
| approvalId | String | 否 | - | 审批流程实例ID |
| comment | String | 否 | - | 审批意见/驳回原因 |

### LC-09 更新合同状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/contracts/{id}/status` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | **是** | 目标状态（3到期/4终止） |

### LC-10 批量保存商铺关联（全量替换）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/contracts/{id}/shops` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |

**请求体 `List<ContractShopItemDTO>`：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| shopId | Long | **是** | @NotNull | 商铺ID |
| buildingId | Long | 否 | - | 楼栋ID |
| floorId | Long | 否 | - | 楼层ID |
| formatType | String | 否 | - | 业态 |
| area | BigDecimal | 否 | - | 租赁面积(㎡) |
| rentUnitPrice | BigDecimal | 否 | - | 租金单价(元/㎡/月) |
| propertyUnitPrice | BigDecimal | 否 | - | 物业费单价(元/㎡/月) |

### LC-11 查询合同关联商铺列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/contracts/{id}/shops` |
| **路径参数** | id — Long |
| **响应** | `R<List<InvLeaseContractShop>>` |

### LC-12 批量保存费项配置（全量替换）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/contracts/{id}/fees` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |

**请求体 `List<ContractFeeItemDTO>`：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| feeItemId | Long | 否 | - | 收款项目ID |
| feeName | String | 否 | - | 费项名称 |
| chargeType | Integer | **是** | @NotNull | 收费方式（1~5） |
| unitPrice | BigDecimal | 否 | - | 单价(元/㎡/月) |
| area | BigDecimal | 否 | - | 面积(㎡) |
| startDate | LocalDate | 否 | - | 费项开始日期 |
| endDate | LocalDate | 否 | - | 费项结束日期 |
| periodIndex | Integer | 否 | - | 租期阶段序号 |
| formulaParams | JSON | 否 | - | 计算公式参数 |

### LC-13 查询合同费项列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/contracts/{id}/fees` |
| **路径参数** | id — Long |
| **响应** | `R<List<InvLeaseContractFee>>` |

### LC-14 批量保存分铺计租阶段

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/contracts/{id}/fee-stages` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |

**请求体 `List<ContractFeeStageItemDTO>`：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractFeeId | Long | **是** | @NotNull | 合同费项ID（inv_lease_contract_fee.id） |
| shopId | Long | 否 | - | 商铺ID |
| stageStart | LocalDate | **是** | @NotNull | 阶段开始日期 |
| stageEnd | LocalDate | **是** | @NotNull | 阶段结束日期 |
| unitPrice | BigDecimal | 否 | - | 该阶段单价 |
| commissionRate | BigDecimal | 否 | - | 提成比例(%) |
| minCommissionAmount | BigDecimal | 否 | - | 最低提成金额 |

### LC-15 查询分铺计租阶段列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/contracts/{id}/fee-stages` |
| **路径参数** | id — Long |
| **响应** | `R<List<InvLeaseContractFeeStage>>` |

### LC-16 生成费用明细

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/contracts/{id}/generate-cost` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<Map<String, Object>>` — 费项明细 + 总金额 |

### LC-17 生成账期

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/contracts/{id}/billing` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<List<InvLeaseContractBilling>>` |

### LC-18 查询账期列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/contracts/{id}/billing` |
| **路径参数** | id — Long |
| **响应** | `R<List<InvLeaseContractBilling>>` |

### LC-19 查询合同版本历史列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/contracts/{id}/versions` |
| **路径参数** | id — Long，合同ID |
| **响应** | `R<List<InvLeaseContractVersion>>` — 按版本降序 |

### InvLeaseContractVersion 字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| contractId | Long | 合同ID |
| version | Integer | 版本号 |
| snapshotData | JSON | 完整合同数据快照 |
| changeReason | String | 变更原因 |
| createdBy | Long | 创建人ID |
| createdAt | LocalDateTime | 创建时间 |

---

## 5. 开业审批管理 — InvOpeningApprovalController `/inv/opening-approvals`

### 状态流转

```
草稿(0) ──[提交审批]──► 审批中(1)
审批中(1) ──[通过回调]──► 通过(2)
审批中(1) ──[驳回回调]──► 驳回(3)（自动生成 snapshotData）
驳回(3) ──[基于历史创建]──► 新单草稿(0)
```

### OA-01 分页查询开业审批列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/opening-approvals` |
| **响应** | `R<IPage<InvOpeningApproval>>` |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20 |
| projectId | Long | 否 | 项目ID |
| status | Integer | 否 | 状态（0待提交/1审批中/2通过/3驳回） |
| contractId | Long | 否 | 关联合同ID |

### OA-02 查询详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/opening-approvals/{id}` |
| **路径参数** | id — Long |
| **响应** | `R<InvOpeningApproval>` |

### OA-03 新增开业审批（草稿）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/opening-approvals` |
| **响应** | `R<Long>` — 新建审批ID |

**请求体 InvOpeningApproval（直接使用实体）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | **是** | 项目ID |
| buildingId | Long | 否 | 楼栋ID |
| floorId | Long | 否 | 楼层ID |
| shopId | Long | 否 | 商铺ID |
| contractId | Long | 否 | 关联合同ID |
| merchantId | Long | 否 | 商家ID |
| plannedOpeningDate | LocalDate | 否 | 计划开业日期 |
| actualOpeningDate | LocalDate | 否 | 实际开业日期 |
| remark | String | 否 | 备注 |

> **自动设置：** status=0（草稿），approvalCode 自动生成（格式：`OA000001`）。

### OA-04 编辑开业审批

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/opening-approvals/{id}` |
| **路径参数** | id — Long |
| **请求体** | 同 OA-03 |
| **响应** | `R<Void>` |
| **业务约束** | 审批中(1)不可修改 |

### OA-05 删除开业审批

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/inv/opening-approvals/{id}` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **业务约束** | 审批中(1)/已通过(2)不可删除 |

### OA-06 提交审批

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/opening-approvals/{id}/submit` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<Void>` |
| **状态变更** | 草稿(0) → 审批中(1) |
| **业务约束** | 仅草稿状态可提交 |

### OA-07 审批回调

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/opening-approvals/{id}/approval-callback` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **状态变更** | 审批中(1) → 通过(2) 或 驳回(3) |

**请求体 `Map<String, Object>`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approved | Boolean | **是** | true=通过，false=驳回 |

> **驳回逻辑：** 驳回时自动将当前单据数据序列化写入 `snapshotData`（JSON），便于后续"基于历史创建"接口恢复数据。

### OA-08 基于历史驳回单据创建新审批

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/opening-approvals/from-previous/{id}` |
| **路径参数** | id — Long，被驳回的原单ID |
| **请求参数** | 无 |
| **响应** | `R<Long>` — 新建审批单ID |
| **业务约束** | 仅驳回(3)状态的记录可创建 |

> **业务逻辑：** 从原单复制全部业务字段 → 新单状态为草稿(0) → 设置 `previousApprovalId` 关联原单 → 生成新 approvalCode。

### OA-09 查询附件列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/opening-approvals/{id}/attachments` |
| **路径参数** | id — Long，审批单ID |
| **响应** | `R<List<InvOpeningAttachment>>` |

### OA-10 新增附件记录

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/opening-approvals/{id}/attachments` |
| **路径参数** | id — Long，审批单ID |
| **响应** | `R<Long>` — 附件记录ID |

**请求体 InvOpeningAttachment：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fileName | String | 否 | 文件名 |
| fileUrl | String | 否 | 文件地址（OSS URL） |
| fileType | String | 否 | 文件类型 |
| fileSize | Long | 否 | 文件大小（字节） |

### OA-11 删除附件

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/inv/opening-approvals/attachments/{attachmentId}` |
| **路径参数** | attachmentId — Long，附件ID |
| **响应** | `R<Void>` |

### InvOpeningApproval 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| approvalCode | String | 审批单号（自动生成） |
| projectId | Long | 项目ID |
| buildingId | Long | 楼栋ID |
| floorId | Long | 楼层ID |
| shopId | Long | 商铺ID |
| shopCode | String | 商铺编码（联表冗余，非DB字段） |
| contractId | Long | 关联合同ID |
| contractCode | String | 合同编号（联表冗余，非DB字段） |
| merchantId | Long | 商家ID |
| merchantName | String | 商家名称（联表冗余，非DB字段） |
| plannedOpeningDate | LocalDate | 计划开业日期 |
| actualOpeningDate | LocalDate | 实际开业日期 |
| status | Integer | 状态（0待提交/1审批中/2通过/3驳回） |
| approvalId | String | 审批流程实例ID |
| remark | String | 备注 |
| previousApprovalId | Long | 被驳回原单ID |
| snapshotData | JSON | 驳回时数据快照 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

## 6. 租决政策管理 — InvRentPolicyController `/inv/rent-policies`

### 状态流转

```
草稿(0) ──[提交审批]──► 审批中(1)
审批中(1) ──[通过回调]──► 通过(2)
审批中(1) ──[驳回回调]──► 驳回(3)
驳回(3) ──[重新提交]──► 审批中(1)
```

### RP-01 分页查询租决政策列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/rent-policies` |
| **响应** | `R<IPage<InvRentPolicy>>` — 按创建时间降序 |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20 |
| projectId | Long | 否 | 项目ID |
| status | Integer | 否 | 状态（0草稿/1审批中/2通过/3驳回） |

### RP-02 查询详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/rent-policies/{id}` |
| **路径参数** | id — Long |
| **响应** | `R<InvRentPolicy>` |

### RP-03 获取已审批通过的政策列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/rent-policies/approved` |
| **响应** | `R<List<InvRentPolicy>>` — 按创建时间降序 |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |

> **用途：** 供租金分解新增时选择关联的已通过政策。

### RP-04 新增租决政策（草稿）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-policies` |
| **响应** | `R<Long>` — 新建政策ID |

**请求体 InvRentPolicy（直接使用实体）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | **是** | 项目ID |
| policyType | Integer | 否 | 租金决策类型 |
| year1Rent | BigDecimal | 否 | 第一年租金指标 |
| year2Rent | BigDecimal | 否 | 第二年租金指标 |
| year1PropertyFee | BigDecimal | 否 | 第一年物业指标 |
| year2PropertyFee | BigDecimal | 否 | 第二年物业指标 |
| shopAttr | String | 否 | 适用铺位属性 |
| formatType | String | 否 | 适用业态 |
| minLeaseTerm | Integer | 否 | 租期范围-最小(月) |
| maxLeaseTerm | Integer | 否 | 租期范围-最大(月) |
| rentGrowthRate | BigDecimal | 否 | 租金增长率(%) |
| feeGrowthRate | BigDecimal | 否 | 管理费增长率(%) |
| freeRentPeriod | Integer | 否 | 免租期(月) |
| depositMonths | Integer | 否 | 租赁保证金月数 |
| paymentCycle | Integer | 否 | 支付周期 |

> **自动设置：** status=0（草稿），policyCode 自动生成（格式：`RP20260001`）。

### RP-05 编辑租决政策

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/rent-policies/{id}` |
| **路径参数** | id — Long |
| **请求体** | 同 RP-04 |
| **响应** | `R<Void>` |
| **业务约束** | 审批中(1)不可修改 |

### RP-06 删除租决政策

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/inv/rent-policies/{id}` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **业务约束** | 审批中(1)/已通过(2)不可删除；有关联租金分解记录不可删除（RP-08） |

> **级联操作：** 删除时自动级联删除该政策下的全部分类指标。

### RP-07 提交审批

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-policies/{id}/submit-approval` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<Void>` |
| **状态变更** | 草稿(0)/驳回(3) → 审批中(1) |

### RP-08 审批回调

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-policies/{id}/approval-callback` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **状态变更** | 审批中(1) → 通过(2) 或 驳回(3) |

**请求体 `Map<String, Object>`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approved | Boolean | **是** | true=通过，false=驳回 |

### RP-09 查询分类指标列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/rent-policies/{id}/indicators` |
| **路径参数** | id — Long，政策ID |
| **响应** | `R<List<InvRentPolicyIndicator>>` — 按 shopCategory 升序 |

### RP-10 批量保存分类指标（全量替换）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-policies/{id}/indicators` |
| **路径参数** | id — Long，政策ID |
| **响应** | `R<Void>` |
| **业务约束** | 审批中(1)不可修改指标 |

**请求体 `List<InvRentPolicyIndicator>`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| shopCategory | Integer | **是** | 商铺类别（1主力店/2次主力店/3一般商铺） |
| rentPrice | BigDecimal | 否 | 租金单价(元/㎡·月) |
| propertyFeePrice | BigDecimal | 否 | 物管费单价(元/㎡·月) |
| formatType | String | 否 | 业态 |
| rentGrowthRate | BigDecimal | 否 | 租金增长率(%，覆盖政策级) |
| feeGrowthRate | BigDecimal | 否 | 管理费增长率(%) |
| freeRentMonths | Integer | 否 | 免租期(月) |
| depositMonths | Integer | 否 | 保证金月数 |

> **约束：** 同一政策下每个 shopCategory 仅允许一条记录（唯一约束 `uk_policy_category`）。

---

## 7. 租金分解管理 — InvRentDecompositionController `/inv/rent-decomps`

### RD-01 分页查询租金分解列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/rent-decomps` |
| **响应** | `R<IPage<InvRentDecomposition>>` — 按创建时间降序 |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页条数，默认20 |
| projectId | Long | 否 | 项目ID |
| status | Integer | 否 | 状态（0草稿/1审批中/2通过/3驳回） |

### RD-02 查询详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/rent-decomps/{id}` |
| **路径参数** | id — Long |
| **响应** | `R<InvRentDecomposition>` |

### RD-03 新增租金分解（草稿）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-decomps` |
| **响应** | `R<Long>` — 新建分解ID |

**请求体 InvRentDecomposition（直接使用实体）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | **是** | 项目ID |
| policyId | Long | **是** | 关联租决政策ID |

> **自动设置：** status=0（草稿），decompCode 自动生成（格式：`RD20260001`）。创建时自动快照关联政策的关键参数至 `policySnapshot`。

### RD-04 编辑租金分解基础信息

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/rent-decomps/{id}` |
| **路径参数** | id — Long |
| **请求体** | InvRentDecomposition 部分字段 |
| **响应** | `R<Void>` |

### RD-05 重新关联租决政策

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/inv/rent-decomps/{id}/re-link-policy` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |

**请求体 `Map<String, Object>`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| policyId | Long | **是** | 新的租决政策ID（必须为已通过状态） |

> **业务逻辑：** 人工触发，更新 policyId 并重新生成 policySnapshot。

### RD-06 删除租金分解

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/inv/rent-decomps/{id}` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **业务约束** | 审批中(1)/已通过(2)不可删除；级联删除全部明细 |

### RD-07 提交审批

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-decomps/{id}/submit-approval` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<Void>` |
| **状态变更** | 草稿(0)/驳回(3) → 审批中(1) |

### RD-08 审批回调

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-decomps/{id}/approval-callback` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |
| **状态变更** | 审批中(1) → 通过(2) 或 驳回(3) |

**请求体 `Map<String, Object>`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approved | Boolean | **是** | true=通过，false=驳回 |

### RD-09 查询明细列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/rent-decomps/{id}/details` |
| **路径参数** | id — Long，分解ID |
| **响应** | `R<List<InvRentDecompDetail>>` — 按 shopCategory + id 排序 |

### RD-10 批量保存明细（全量替换）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-decomps/{id}/details` |
| **路径参数** | id — Long |
| **响应** | `R<Void>` |

**请求体 `List<InvRentDecompDetail>`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| shopCategory | Integer | 否 | 商铺类别（1主力/2次主力/3一般） |
| formatType | String | 否 | 业态 |
| rentUnitPrice | BigDecimal | 否 | 租金单价(元/㎡/月) |
| propertyUnitPrice | BigDecimal | 否 | 物管费单价(元/㎡/月) |
| area | BigDecimal | 否 | 面积(㎡) |
| remark | String | 否 | 备注 |

> **自动计算：** `annualRent = rentUnitPrice × area × 12`；`annualFee = propertyUnitPrice × area × 12`。

### RD-11 自动汇总计算

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-decomps/{id}/calculate` |
| **路径参数** | id — Long |
| **请求参数** | 无 |
| **响应** | `R<Map<String, Object>>` |

**响应 data 结构：**

| 字段 | 类型 | 含义 |
|------|------|------|
| totalAnnualRent | BigDecimal | 标准年租金汇总（∑annualRent） |
| totalAnnualFee | BigDecimal | 标准年物管费汇总（∑annualFee） |
| detailCount | int | 明细条数 |

> **业务逻辑：** 汇总全部明细的 annualRent/annualFee，写入主表 totalAnnualRent/totalAnnualFee。

### RD-12 Excel 批量导入明细

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/inv/rent-decomps/{id}/import` |
| **路径参数** | id — Long，分解ID |
| **Content-Type** | `multipart/form-data` |
| **响应** | `R<Map<String, Object>>` |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | **是** | Excel 文件 |

**响应 data 结构：**

| 字段 | 类型 | 含义 |
|------|------|------|
| successCount | int | 成功导入条数 |
| errorCount | int | 失败条数 |
| errors | List\<String\> | 错误明细列表（逐行错误原因） |

**Excel 模板列（RentDecompDetailExcel）：**

| 列名 | 类型 | 说明 | 校验规则 |
|------|------|------|----------|
| 商铺类别(1主力/2次主力/3一般) | Integer | shopCategory | 必填，值为1/2/3 |
| 业态 | String | formatType | 可选 |
| 租金单价(元/㎡/月) | BigDecimal | rentUnitPrice | 必填，不能为负 |
| 物管费单价(元/㎡/月) | BigDecimal | propertyUnitPrice | 可选，默认0 |
| 面积(㎡) | BigDecimal | area | 必填，须大于0 |
| 标准年租金(元) | BigDecimal | annualRent | 导出时自动计算 |
| 标准年物管费(元) | BigDecimal | annualFee | 导出时自动计算 |
| 备注 | String | remark | 可选 |

> **业务规则：** 导入时覆盖写入（先清除旧明细），自动计算 annualRent/annualFee。分批写入，每批 500 条。

### RD-13 导出明细 Excel 报表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/inv/rent-decomps/{id}/export` |
| **路径参数** | id — Long，分解ID |
| **响应** | Excel 文件流（`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`） |

> **文件名格式：** `{decompCode}_明细.xlsx`

### InvRentDecomposition 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| decompCode | String | 分解编号（自动生成） |
| projectId | Long | 项目ID |
| policyId | Long | 关联租决政策ID |
| totalAnnualRent | BigDecimal | 标准年租金汇总 |
| totalAnnualFee | BigDecimal | 标准年物管费汇总 |
| status | Integer | 状态（0草稿/1审批中/2通过/3驳回） |
| approvalId | String | 审批流程实例ID |
| policySnapshot | JSON | 租决政策关键参数快照 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

### InvRentDecompDetail 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| decompId | Long | 租金分解ID |
| shopCategory | Integer | 商铺类别（1主力/2次主力/3一般） |
| formatType | String | 业态 |
| rentUnitPrice | BigDecimal | 租金单价(元/㎡/月) |
| propertyUnitPrice | BigDecimal | 物管费单价(元/㎡/月) |
| area | BigDecimal | 面积(㎡) |
| annualRent | BigDecimal | 标准年租金（自动计算） |
| annualFee | BigDecimal | 标准年物管费（自动计算） |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

## 汇总统计

| 维度 | 数量 |
|------|------|
| 控制器 | 7 |
| API 端点总数 | **68** |
| 查询 DTO | 2（IntentionQueryDTO, ContractQueryDTO） |
| 保存/操作 DTO | 10（IntentionSaveDTO, IntentionShopItemDTO, IntentionFeeItemDTO, IntentionFeeStageItemDTO, ApprovalCallbackDTO, ContractSaveDTO, ContractShopItemDTO, ContractFeeItemDTO, ContractFeeStageItemDTO, ContractApprovalCallbackDTO） |
| 实体（直接作为请求/响应） | 12 |
| Excel 导入行模型 | 1（RentDecompDetailExcel） |

**各控制器端点分布：**

| 控制器 | 端点数 |
|--------|--------|
| CfgRentSchemeController | 6 |
| CfgFeeItemController | 6 |
| InvIntentionController | 17 |
| InvLeaseContractController | 19 |
| InvOpeningApprovalController | 11 |
| InvRentPolicyController | 10 |
| InvRentDecompositionController | 13 |

> **注：** 含 5 类审批流程（意向协议、招商合同、开业审批、租决政策、租金分解），均提供 submit-approval + approval-callback 成对端点。当前阶段审批引擎为 Mock 实现，后续对接真实 OA 系统。

---

## 代码 vs 技术分析报告差异

| 差异点 | 技术报告设计 | 代码实际实现 |
|--------|------------|------------|
| **路径前缀** | `/api/v1/investment/*` | `/inv/*` |
| **配置管理路径** | 未明确 | `/inv/config/rent-schemes` + `/inv/config/fee-items` |
| **意向协议审批** | 统一审批引擎 | Mock 审批回调接口（`/approval-callback`），预留 approvalId 字段 |
| **开业审批提交** | `POST /{id}/submit` | 代码实现一致：`POST /{id}/submit` |
| **开业审批驳回处理** | 报告设计为修改原单 | 代码实现为"不可修改原单，基于历史创建新单"（`POST /from-previous/{id}`） |
| **租金分解表数** | 报告设计 20 张表 | 代码实际 19 张表（`biz_shop_relation` 属于基础数据模块） |
| **合同费项 formulaParams** | 报告未强调 | 代码保证意向→合同费项的 `formulaParams` 完整迁移 |
| **合同驳回后状态** | 报告为驳回(3) | 代码为回到草稿(0)，可直接重新编辑 |
| **费项/阶段/账期批量保存** | 报告设计为增量 | 代码实现为全量替换（先删后插），更简洁可靠 |
| **分布式锁实现** | 报告描述 Redis 锁 | 代码使用 Redisson 分布式锁（key: `shop:contract:{shopId}`） |
| **排序更新** | 报告未涉及 | 代码提供 `PUT /inv/config/fee-items/sort` 批量排序 |
| **政策快照** | 报告提到快照机制 | 代码在创建分解时自动快照，并提供 `re-link-policy` 手动更新 |
