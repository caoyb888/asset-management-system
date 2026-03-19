# 产城资管系统 — REST API 契约文档

> **认证方式：** JWT Token（Header: `Authorization: Bearer <token>`）

---

## 目录

### 公共部分

- [统一响应格式](#统一响应格式)
- [通用分页参数](#通用分页参数)
- [错误码](#错误码)

### 一、基础数据管理（asset-base，端口 8001）

- [0. 认证管理 /auth](#0-认证管理--authcontroller-auth)
- [1. 项目管理 /base/projects](#1-项目管理--bizprojectcontroller-baseprojects)
- [2. 楼栋管理 /base/buildings](#2-楼栋管理--bizbuildingcontroller-basebuildings)
- [3. 楼层管理 /base/floors](#3-楼层管理--bizfloorcontroller-basefloors)
- [4. 商铺管理 /base/shops](#4-商铺管理--bizshopcontroller-baseshops)
- [5. 品牌管理 /base/brands](#5-品牌管理--bizbrandcontroller-basebrands)
- [6. 商家管理 /base/merchants](#6-商家管理--bizmerchantcontroller-basemerchants)
- [7. 通知公告 /base/notices](#7-通知公告--biznoticecontroller-basenotices)
- [8. 新闻资讯 /base/news](#8-新闻资讯--biznewscontroller-basenews)
- [9. 公司管理 /base/companies](#9-公司管理--syscompanycontroller-basecompanies)
- [10. 用户管理 /base/users](#10-用户管理--sysusercontroller-baseusers)
- [基础数据管理 — 汇总统计](#基础数据管理--汇总统计)
- [基础数据管理 — 代码 vs 技术分析报告差异](#基础数据管理--代码-vs-技术分析报告差异)

### 二、招商管理（asset-investment，端口 8002）

- [招商管理 — 状态枚举字典](#招商管理--状态枚举字典)
- [招商管理 — 业务类型字典](#招商管理--业务类型字典)
- [11. 计租方案配置 /inv/config/rent-schemes](#11-计租方案配置--cfgrentschemecontroller-invconfigrent-schemes)
- [12. 收款项目配置 /inv/config/fee-items](#12-收款项目配置--cfgfeeitemcontroller-invconfigfee-items)
- [13. 意向协议管理 /inv/intentions](#13-意向协议管理--invintentioncontroller-invintentions)
- [14. 招商合同管理 /inv/contracts](#14-招商合同管理--invleasecontractcontroller-invcontracts)
- [15. 开业审批管理 /inv/opening-approvals](#15-开业审批管理--invopeningapprovalcontroller-invopening-approvals)
- [16. 租决政策管理 /inv/rent-policies](#16-租决政策管理--invrentpolicycontroller-invrent-policies)
- [17. 租金分解管理 /inv/rent-decomps](#17-租金分解管理--invrentdecompositioncontroller-invrent-decomps)
- [招商管理 — 汇总统计](#招商管理--汇总统计)
- [招商管理 — 代码 vs 技术分析报告差异](#招商管理--代码-vs-技术分析报告差异)

### 三、营运管理（asset-operation，端口 8003）

- [营运管理 — 状态枚举字典](#营运管理--状态枚举字典)
- [营运管理 — 业务类型字典](#营运管理--业务类型字典)
- [18. 合同台账管理 /opr/ledgers](#18-合同台账管理--oprcontractledgercontroller-oprledgers)
- [19. 合同变更管理 /opr/contract-changes](#19-合同变更管理--oprcontractchangecontroller-oprcontract-changes)
- [20. 营收填报与浮动租金 /opr/revenue-reports + /opr/floating-rents](#20-营收填报与浮动租金--oprrevenuereportcontroller-oprrevenue-reports--oprfloating-rents)
- [21. 客流填报管理 /opr/passenger-flows](#21-客流填报管理--oprpassengerflowcontroller-oprpassenger-flows)
- [22. 合同解约管理 /opr/terminations](#22-合同解约管理--oprcontractterminationcontroller-oprterminations)
- [23. 预警记录管理 /opr/alerts](#23-预警记录管理--opralertrecordcontroller-opralerts)
- [营运管理 — 汇总统计](#营运管理--汇总统计)
- [营运管理 — 代码 vs 技术分析报告差异](#营运管理--代码-vs-技术分析报告差异)

### 四、财务管理（asset-finance，端口 8004）

- [财务管理 — 状态枚举字典](#财务管理--状态枚举字典)
- [财务管理 — 业务类型字典](#财务管理--业务类型字典)
- [24. 应收管理 /fin/receivables](#24-应收管理--finreceivablecontroller-finreceivables)
- [25. 收款管理 /fin/receipts](#25-收款管理--finreceiptcontroller-finreceipts)
- [26. 核销管理 /fin/write-offs](#26-核销管理--finwriteoffcontroller-finwrite-offs)
- [27. 凭证管理 /fin/vouchers](#27-凭证管理--finvouchercontroller-finvouchers)
- [28. 保证金管理 /fin/deposits](#28-保证金管理--findepositcontroller-findeposits)
- [29. 预收款管理 /fin/prepayments](#29-预收款管理--finprepaymentcontroller-finprepayments)
- [30. 财务看板 /fin/dashboard](#30-财务看板--findashboardcontroller-findashboard)
- [财务管理 — 汇总统计](#财务管理--汇总统计)
- [财务管理 — 代码 vs 技术分析报告差异](#财务管理--代码-vs-技术分析报告差异)

### 五、报表管理（asset-report，端口 8005）

- [报表管理 — 通用查询参数](#报表管理--通用查询参数)
- [报表管理 — 数据权限与脱敏机制](#报表管理--数据权限与脱敏机制)
- [报表管理 — 业务类型字典](#报表管理--业务类型字典)
- [31. 资产类报表 /rpt/asset](#31-资产类报表--reportassetcontroller-rptasset)
- [32. 招商类报表 /rpt/inv](#32-招商类报表--reportinvestmentcontroller-rptinv)
- [33. 营运类报表 /rpt/opr](#33-营运类报表--reportoperationcontroller-rptopr)
- [34. 财务类报表 /rpt/fin](#34-财务类报表--reportfinancecontroller-rptfin)
- [35. 报表导出 /rpt/common/export](#35-报表导出--reportexportcontroller-rptcommonexport)
- [36. 数据钻取 /rpt/common/drill-down](#36-数据钻取--drilldowncontroller-rptcommondrill-down)
- [37. 报表收藏 /rpt/common/favorites](#37-报表收藏--favoritecontroller-rptcommonfavorites)
- [38. 定时推送 /rpt/common/schedule-tasks](#38-定时推送--scheduletaskcontroller-rptcommonschedule-tasks)
- [39. ETL 手动触发 /rpt/etl](#39-etl-手动触发--etltriggercontroller-rptetl)
- [40. 报表权限 /rpt/common](#40-报表权限--reportpermissioncontroller-rptcommon)
- [报表管理 — 汇总统计](#报表管理--汇总统计)
- [报表管理 — 代码 vs 技术分析报告差异](#报表管理--代码-vs-技术分析报告差异)

### 六、系统管理（asset-system，端口 8006）

- [系统管理 — 状态枚举字典](#系统管理--状态枚举字典)
- [系统管理 — 认证与安全机制](#系统管理--认证与安全机制)
- [41. 认证管理 /auth](#41-认证管理--sysauthcontroller-auth)
- [42. 用户管理 /sys/users](#42-用户管理--sysusercontroller-sysusers)
- [43. 机构管理 /sys/depts](#43-机构管理--sysdeptcontroller-sysdepts)
- [44. 岗位管理 /sys/posts](#44-岗位管理--syspostcontroller-sysposts)
- [45. 角色管理 /sys/roles](#45-角色管理--sysrolecontroller-sysroles)
- [46. 菜单管理 /sys/menus](#46-菜单管理--sysmenucontroller-sysmenus)
- [47. 业务字典管理 /sys/dict](#47-业务字典管理--sysdictcontroller-sysdict)
- [48. 操作日志管理 /sys/logs](#48-操作日志管理--sysoperlogcontroller-syslogs)
- [49. 登录日志管理 /sys/login-logs](#49-登录日志管理--sysloginlogcontroller-syslogin-logs)
- [50. 在线用户管理 /sys/online-users](#50-在线用户管理--sysonlineusercontroller-sysonline-users)
- [51. 编码规则管理 /sys/code-rules](#51-编码规则管理--syscoderulecontroller-syscode-rules)
- [52. 租费算法管理 /sys/fee-algorithms](#52-租费算法管理--sysfeealgorithmcontroller-sysfee-algorithms)
- [53. 分类管理 /sys/categories](#53-分类管理--syscategorycontroller-syscategories)
- [54. 系统参数配置 /sys/configs](#54-系统参数配置--sysconfigcontroller-sysconfigs)
- [55. 数据权限调试 /sys/data-scope](#55-数据权限调试--datascopecontroller-sysdata-scope)
- [系统管理 — 数据表清单](#系统管理--数据表清单)
- [系统管理 — 汇总统计](#系统管理--汇总统计)
- [系统管理 — 代码 vs 技术分析报告差异](#系统管理--代码-vs-技术分析报告差异)

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

所有分页查询接口均继承 `PageQuery` 基类，以 Query String 方式传递：

| 参数 | 类型 | 必填 | 默认值 | 校验 | 说明 |
|------|------|------|--------|------|------|
| pageNum | int | 否 | 1 | @Min(1) | 页码 |
| pageSize | int | 否 | 20 | @Min(1) @Max(500) | 每页条数 |
| orderBy | String | 否 | - | - | 排序字段 |
| orderDirection | String | 否 | "asc" | - | 排序方向（asc/desc） |

---

## 错误码

| code | 含义 | 典型场景 |
|------|------|----------|
| 200 | 成功 | 所有成功请求 |
| 400 | 参数校验失败 | @Valid 校验不通过（@NotBlank/@NotNull/@Size 等） |
| 401 | 未认证 | JWT Token 缺失或过期 |
| 403 | 无权限 | 角色/数据权限不满足 |
| 404 | 资源不存在 | 按 ID 查询不到数据 |
| 409 | 业务冲突 | 编码重复、有关联数据不可删除等 |
| 500 | 服务器内部错误 | 未捕获异常 |

> **路径说明：** 控制器注解路径为 `/base/*`、`/auth/*`。前端通过 Vite 代理 `/api/base/*` → `localhost:8001` 并 rewrite 去掉 `/api`。技术分析报告中设计的 `/api/v1/base-data/*` 前缀未在代码中使用，本文档以代码实际实现为准。

---

## 0. 认证管理 — AuthController `/auth`

### AUTH-01 获取 SM2 公钥

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/auth/publicKey` |
| **认证** | 否（白名单） |
| **请求参数** | 无 |
| **响应** | `R<String>` — SM2 公钥十六进制字符串 |

### AUTH-02 登录

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/auth/login` |
| **认证** | 否（白名单） |

**请求体 LoginRequest：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | SM2 加密后的密码 |

**响应 `R<Map<String, String>>`：**

| 字段 | 类型 | 含义 |
|------|------|------|
| token | String | JWT Token |

### AUTH-03 获取当前用户信息

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/auth/userInfo` |
| **认证** | 是 |
| **请求参数** | 无（从 Token 中解析） |
| **响应** | `R<Map<String, Object>>` — 用户ID/用户名/真实姓名/角色等 |

### AUTH-04 登出

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/auth/logout` |
| **认证** | 是 |
| **请求参数** | 无 |
| **响应** | `R<Void>` |

---

## 1. 项目管理 — BizProjectController `/base/projects`

### P-01 项目分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/projects` |
| **响应** | `R<IPage<ProjectVO>>` |

**查询参数 ProjectQuery（含 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectName | String | 否 | 项目名称（模糊匹配） |
| projectCode | String | 否 | 项目编号（模糊匹配） |
| operationStatus | Integer | 否 | 运营状态 |
| province | String | 否 | 省份 |
| city | String | 否 | 城市 |
| companyId | Long | 否 | 所属公司ID |

### P-02 项目全量列表（下拉用）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/projects/list` |
| **响应** | `R<List<BizProject>>` |

### P-03 项目详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/projects/{id}` |
| **路径参数** | id — Long，项目ID |
| **响应** | `R<ProjectVO>` |

### P-04 新增项目

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/projects` |
| **响应** | `R<Long>` — 新建项目ID |

**请求体 ProjectSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| projectCode | String | **是** | @NotBlank | 项目编号 |
| projectName | String | **是** | @NotBlank | 项目名称 |
| companyId | Long | **是** | @NotNull | 所属公司 |
| province | String | 否 | - | 省份 |
| city | String | 否 | - | 城市 |
| address | String | 否 | - | 地址 |
| propertyType | Integer | 否 | - | 产权性质 |
| businessType | Integer | 否 | - | 经营类型 |
| buildingArea | BigDecimal | 否 | @DecimalMin("0") | 建筑面积 |
| operatingArea | BigDecimal | 否 | @DecimalMin("0") | 经营面积 |
| operationStatus | Integer | 否 | - | 运营状态 |
| openingDate | LocalDate | 否 | - | 开业时间 |
| managerId | Long | 否 | - | 负责人ID |
| imageUrls | List\<ImageUrl\> | 否 | - | 项目图片 |

### P-05 修改项目

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/projects/{id}` |
| **路径参数** | id — Long，项目ID |
| **请求体** | 同 ProjectSaveDTO |
| **响应** | `R<Void>` |

### P-06 删除项目

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/projects/{id}` |
| **路径参数** | id — Long，项目ID |
| **响应** | `R<Void>` |

### P-07 获取合同甲方信息

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/projects/{id}/contracts` |
| **路径参数** | id — Long，项目ID |
| **响应** | `R<ProjectContractVO>` |

**ProjectContractVO：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目ID |
| partyAName | String | 甲方抬头 |
| partyAAbbr | String | 甲方缩写 |
| partyAAddress | String | 甲方地址 |
| partyAPhone | String | 甲方电话 |
| businessLicense | String | 营业执照号 |
| legalRepresentative | String | 法人代表 |
| email | String | 邮箱 |

### P-08 保存合同甲方信息

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/projects/{id}/contracts` |
| **路径参数** | id — Long，项目ID |
| **响应** | `R<Void>` |

**请求体 ProjectContractDTO：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| partyAName | String | 否 | 甲方抬头 |
| partyAAbbr | String | 否 | 甲方缩写 |
| partyAAddress | String | 否 | 甲方地址 |
| partyAPhone | String | 否 | 甲方电话 |
| businessLicense | String | 否 | 营业执照号 |
| legalRepresentative | String | 否 | 法人代表 |
| email | String | 否 | 邮箱 |

### P-09 获取财务联系人列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/projects/{id}/finance-contacts` |
| **路径参数** | id — Long，项目ID |
| **响应** | `R<List<ProjectFinanceContactVO>>` |

**ProjectFinanceContactVO：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目ID |
| contactName | String | 联系人姓名 |
| phone | String | 电话 |
| email | String | 邮箱 |
| creditCode | String | 社会信用代码 |
| sealType | String | 用章类型 |
| sealDesc | String | 用章说明 |

### P-10 新增财务联系人

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/projects/{id}/finance-contacts` |
| **路径参数** | id — Long，项目ID |
| **响应** | `R<Long>` — 新建联系人ID |

**请求体 ProjectFinanceContactDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contactName | String | **是** | @NotBlank | 联系人姓名 |
| phone | String | 否 | - | 电话 |
| email | String | 否 | - | 邮箱 |
| creditCode | String | 否 | - | 社会信用代码 |
| sealType | String | 否 | - | 用章类型 |
| sealDesc | String | 否 | - | 用章说明 |

### P-11 修改财务联系人

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/projects/{id}/finance-contacts/{cid}` |
| **路径参数** | id — 项目ID, cid — 联系人ID |
| **请求体** | 同 ProjectFinanceContactDTO |
| **响应** | `R<Void>` |

### P-12 删除财务联系人

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/projects/{id}/finance-contacts/{cid}` |
| **路径参数** | id — 项目ID, cid — 联系人ID |
| **响应** | `R<Void>` |

### P-13 获取银行账号列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/projects/{id}/banks` |
| **路径参数** | id — Long，项目ID |
| **响应** | `R<List<ProjectBankVO>>` |

**ProjectBankVO：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目ID |
| bankName | String | 银行名称 |
| bankAccount | String | 银行账号 |
| accountName | String | 户名 |
| isDefault | Integer | 是否默认（0否/1是） |

### P-14 新增银行账号

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/projects/{id}/banks` |
| **路径参数** | id — Long，项目ID |
| **响应** | `R<Long>` — 新建银行账号ID |

**请求体 ProjectBankDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| bankName | String | **是** | @NotBlank | 银行名称 |
| bankAccount | String | **是** | @NotBlank | 银行账号 |
| accountName | String | **是** | @NotBlank | 户名 |
| isDefault | Integer | 否 | - | 是否默认（0否/1是） |

### P-15 修改银行账号

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/projects/{id}/banks/{bid}` |
| **路径参数** | id — 项目ID, bid — 银行账号ID |
| **请求体** | 同 ProjectBankDTO |
| **响应** | `R<Void>` |

### P-16 删除银行账号

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/projects/{id}/banks/{bid}` |
| **路径参数** | id — 项目ID, bid — 银行账号ID |
| **响应** | `R<Void>` |

### P-17 添加项目图片

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/projects/{id}/images` |
| **路径参数** | id — Long，项目ID |
| **响应** | `R<Void>` |

**请求体 ProjectImageDTO：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| url | String | 否 | 图片URL |
| name | String | 否 | 图片名称 |

### P-18 删除项目图片

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/projects/{id}/images/{index}` |
| **路径参数** | id — 项目ID, index — 图片索引（Integer） |
| **响应** | `R<Void>` |

### ProjectVO 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectCode | String | 项目编号 |
| projectName | String | 项目名称 |
| companyId | Long | 所属公司ID |
| companyName | String | 公司名称 |
| province | String | 省份 |
| city | String | 城市 |
| address | String | 地址 |
| propertyType | Integer | 产权性质编码 |
| propertyTypeName | String | 产权性质名称 |
| businessType | Integer | 经营类型编码 |
| businessTypeName | String | 经营类型名称 |
| buildingArea | BigDecimal | 建筑面积 |
| operatingArea | BigDecimal | 经营面积 |
| operationStatus | Integer | 运营状态编码 |
| operationStatusName | String | 运营状态名称 |
| openingDate | LocalDate | 开业时间（yyyy-MM-dd） |
| managerId | Long | 负责人ID |
| managerName | String | 负责人姓名 |
| imageUrls | List\<ImageUrl\> | 图片列表 |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |
| updatedAt | LocalDateTime | 更新时间（yyyy-MM-dd HH:mm:ss） |

---

## 2. 楼栋管理 — BizBuildingController `/base/buildings`

### B-01 楼栋分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/buildings` |
| **响应** | `R<IPage<BuildingVO>>` |

**查询参数 BuildingQuery（含 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 所属项目 |
| buildingName | String | 否 | 楼栋名称（模糊匹配） |
| buildingCode | String | 否 | 楼栋编码 |
| status | Integer | 否 | 状态 |

### B-02 楼栋详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/buildings/{id}` |
| **路径参数** | id — Long，楼栋ID |
| **响应** | `R<BuildingVO>` |

### B-03 新增楼栋

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/buildings` |
| **响应** | `R<Long>` — 新建楼栋ID |

**请求体 BuildingSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| projectId | Long | **是** | @NotNull | 所属项目 |
| buildingCode | String | 否 | - | 楼栋编码 |
| buildingName | String | **是** | @NotBlank | 楼栋名称 |
| status | Integer | 否 | - | 状态 |
| buildingArea | BigDecimal | 否 | @DecimalMin("0") | 建筑面积 |
| operatingArea | BigDecimal | 否 | @DecimalMin("0") | 营业面积 |
| aboveFloors | Integer | 否 | - | 地上楼层数 |
| belowFloors | Integer | 否 | - | 地下楼层数 |
| imageUrl | String | 否 | - | 楼栋图URL |

### B-04 修改楼栋

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/buildings/{id}` |
| **路径参数** | id — Long，楼栋ID |
| **请求体** | 同 BuildingSaveDTO |
| **响应** | `R<Void>` |

### B-05 删除楼栋

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/buildings/{id}` |
| **路径参数** | id — Long，楼栋ID |
| **响应** | `R<Void>` |
| **业务约束** | 需校验是否有关联楼层/商铺，有则拒绝删除 |

### BuildingVO 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目ID |
| projectName | String | 项目名称 |
| buildingCode | String | 楼栋编码 |
| buildingName | String | 楼栋名称 |
| status | Integer | 状态编码 |
| statusName | String | 状态名称 |
| buildingArea | BigDecimal | 建筑面积 |
| operatingArea | BigDecimal | 营业面积 |
| aboveFloors | Integer | 地上楼层数 |
| belowFloors | Integer | 地下楼层数 |
| imageUrl | String | 楼栋图 |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |
| updatedAt | LocalDateTime | 更新时间（yyyy-MM-dd HH:mm:ss） |

---

## 3. 楼层管理 — BizFloorController `/base/floors`

### F-01 楼层分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/floors` |
| **响应** | `R<IPage<FloorVO>>` |

**查询参数 FloorQuery（含 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 所属项目 |
| buildingId | Long | 否 | 所属楼栋 |
| floorName | String | 否 | 楼层名称 |
| floorCode | String | 否 | 楼层编码 |
| status | Integer | 否 | 状态 |

### F-02 楼层详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/floors/{id}` |
| **路径参数** | id — Long，楼层ID |
| **响应** | `R<FloorVO>` |

### F-03 新增楼层

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/floors` |
| **响应** | `R<Long>` — 新建楼层ID |

**请求体 FloorSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| projectId | Long | **是** | @NotNull | 所属项目 |
| buildingId | Long | **是** | @NotNull | 所属楼栋 |
| floorCode | String | 否 | - | 楼层编码 |
| floorName | String | **是** | @NotBlank | 楼层名称 |
| status | Integer | 否 | - | 状态 |
| buildingArea | BigDecimal | 否 | @DecimalMin("0") | 建筑面积 |
| operatingArea | BigDecimal | 否 | @DecimalMin("0") | 营业面积 |
| remark | String | 否 | - | 备注 |
| imageUrl | String | 否 | - | 楼层图URL |

### F-04 修改楼层

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/floors/{id}` |
| **路径参数** | id — Long，楼层ID |
| **请求体** | 同 FloorSaveDTO |
| **响应** | `R<Void>` |

### F-05 删除楼层

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/floors/{id}` |
| **路径参数** | id — Long，楼层ID |
| **响应** | `R<Void>` |
| **业务约束** | 需校验是否有关联商铺，有则拒绝删除 |

### FloorVO 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目ID |
| buildingId | Long | 楼栋ID |
| buildingName | String | 楼栋名称 |
| floorCode | String | 楼层编码 |
| floorName | String | 楼层名称 |
| status | Integer | 状态编码 |
| statusName | String | 状态名称 |
| buildingArea | BigDecimal | 建筑面积 |
| operatingArea | BigDecimal | 营业面积 |
| remark | String | 备注 |
| imageUrl | String | 楼层图 |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |
| updatedAt | LocalDateTime | 更新时间（yyyy-MM-dd HH:mm:ss） |

---

## 4. 商铺管理 — BizShopController `/base/shops`

### S-01 商铺分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/shops` |
| **响应** | `R<IPage<ShopVO>>` |

**查询参数 ShopQuery（含 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 所属项目 |
| buildingId | Long | 否 | 所属楼栋 |
| floorId | Long | 否 | 所在楼层 |
| shopCode | String | 否 | 铺位号 |
| shopStatus | Integer | 否 | 状态（0空置/1在租/2自用/3预留） |
| shopType | Integer | 否 | 类型（1临街/2内铺/3专柜） |
| signedFormat | String | 否 | 签约业态 |

### S-02 商铺详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/shops/{id}` |
| **路径参数** | id — Long，商铺ID |
| **响应** | `R<ShopVO>` |

### S-03 新增商铺

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/shops` |
| **响应** | `R<Long>` — 新建商铺ID |

**请求体 ShopSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| projectId | Long | **是** | @NotNull | 所属项目 |
| buildingId | Long | **是** | @NotNull | 所属楼栋 |
| floorId | Long | **是** | @NotNull | 所在楼层 |
| shopCode | String | **是** | @NotBlank | 铺位号 |
| shopType | Integer | 否 | - | 商铺类型（1临街/2内铺/3专柜） |
| rentArea | BigDecimal | 否 | @DecimalMin("0") | 计租面积 |
| measuredArea | BigDecimal | 否 | @DecimalMin("0") | 实测面积 |
| buildingArea | BigDecimal | 否 | @DecimalMin("0") | 建筑面积 |
| operatingArea | BigDecimal | 否 | @DecimalMin("0") | 经营面积 |
| shopStatus | Integer | 否 | - | 商铺状态（0空置/1在租/2自用/3预留） |
| countLeasingRate | Integer | 否 | - | 计入招商率（0否/1是） |
| countRentalRate | Integer | 否 | - | 计入出租率（0否/1是） |
| countOpeningRate | Integer | 否 | - | 计入开业率（0否/1是） |
| signedFormat | String | 否 | - | 签约业态 |
| plannedFormat | String | 否 | - | 规划业态 |
| ownerName | String | 否 | - | 业主名称 |
| ownerContact | String | 否 | - | 业主联系人 |
| ownerPhone | String | 否 | - | 业主电话 |

### S-04 修改商铺

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/shops/{id}` |
| **路径参数** | id — Long，商铺ID |
| **请求体** | 同 ShopSaveDTO |
| **响应** | `R<Void>` |

### S-05 删除商铺

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/shops/{id}` |
| **路径参数** | id — Long，商铺ID |
| **响应** | `R<Void>` |
| **业务约束** | 需校验是否有关联合同/商机，有则拒绝删除 |

### S-06 商铺拆分

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/shops/split` |
| **响应** | `R<Void>` |

**请求体 ShopSplitDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| sourceShopId | Long | **是** | @NotNull | 原商铺ID |
| remark | String | 否 | - | 备注 |
| subShops | List\<SubShopDTO\> | **是** | @Valid @NotEmpty @Size(min=2) | 拆分后子商铺列表 |

**SubShopDTO（内部类）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| shopCode | String | **是** | @NotBlank | 铺位号 |
| shopType | Integer | 否 | - | 商铺类型 |
| rentArea | BigDecimal | **是** | @NotNull | 计租面积 |
| measuredArea | BigDecimal | 否 | - | 实测面积 |
| buildingArea | BigDecimal | 否 | - | 建筑面积 |
| operatingArea | BigDecimal | 否 | - | 经营面积 |
| plannedFormat | String | 否 | - | 规划业态 |

> **业务规则：** 拆分后子商铺面积之和必须等于原商铺面积；原商铺逻辑删除，新商铺 `parent_shop_id` 指向原商铺，`split_merge_type=1`。

### S-07 商铺合并

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/shops/merge` |
| **响应** | `R<Void>` |

**请求体 ShopMergeDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| sourceShopIds | List\<Long\> | **是** | @NotEmpty @Size(min=2) | 原商铺ID列表 |
| remark | String | 否 | - | 备注 |
| newShop | MergedShopDTO | **是** | @Valid @NotNull | 合并后新商铺 |

**MergedShopDTO（内部类）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| shopCode | String | **是** | @NotBlank | 铺位号 |
| shopType | Integer | 否 | - | 商铺类型 |
| rentArea | BigDecimal | **是** | @NotNull | 计租面积 |
| measuredArea | BigDecimal | 否 | - | 实测面积 |
| buildingArea | BigDecimal | 否 | - | 建筑面积 |
| operatingArea | BigDecimal | 否 | - | 经营面积 |
| plannedFormat | String | 否 | - | 规划业态 |

> **业务规则：** 原商铺逻辑删除，新商铺 `split_merge_type=2`；所有原商铺必须属于同一楼层。

### S-08 Excel 批量导入商铺

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/shops/import` |
| **Content-Type** | `multipart/form-data` |
| **响应** | `R<Map<String, Object>>` |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | **是** | Excel 文件 |

**响应 data 结构：**

| 字段 | 类型 | 含义 |
|------|------|------|
| success | int | 成功导入条数 |
| fail | int | 失败条数 |
| errors | List\<String\> | 错误明细列表 |

**Excel 模板列（ShopImportRow）：**

| 列名 | 类型 | 说明 |
|------|------|------|
| 项目编码 | String | projectCode |
| 楼栋编码 | String | buildingCode |
| 楼层编码 | String | floorCode |
| 铺位号 | String | shopCode |
| 商铺类型(1临街/2内铺/3专柜) | Integer | shopType |
| 计租面积(m2) | BigDecimal | rentArea |
| 实测面积(m2) | BigDecimal | measuredArea |
| 建筑面积(m2) | BigDecimal | buildingArea |
| 经营面积(m2) | BigDecimal | operatingArea |
| 商铺状态(0空置/1在租/2自用/3预留) | Integer | shopStatus |
| 规划业态 | String | plannedFormat |
| 签约业态 | String | signedFormat |
| 业主名称 | String | ownerName |
| 业主联系人 | String | ownerContact |
| 业主电话 | String | ownerPhone |

### S-09 下载导入模板

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/shops/template` |
| **响应** | Excel 文件流（application/vnd.openxmlformats-officedocument.spreadsheetml.sheet） |

### ShopVO 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目ID |
| projectName | String | 项目名称 |
| buildingId | Long | 楼栋ID |
| buildingName | String | 楼栋名称 |
| floorId | Long | 楼层ID |
| floorName | String | 楼层名称 |
| shopCode | String | 铺位号 |
| shopType | Integer | 商铺类型编码 |
| shopTypeName | String | 商铺类型名称 |
| rentArea | BigDecimal | 计租面积 |
| measuredArea | BigDecimal | 实测面积 |
| buildingArea | BigDecimal | 建筑面积 |
| operatingArea | BigDecimal | 经营面积 |
| shopStatus | Integer | 商铺状态编码 |
| shopStatusName | String | 商铺状态名称 |
| countLeasingRate | Integer | 计入招商率 |
| countRentalRate | Integer | 计入出租率 |
| countOpeningRate | Integer | 计入开业率 |
| signedFormat | String | 签约业态 |
| plannedFormat | String | 规划业态 |
| ownerName | String | 业主名称 |
| ownerContact | String | 业主联系人 |
| ownerPhone | String | 业主电话 |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |
| updatedAt | LocalDateTime | 更新时间（yyyy-MM-dd HH:mm:ss） |

---

## 5. 品牌管理 — BizBrandController `/base/brands`

### BR-01 品牌分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/brands` |
| **响应** | `R<IPage<BrandVO>>` |

**查询参数 BrandQuery（含 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| brandNameCn | String | 否 | 品牌中文名（模糊匹配） |
| formatType | String | 否 | 业态 |
| brandLevel | Integer | 否 | 品牌等级（1高端/2中端/3大众） |
| cooperationType | Integer | 否 | 合作关系（1直营/2加盟/3代理） |
| businessNature | Integer | 否 | 经营性质（1餐饮/2零售/3娱乐/4服务） |
| brandType | Integer | 否 | 品牌类型 |

### BR-02 品牌详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/brands/{id}` |
| **路径参数** | id — Long，品牌ID |
| **响应** | `R<BrandVO>` |

### BR-03 新增品牌

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/brands` |
| **响应** | `R<Long>` — 新建品牌ID |

**请求体 BrandSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| brandCode | String | 否 | - | 品牌编码 |
| brandNameCn | String | **是** | @NotBlank | 中文名 |
| brandNameEn | String | 否 | - | 英文名 |
| formatType | String | 否 | - | 业态 |
| brandLevel | Integer | 否 | - | 品牌等级 |
| cooperationType | Integer | 否 | - | 合作关系 |
| businessNature | Integer | 否 | - | 经营性质 |
| chainType | Integer | 否 | - | 连锁/单店 |
| projectStage | String | 否 | - | 项目阶段 |
| groupName | String | 否 | - | 集团名称 |
| hqAddress | String | 否 | - | 总部地址 |
| mainCities | String | 否 | - | 主要分布城市 |
| website | String | 否 | - | 网址 |
| phone | String | 否 | - | 联系电话 |
| brandType | Integer | 否 | - | 品牌类型 |
| avgRent | BigDecimal | 否 | - | 平均租金 |
| minCustomerPrice | BigDecimal | 否 | - | 最低客单价 |
| brandIntro | String | 否 | - | 品牌简介 |
| contacts | List\<BrandContactDTO\> | 否 | - | 联系人列表（随品牌一同保存） |

### BR-04 修改品牌

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/brands/{id}` |
| **路径参数** | id — Long，品牌ID |
| **请求体** | 同 BrandSaveDTO |
| **响应** | `R<Void>` |

### BR-05 删除品牌

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/brands/{id}` |
| **路径参数** | id — Long，品牌ID |
| **响应** | `R<Void>` |
| **业务约束** | 需校验是否有关联商家/合同，有则拒绝删除 |

### BR-06 获取品牌联系人列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/brands/{id}/contacts` |
| **路径参数** | id — Long，品牌ID |
| **响应** | `R<List<BrandContactVO>>` |

**BrandContactVO：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| brandId | Long | 品牌ID |
| contactName | String | 联系人姓名 |
| phone | String | 电话 |
| email | String | 邮箱 |
| position | String | 职位 |
| isPrimary | Integer | 是否主要联系人（0否/1是） |
| isPrimaryDesc | String | 主要联系人描述 |

### BR-07 新增品牌联系人

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/brands/{id}/contacts` |
| **路径参数** | id — Long，品牌ID |
| **响应** | `R<Long>` — 新建联系人ID |

**请求体 BrandContactDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contactName | String | **是** | @NotBlank | 联系人姓名 |
| phone | String | 否 | - | 电话 |
| email | String | 否 | - | 邮箱 |
| position | String | 否 | - | 职位 |
| isPrimary | Integer | 否 | - | 是否主要联系人（0/1） |

### BR-08 修改品牌联系人

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/brands/{id}/contacts/{cid}` |
| **路径参数** | id — 品牌ID, cid — 联系人ID |
| **请求体** | 同 BrandContactDTO |
| **响应** | `R<Void>` |

### BR-09 删除品牌联系人

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/brands/{id}/contacts/{cid}` |
| **路径参数** | id — 品牌ID, cid — 联系人ID |
| **响应** | `R<Void>` |

### BR-10 Excel 批量导入品牌

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/brands/import` |
| **Content-Type** | `multipart/form-data` |
| **响应** | `R<Map<String, Object>>` — {success, fail, errors} |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | **是** | Excel 文件 |

**Excel 模板列（BrandImportRow）：**

| 列名 | 类型 | 说明 |
|------|------|------|
| 品牌编码 | String | brandCode |
| 品牌名称(中) | String | brandNameCn |
| 品牌名称(英) | String | brandNameEn |
| 所属业态 | String | formatType |
| 品牌等级(1高端/2中端/3大众) | Integer | brandLevel |
| 合作关系(1直营/2加盟/3代理) | Integer | cooperationType |
| 经营性质(1餐饮/2零售/3娱乐/4服务) | Integer | businessNature |
| 联系电话 | String | phone |
| 品牌简介 | String | brandIntro |

### BR-11 下载导入模板

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/brands/template` |
| **响应** | Excel 文件流 |

### BrandVO 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| brandCode | String | 品牌编码 |
| brandNameCn | String | 中文名 |
| brandNameEn | String | 英文名 |
| formatType | String | 业态 |
| brandLevel | Integer | 品牌等级编码 |
| brandLevelName | String | 品牌等级名称 |
| cooperationType | Integer | 合作关系编码 |
| cooperationTypeName | String | 合作关系名称 |
| businessNature | Integer | 经营性质编码 |
| businessNatureName | String | 经营性质名称 |
| chainType | Integer | 连锁类型编码 |
| chainTypeName | String | 连锁类型名称 |
| projectStage | String | 项目阶段 |
| groupName | String | 集团名称 |
| hqAddress | String | 总部地址 |
| mainCities | String | 主要分布城市 |
| website | String | 网址 |
| phone | String | 联系电话 |
| brandType | Integer | 品牌类型编码 |
| brandTypeName | String | 品牌类型名称 |
| avgRent | BigDecimal | 平均租金 |
| minCustomerPrice | BigDecimal | 最低客单价 |
| brandIntro | String | 品牌简介 |
| contacts | List\<BrandContactVO\> | 联系人列表 |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |
| updatedAt | LocalDateTime | 更新时间（yyyy-MM-dd HH:mm:ss） |

---

## 6. 商家管理 — BizMerchantController `/base/merchants`

### M-01 商家分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/merchants` |
| **响应** | `R<IPage<MerchantVO>>` |

**查询参数 MerchantQuery（含 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 所属项目 |
| merchantName | String | 否 | 商家名称（模糊匹配） |
| merchantAttr | Integer | 否 | 商家属性（1个体户/2企业） |
| merchantNature | Integer | 否 | 商家性质（1民营/2国营/3外资/4合资） |
| formatType | String | 否 | 经营业态 |
| merchantLevel | Integer | 否 | 评级（1优秀/2良好/3一般/4差） |
| auditStatus | Integer | 否 | 审核状态 |

### M-02 商家详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/merchants/{id}` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<MerchantVO>` |

### M-03 新增商家

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/merchants` |
| **响应** | `R<Long>` — 新建商家ID |

**请求体 MerchantSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| projectId | Long | **是** | @NotNull | 所属项目 |
| merchantCode | String | 否 | - | 商家编号 |
| merchantName | String | **是** | @NotBlank | 商家名称 |
| merchantAttr | Integer | 否 | - | 商家属性（1个体户/2企业） |
| merchantNature | Integer | 否 | - | 商家性质 |
| formatType | String | 否 | - | 经营业态 |
| naturalPerson | String | 否 | - | 自然人 |
| idCard | String | 否 | - | 身份证号（SM4 加密存储） |
| address | String | 否 | - | 地址 |
| phone | String | 否 | - | 手机 |
| merchantLevel | Integer | 否 | - | 评级 |
| auditStatus | Integer | 否 | - | 审核状态 |
| contacts | List\<MerchantContactDTO\> | 否 | - | 联系人列表 |
| invoices | List\<MerchantInvoiceDTO\> | 否 | - | 开票信息列表 |

### M-04 修改商家

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/merchants/{id}` |
| **路径参数** | id — Long，商家ID |
| **请求体** | 同 MerchantSaveDTO |
| **响应** | `R<Void>` |

### M-05 删除商家

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/merchants/{id}` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<Void>` |

### M-06 商家审核

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/merchants/{id}/audit` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<Void>` |

**请求体 `Map<String, Object>`：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| auditStatus | Integer | 是 | 审核状态 |
| auditRemark | String | 否 | 审核意见 |

### M-07 获取商家联系人列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/merchants/{id}/contacts` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<List<MerchantContactVO>>` |

**MerchantContactVO：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| merchantId | Long | 商家ID |
| contactName | String | 联系人姓名 |
| phone | String | 电话 |
| email | String | 邮箱 |
| position | String | 职位 |
| isPrimary | Integer | 是否主要联系人（0否/1是） |
| isPrimaryDesc | String | 主要联系人描述 |

### M-08 新增商家联系人

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/merchants/{id}/contacts` |
| **响应** | `R<Long>` |

**请求体 MerchantContactDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contactName | String | **是** | @NotBlank | 联系人姓名 |
| phone | String | 否 | - | 电话 |
| email | String | 否 | - | 邮箱 |
| position | String | 否 | - | 职位 |
| isPrimary | Integer | 否 | - | 是否主要联系人（0/1） |

### M-09 修改商家联系人

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/merchants/{id}/contacts/{cid}` |
| **路径参数** | id — 商家ID, cid — 联系人ID |
| **请求体** | 同 MerchantContactDTO |
| **响应** | `R<Void>` |

### M-10 删除商家联系人

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/merchants/{id}/contacts/{cid}` |
| **路径参数** | id — 商家ID, cid — 联系人ID |
| **响应** | `R<Void>` |

### M-11 获取商家诚信记录列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/merchants/{id}/credits` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<List<CreditVO>>` |

**CreditVO：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| merchantId | Long | 商家ID |
| recordType | Integer | 记录类型编码 |
| recordTypeName | String | 记录类型名称 |
| content | String | 内容描述 |
| recordDate | LocalDate | 记录日期（yyyy-MM-dd） |
| operatorId | Long | 操作人ID |
| attachmentUrl | String | 附件URL |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |

### M-12 新增诚信记录

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/merchants/{id}/credits` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<Long>` |

**请求体 CreditSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| recordType | Integer | **是** | @NotNull | 记录类型 |
| content | String | 否 | - | 内容描述 |
| recordDate | LocalDate | 否 | - | 记录日期 |
| attachmentUrl | String | 否 | - | 附件URL |

### M-13 删除诚信记录

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/merchants/{id}/credits/{rid}` |
| **路径参数** | id — 商家ID, rid — 记录ID |
| **响应** | `R<Void>` |

### M-14 获取商家开票信息列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/merchants/{id}/invoices` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<List<MerchantInvoiceVO>>` |

**MerchantInvoiceVO：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| merchantId | Long | 商家ID |
| invoiceTitle | String | 发票抬头 |
| taxNumber | String | 税号 |
| bankName | String | 银行名称 |
| bankAccount | String | 银行账号 |
| address | String | 地址 |
| phone | String | 电话 |
| isDefault | Integer | 是否默认（0否/1是） |

### M-15 新增开票信息

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/merchants/{id}/invoices` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<Long>` |

**请求体 InvoiceSaveDTO：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| invoiceTitle | String | 否 | 发票抬头 |
| taxNumber | String | 否 | 税号 |
| bankName | String | 否 | 银行名称 |
| bankAccount | String | 否 | 银行账号 |
| address | String | 否 | 地址 |
| phone | String | 否 | 电话 |
| isDefault | Integer | 否 | 是否默认 |

### M-16 修改开票信息

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/merchants/{id}/invoices/{iid}` |
| **路径参数** | id — 商家ID, iid — 开票信息ID |
| **请求体** | 同 InvoiceSaveDTO |
| **响应** | `R<Void>` |

### M-17 删除开票信息

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/merchants/{id}/invoices/{iid}` |
| **路径参数** | id — 商家ID, iid — 开票信息ID |
| **响应** | `R<Void>` |

### M-18 获取商家附件列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/merchants/{id}/attachments` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<List<AttachmentVO>>` |

**AttachmentVO：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| merchantId | Long | 商家ID |
| fileName | String | 文件名 |
| fileUrl | String | 文件URL |
| fileType | String | 文件类型 |
| fileSize | Long | 文件大小（bytes） |
| uploadBy | Long | 上传人ID |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |

### M-19 新增附件

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/merchants/{id}/attachments` |
| **路径参数** | id — Long，商家ID |
| **响应** | `R<Long>` |

**请求体 AttachmentSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| fileName | String | **是** | @NotBlank | 文件名 |
| fileUrl | String | **是** | @NotBlank | 文件URL |
| fileType | String | 否 | - | 文件类型 |
| fileSize | Long | 否 | - | 文件大小（bytes） |

### M-20 删除附件

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/merchants/{id}/attachments/{aid}` |
| **路径参数** | id — 商家ID, aid — 附件ID |
| **响应** | `R<Void>` |

### M-21 Excel 批量导入商家

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/merchants/import` |
| **Content-Type** | `multipart/form-data` |
| **响应** | `R<Map<String, Object>>` — {success, fail, errors} |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | **是** | Excel 文件 |
| projectId | Long | **是** | 所属项目ID（@RequestParam） |

**Excel 模板列（MerchantImportRow）：**

| 列名 | 类型 | 说明 |
|------|------|------|
| 商家编号 | String | merchantCode |
| 商家名称 | String | merchantName |
| 商家属性(1个体户/2企业) | Integer | merchantAttr |
| 商家性质(1民营/2国营/3外资/4合资) | Integer | merchantNature |
| 经营业态 | String | formatType |
| 自然人姓名 | String | naturalPerson |
| 地址 | String | address |
| 手机 | String | phone |
| 商家评级(1优秀/2良好/3一般/4差) | Integer | merchantLevel |

### M-22 下载导入模板

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/merchants/template` |
| **响应** | Excel 文件流 |

### MerchantVO 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目ID |
| projectName | String | 项目名称 |
| merchantCode | String | 商家编号 |
| merchantName | String | 商家名称 |
| merchantAttr | Integer | 商家属性编码 |
| merchantAttrName | String | 商家属性名称 |
| merchantNature | Integer | 商家性质编码 |
| merchantNatureName | String | 商家性质名称 |
| formatType | String | 经营业态 |
| naturalPerson | String | 自然人 |
| idCard | String | 身份证号（脱敏显示） |
| address | String | 地址 |
| phone | String | 手机 |
| merchantLevel | Integer | 评级编码 |
| merchantLevelName | String | 评级名称 |
| auditStatus | Integer | 审核状态编码 |
| auditStatusName | String | 审核状态名称 |
| contacts | List\<MerchantContactVO\> | 联系人列表 |
| invoices | List\<MerchantInvoiceVO\> | 开票信息列表 |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |
| updatedAt | LocalDateTime | 更新时间（yyyy-MM-dd HH:mm:ss） |

---

## 7. 通知公告 — BizNoticeController `/base/notices`

### N-01 公告分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/notices` |
| **响应** | `R<IPage<NoticeVO>>` |

**查询参数 NoticeQuery（含 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 否 | 标题（模糊匹配） |
| noticeType | Integer | 否 | 类型（通知/公告/政策） |
| status | Integer | 否 | 状态（草稿/已发布/下架） |

### N-02 公告详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/notices/{id}` |
| **路径参数** | id — Long，公告ID |
| **响应** | `R<NoticeVO>` |

### N-03 新增公告

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/notices` |
| **响应** | `R<Long>` — 新建公告ID |

**请求体 NoticeSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| title | String | **是** | @NotBlank | 标题 |
| content | String | 否 | - | 富文本内容（HTML） |
| noticeType | Integer | 否 | - | 类型 |
| status | Integer | 否 | - | 状态 |
| scheduledTime | LocalDateTime | 否 | - | 定时发送时间（yyyy-MM-dd HH:mm:ss） |

### N-04 修改公告

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/notices/{id}` |
| **路径参数** | id — Long，公告ID |
| **请求体** | 同 NoticeSaveDTO |
| **响应** | `R<Void>` |

### N-05 删除公告

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/notices/{id}` |
| **路径参数** | id — Long，公告ID |
| **响应** | `R<Void>` |

### N-06 发布公告

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/notices/{id}/publish` |
| **路径参数** | id — Long，公告ID |
| **响应** | `R<Void>` |

### N-07 下架公告

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/notices/{id}/unpublish` |
| **路径参数** | id — Long，公告ID |
| **响应** | `R<Void>` |

### N-08 标记已读

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/notices/{id}/read` |
| **路径参数** | id — Long，公告ID |
| **响应** | `R<Void>` |

### N-09 阅读统计

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/notices/{id}/read-stats` |
| **路径参数** | id — Long，公告ID |
| **响应** | `R<NoticeReadStatsVO>` |

**NoticeReadStatsVO：**

| 字段 | 类型 | 含义 |
|------|------|------|
| noticeId | Long | 公告ID |
| readCount | long | 已读人数 |
| currentUserRead | boolean | 当前用户是否已读 |

### NoticeVO 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| title | String | 标题 |
| content | String | 富文本内容 |
| noticeType | Integer | 类型编码 |
| noticeTypeName | String | 类型名称 |
| status | Integer | 状态编码 |
| statusName | String | 状态名称 |
| scheduledTime | LocalDateTime | 定时发送时间（yyyy-MM-dd HH:mm:ss） |
| publishTime | LocalDateTime | 实际发布时间（yyyy-MM-dd HH:mm:ss） |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |
| updatedAt | LocalDateTime | 更新时间（yyyy-MM-dd HH:mm:ss） |

---

## 8. 新闻资讯 — BizNewsController `/base/news`

### NW-01 资讯分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/news` |
| **响应** | `R<IPage<NewsVO>>` |

**查询参数 NewsQuery（含 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 否 | 标题（模糊匹配） |
| category | Integer | 否 | 分类（新闻/政策/招商/服务指南） |
| status | Integer | 否 | 状态（草稿/上架/下架） |

### NW-02 资讯详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/news/{id}` |
| **路径参数** | id — Long，资讯ID |
| **响应** | `R<NewsVO>` |

### NW-03 新增资讯

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/base/news` |
| **响应** | `R<Long>` — 新建资讯ID |

**请求体 NewsSaveDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| title | String | **是** | @NotBlank | 标题 |
| content | String | 否 | - | 富文本内容（HTML） |
| category | Integer | 否 | - | 分类 |
| status | Integer | 否 | - | 状态 |
| publishTime | LocalDateTime | 否 | - | 发布时间（yyyy-MM-dd HH:mm:ss） |

### NW-04 修改资讯

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/news/{id}` |
| **路径参数** | id — Long，资讯ID |
| **请求体** | 同 NewsSaveDTO |
| **响应** | `R<Void>` |

### NW-05 删除资讯

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/base/news/{id}` |
| **路径参数** | id — Long，资讯ID |
| **响应** | `R<Void>` |

### NW-06 发布上架

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/news/{id}/publish` |
| **路径参数** | id — Long，资讯ID |
| **响应** | `R<Void>` |

### NW-07 下架

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/base/news/{id}/unpublish` |
| **路径参数** | id — Long，资讯ID |
| **响应** | `R<Void>` |

### NewsVO 完整字段

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| title | String | 标题 |
| content | String | 富文本内容 |
| category | Integer | 分类编码 |
| categoryName | String | 分类名称 |
| status | Integer | 状态编码 |
| statusName | String | 状态名称 |
| publishTime | LocalDateTime | 发布时间（yyyy-MM-dd HH:mm:ss） |
| createdAt | LocalDateTime | 创建时间（yyyy-MM-dd HH:mm:ss） |
| updatedAt | LocalDateTime | 更新时间（yyyy-MM-dd HH:mm:ss） |

---

## 9. 公司管理 — SysCompanyController `/base/companies`

### C-01 公司全量列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/companies/list` |
| **响应** | `R<List<SysCompany>>` — 直接返回实体列表 |

> 无专用 DTO/VO，返回 SysCompany 实体，用于项目管理等下拉选择。

---

## 10. 用户管理 — SysUserController `/base/users`

### U-01 用户列表（简要）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/base/users/list` |
| **响应** | `R<List<SysUser>>` — 仅含 id / username / realName |

> 用于项目负责人等下拉选择，不返回密码等敏感字段。

---

## 基础数据管理 — 汇总统计

> **数据来源：** `asset-base` 模块源码（端口 8001） · 生成日期 2026-03-10

| 维度 | 数量 |
|------|------|
| 控制器 | 11 |
| API 端点总数 | **76** |
| 查询 DTO | 8 |
| 保存/操作 DTO | 23（含内部类） |
| 响应 VO | 17 |
| Excel 导入行模型 | 3 |

**各控制器端点分布：**

| 控制器 | 端点数 |
|--------|--------|
| AuthController | 4 |
| BizProjectController | 18 |
| BizBuildingController | 5 |
| BizFloorController | 5 |
| BizShopController | 9 |
| BizBrandController | 11 |
| BizMerchantController | 22 |
| BizNoticeController | 9 |
| BizNewsController | 7 |
| SysCompanyController | 1 |
| SysUserController | 1 |

---

## 基础数据管理 — 代码 vs 技术分析报告差异

| 差异点 | 技术报告设计 | 代码实际实现 |
|--------|------------|------------|
| **路径前缀** | `/api/v1/base-data/` | `/base/` |
| **商铺面积独立 API** | `PUT /shops/{id}/area` 独立权限 | 未拆分，统一在 `PUT /shops/{id}` |
| **楼栋图上传** | `POST /buildings/{id}/image` | 合并到 BuildingSaveDTO.imageUrl |
| **项目删除** | 报告未列出 | 代码实现 `DELETE /projects/{id}` |
| **公告已读/统计** | 报告未列出 | 代码实现 `POST /notices/{id}/read` + `GET /read-stats` |
| **资讯上下架** | `PUT /news/{id}/status` | 拆为 `PUT /publish` + `PUT /unpublish` |
| **商家子资源 CRUD** | 报告仅简略提及 | 代码完整实现联系人/诚信/开票/附件共 13 个端点 |
| **品牌联系人修改/删除** | 报告未列出 | 代码实现 `PUT/DELETE /brands/{id}/contacts/{cid}` |
| **删除接口** | 报告中财务联系人/银行无删除 | 代码均实现 DELETE |
| **公司/用户辅助接口** | 报告未涉及 | 代码提供 `/base/companies/list` + `/base/users/list` |

---

---

# 二、招商管理模块（asset-investment，端口 8002）

> **数据来源：** `asset-investment` 模块源码 + `docs/招商管理模块_技术分析报告_修订版.docx` + `docs/招商管理模块-数据库设计_修订版.md`
> **生成日期：** 2026-03-19

> **路径说明：** 控制器注解路径为 `/inv/*`。前端通过 Vite 代理 `/api/inv/*` → `localhost:8002` 并 rewrite 去掉 `/api`。技术分析报告中设计的 `/api/v1/investment/*` 前缀未在代码中使用，本文档以代码实际实现为准。

---

## 招商管理 — 状态枚举字典

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

## 招商管理 — 业务类型字典

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

## 11. 计租方案配置 — CfgRentSchemeController `/inv/config/rent-schemes`

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

## 12. 收款项目配置 — CfgFeeItemController `/inv/config/fee-items`

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

## 13. 意向协议管理 — InvIntentionController `/inv/intentions`

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

## 14. 招商合同管理 — InvLeaseContractController `/inv/contracts`

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

## 15. 开业审批管理 — InvOpeningApprovalController `/inv/opening-approvals`

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

## 16. 租决政策管理 — InvRentPolicyController `/inv/rent-policies`

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

## 17. 租金分解管理 — InvRentDecompositionController `/inv/rent-decomps`

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

## 招商管理 — 汇总统计

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

## 招商管理 — 代码 vs 技术分析报告差异

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

---

# 三、营运管理（asset-operation，端口 8003）

> **模块职责：** 合同执行期的全生命周期管理 — 台账 → 变更 → 营收填报 → 客流分析 → 解约清算 → 到期预警
> **控制器路径前缀：** `/opr/*`
> **数据库表前缀：** `opr_`（14 张表）
> **数据来源：** `asset-operation` 模块源码 · 生成日期 2026-03-19

---

## 营运管理 — 状态枚举字典

### 台账状态 LedgerStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | ONGOING | 进行中 |
| 1 | COMPLETED | 已完成 |
| 2 | TERMINATED | 已解约 |

### 审核状态（台账）

| 值 | 含义 |
|----|------|
| 0 | 待审核 |
| 1 | 通过 |
| 2 | 驳回 |

### 双签状态

| 值 | 含义 |
|----|------|
| 0 | 未完成 |
| 1 | 已完成 |

### 应收生成状态

| 值 | 含义 |
|----|------|
| 0 | 未生成 |
| 1 | 已生成 |
| 2 | 已推送 |

### 应收计划状态 ReceivablePlanStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | PENDING | 待收 |
| 1 | PARTIAL | 部分收款 |
| 2 | COLLECTED | 已收 |
| 3 | VOIDED | 已作废 |

### 合同变更状态 ChangeStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | DRAFT | 草稿 |
| 1 | APPROVING | 审批中 |
| 2 | APPROVED | 通过 |
| 3 | REJECTED | 驳回 |

### 解约类型 TerminationType

| 值 | 枚举 | 含义 |
|----|------|------|
| 1 | NATURAL | 到期解约 |
| 2 | EARLY | 提前解约 |
| 3 | RENEWAL | 重签解约 |

### 解约状态 TerminationStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | DRAFT | 草稿 |
| 1 | APPROVING | 审批中 |
| 2 | EFFECTIVE | 已生效 |
| 3 | REJECTED | 驳回 |

### 营收填报状态

| 值 | 含义 |
|----|------|
| 0 | 待确认 |
| 1 | 已确认 |

### 预警类型 AlertType

| 值 | 枚举 | 含义 |
|----|------|------|
| 1 | CONTRACT_EXPIRY | 合同到期预警 |
| 2 | RECEIVABLE_OVERDUE | 应收到期预警 |

### 预警发送渠道 AlertChannel

| 值 | 枚举 | 含义 |
|----|------|------|
| 1 | SITE | 站内信 |
| 2 | EMAIL | 邮件 |
| 3 | SMS | 短信 |

### 预警发送状态

| 值 | 含义 |
|----|------|
| 0 | 待发送 |
| 1 | 已发送 |
| 2 | 发送失败 |
| 3 | 已取消 |

---

## 营运管理 — 业务类型字典

### 合同类型

| 值 | 含义 |
|----|------|
| 1 | 租赁 |
| 2 | 联营 |
| 3 | 临时 |

### 变更类型编码 ChangeTypeCode

| 编码 | 含义 |
|------|------|
| RENT | 租金变更 |
| BRAND | 品牌变更 |
| TENANT | 商家主体变更 |
| FEE | 费项变更 |
| CLAUSE | 条款变更 |
| TERM | 租期变更 |
| AREA | 面积变更 |
| COMPANY | 公司主体变更 |

### 应收计划来源类型 sourceType

| 值 | 含义 |
|----|------|
| 1 | 合同生成 |
| 2 | 变更生成 |
| 3 | 浮动租金 |
| 4 | 一次性录入 |

### 一次性首款录入类型 entryType

| 值 | 含义 |
|----|------|
| 1 | 单笔 |
| 2 | 多笔 |
| 3 | 历史账期 |

### 客流数据来源 sourceType

| 值 | 含义 |
|----|------|
| 1 | 手动录入 |
| 2 | Excel 导入 |
| 3 | 设备对接 |

### 清算明细类型 itemType

| 值 | 含义 |
|----|------|
| 1 | 未收租金 |
| 2 | 违约金 |
| 3 | 保证金退还 |
| 4 | 其他费用 |

---

## 18. 合同台账管理 — OprContractLedgerController `/opr/ledgers`

> **状态流转：** 进行中(0) → 已完成(1) → 已解约(2)
> **审核流转：** 待审核(0) → 通过(1) / 驳回(2)
> **双签流转：** 未完成(0) → 已完成(1)
> **应收状态：** 未生成(0) → 已生成(1) → 已推送(2)

### LG-01 台账分页列表查询

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/ledgers` |
| **响应** | `R<IPage<OprContractLedger>>` |

**查询参数 LedgerQueryDTO：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20 |
| projectId | Long | 否 | 项目 ID |
| contractType | Integer | 否 | 合同类型（1 租赁/2 联营/3 临时） |
| status | Integer | 否 | 台账状态（0 进行中/1 已完成/2 已解约） |
| auditStatus | Integer | 否 | 审核状态（0 待审核/1 通过/2 驳回） |
| doubleSignStatus | Integer | 否 | 双签状态（0 未完成/1 已完成） |
| ledgerCode | String | 否 | 台账编号（模糊查询） |
| contractEndFrom | String | 否 | 合同到期日期起（yyyy-MM-dd） |
| contractEndTo | String | 否 | 合同到期日期止（yyyy-MM-dd） |

### LG-02 台账选择器模糊搜索

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/ledgers/search` |
| **响应** | `R<List<LedgerSelectorVO>>` — 简化选择器数据 |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键字（台账编号/合同编号/商家名） |
| pageSize | int | 否 | 返回条数，默认 10，最大 50 |

**响应字段 LedgerSelectorVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 台账 ID |
| ledgerCode | String | 台账编号 |
| contractCode | String | 关联合同编号 |
| merchantName | String | 商家名称 |
| shopCode | String | 商铺编号（第一个商铺） |
| contractStart | LocalDate | 合同开始日期 |
| contractEnd | LocalDate | 合同到期日期 |
| status | Integer | 台账状态 |

### LG-03 台账详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/ledgers/{id}` |
| **响应** | `R<LedgerDetailVO>` — 含应收计划/关联信息 |

**响应字段 LedgerDetailVO（继承 OprContractLedger）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| *（OprContractLedger 全部字段）* | — | 见实体字段表 |
| projectName | String | 项目名称 |
| merchantName | String | 商家名称 |
| brandName | String | 品牌名称 |
| contractCode | String | 合同编号 |
| contractName | String | 合同名称 |
| shopCode | String | 商铺编号（第一个商铺） |
| shopId | Long | 商铺 ID |
| receivablePlans | List\<OprReceivablePlan\> | 应收计划列表 |
| contractTypeName | String | 合同类型名称 |
| doubleSignStatusName | String | 双签状态名称 |
| receivableStatusName | String | 应收状态名称 |
| auditStatusName | String | 审核状态名称 |
| statusName | String | 台账状态名称 |

### LG-04 根据招商合同创建台账

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/ledgers/from-contract/{contractId}` |
| **响应** | `R<Long>` — 新建台账 ID |

> **业务规则：** 从招商合同复制主要信息（项目/商家/品牌/合同类型/起止日期），自动生成台账编号，初始状态为进行中(0)。生产环境建议通过 MQ 事件驱动。

### LG-05 双签确认

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/opr/ledgers/{id}/double-sign` |
| **响应** | `R<Void>` |

> **业务规则：** 将 doubleSignStatus 置为 1（已完成），记录双签完成时间。

### LG-06 生成应收计划

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/ledgers/{id}/generate-receivable` |
| **响应** | `R<Integer>` — 生成的应收计划条数 |

> **业务规则：** 从招商合同账期信息生成应收计划明细，receivableStatus 置为 1（已生成）。应收计划的 sourceType=1（合同生成）。

### LG-07 审核台账

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/opr/ledgers/{id}/audit` |
| **响应** | `R<Void>` |

**请求体 AuditDTO：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| auditStatus | Integer | **是** | 审核结果（1 通过/2 驳回） |
| comment | String | 否 | 审核意见 |

### LG-08 手动推送应收至财务系统

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/ledgers/{id}/push-receivable` |
| **响应** | `R<Void>` |

> **业务规则：** 触发应收同步至财务模块（fin_receivable），receivableStatus 置为 2（已推送），记录推送时间。每条应收计划使用幂等键 `receivable_{id}_{version}` 防重。

### LG-09 一次性首款录入

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/ledgers/{id}/one-time-payment` |
| **响应** | `R<Void>` |

**请求体 OneTimePaymentDTO：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| feeItemId | Long | **是** | 收款项目 ID |
| amount | BigDecimal | **是** | 金额 |
| billingStart | LocalDate | 否 | 账期开始日期 |
| billingEnd | LocalDate | 否 | 账期结束日期 |
| entryType | Integer | **是** | 录入类型（1 单笔/2 多笔/3 历史账期） |
| remark | String | 否 | 备注 |

> **业务规则：** 录入一次性首款后，自动生成一条 sourceType=4 的应收计划，并回填 receivableId。

### LG-10 查询台账下应收计划列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/ledgers/{id}/receivables` |
| **响应** | `R<List<OprReceivablePlan>>` |

---

**OprContractLedger 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| ledgerCode | String | 台账编号（唯一） |
| contractId | Long | 关联招商合同 ID |
| projectId | Long | 项目 ID |
| merchantId | Long | 商家 ID |
| brandId | Long | 品牌 ID |
| contractType | Integer | 合同类型（1 租赁/2 联营/3 临时） |
| contractStart | LocalDate | 合同开始日期 |
| contractEnd | LocalDate | 合同到期日期 |
| doubleSignStatus | Integer | 双签状态（0/1） |
| doubleSignDate | LocalDateTime | 双签完成时间 |
| receivableStatus | Integer | 应收生成状态（0/1/2） |
| auditStatus | Integer | 审核状态（0/1/2） |
| status | Integer | 台账状态（0/1/2） |
| pushTime | LocalDateTime | 应收推送时间 |
| createdBy | String | 创建人 |
| createdAt | LocalDateTime | 创建时间 |
| updatedBy | String | 更新人 |
| updatedAt | LocalDateTime | 更新时间 |

**OprReceivablePlan 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| ledgerId | Long | 合同台账 ID |
| contractId | Long | 合同 ID |
| shopId | Long | 商铺 ID |
| feeItemId | Long | 收款项目 ID |
| feeName | String | 费项名称（快照） |
| billingStart | LocalDate | 账期开始 |
| billingEnd | LocalDate | 账期结束 |
| dueDate | LocalDate | 应收日期/付款截止日 |
| amount | BigDecimal | 应收金额 |
| receivedAmount | BigDecimal | 已收金额（默认 0） |
| status | Integer | 状态（0 待收/1 部分/2 已收/3 已作废） |
| pushStatus | Integer | 推送状态（0 未推送/1 已推送） |
| pushTime | LocalDateTime | 推送财务时间 |
| pushIdempotentKey | String | 推送幂等键 |
| sourceType | Integer | 来源（1 合同/2 变更/3 浮动租金/4 一次性） |
| version | Integer | 版本号（变更后递增） |

**OprOneTimePayment 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| ledgerId | Long | 合同台账 ID |
| contractId | Long | 合同 ID |
| feeItemId | Long | 收款项目 ID |
| receivableId | Long | 关联生成的应收计划 ID（回填） |
| amount | BigDecimal | 金额 |
| billingStart | LocalDate | 账期开始 |
| billingEnd | LocalDate | 账期结束 |
| entryType | Integer | 录入类型（1/2/3） |
| remark | String | 备注 |

---

## 19. 合同变更管理 — OprContractChangeController `/opr/contract-changes`

> **状态流转：** 草稿(0) → 审批中(1) → 通过(2) / 驳回(3)
> **核心机制：** 变更通过后触发应收计划重算引擎，并保存变更前快照

### CG-01 变更单分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/contract-changes` |
| **响应** | `R<IPage<OprContractChange>>` |

**查询参数 ChangeQueryDTO：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20 |
| contractId | Long | 否 | 原合同 ID |
| ledgerId | Long | 否 | 台账 ID |
| projectId | Long | 否 | 项目 ID |
| changeCode | String | 否 | 变更单号（模糊查询） |
| status | Integer | 否 | 状态（0 草稿/1 审批中/2 通过/3 驳回） |
| changeTypeCode | String | 否 | 变更类型编码（RENT/BRAND/TENANT/FEE/CLAUSE/TERM/AREA/COMPANY） |

### CG-02 变更单详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/contract-changes/{id}` |
| **响应** | `R<ChangeDetailVO>` — 含变更类型/字段明细/快照 |

**响应字段 ChangeDetailVO（继承 OprContractChange）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| *（OprContractChange 全部字段）* | — | 见实体字段表 |
| changeTypeCodes | List\<String\> | 变更类型编码列表 |
| changeTypeNames | List\<String\> | 变更类型名称列表（展示用） |
| details | List\<OprContractChangeDetail\> | 字段级变更明细列表 |
| statusName | String | 状态名称 |
| contractCode | String | 合同编号 |
| contractName | String | 合同名称 |
| merchantName | String | 商家名称 |
| projectName | String | 项目名称 |

### CG-03 新增变更单（草稿）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/contract-changes` |
| **响应** | `R<Long>` — 新建变更单 ID |

**请求体 ChangeCreateDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 原合同 ID |
| ledgerId | Long | 否 | - | 关联台账 ID |
| changeTypeCodes | List\<String\> | **是** | @NotNull | 变更类型编码列表（可多选） |
| effectiveDate | LocalDate | **是** | @NotNull | 变更生效日期 |
| reason | String | 否 | - | 变更原因 |
| changeFields | Map\<String, Object\> | 否 | - | 动态变更字段（按类型携带不同 key-value） |

> **changeFields 约定：**
> - `RENT/FEE` → `rentAmount(BigDecimal)`, `rentUnit(String)`
> - `TERM` → `newContractStart(LocalDate)`, `newContractEnd(LocalDate)`
> - `AREA` → `newRentArea(BigDecimal)`
> - `BRAND` → `newBrandId(Long)`, `newBrandName(String)`
> - `TENANT` → `newMerchantId(Long)`, `newMerchantName(String)`
> - `COMPANY` → `newCompanyName(String)`
> - `CLAUSE` → `clauseContent(String)`

> **业务规则：** 状态自动置为草稿(0)，系统自动生成变更单号（BG + yyyyMMdd + 4 位流水）。

### CG-04 编辑变更单

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/opr/contract-changes/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 仅草稿(0)或驳回(3)状态可编辑。

### CG-05 预览变更影响

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/contract-changes/{id}/preview-impact` |
| **响应** | `R<ChangeImpactVO>` — 受影响应收笔数/金额差异 |

**响应字段 ChangeImpactVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| affectedPlanCount | int | 受影响应收计划笔数 |
| originalTotalAmount | BigDecimal | 原应收总金额 |
| newTotalAmount | BigDecimal | 变更后预估总金额 |
| amountDiff | BigDecimal | 差额（正数增加/负数减少） |
| fieldComparisons | List\<Map\<String, String\>\> | 变更字段前后对比 |
| impactDesc | String | 影响说明文本 |

> **业务规则：** 计算结果暂存至 impact_summary（JSON 字段），供审批前确认使用。

### CG-06 提交 OA 审批

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/contract-changes/{id}/submit-approval` |
| **响应** | `R<Void>` |

> **业务规则：** 状态从草稿(0) → 审批中(1)，调用 OA 审批引擎（当前 Mock 实现）。

### CG-07 审批回调

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/contract-changes/{id}/approval-callback` |
| **响应** | `R<Void>` |

**请求体 ApprovalCallbackDTO：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approvalId | String | 否 | OA 审批实例 ID |
| status | Integer | **是** | 审批结果（2 通过/3 驳回） |
| comment | String | 否 | 审批意见 |

> **业务规则：**
> - 通过(2)：保存变更前快照（合同主表/费项/应收），触发 ReceivableRecalculate 引擎重算应收计划
> - 驳回(3)：状态回退，可重新编辑后再次提交

### CG-08 合同变更历史时间线

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/contract-changes/history/{contractId}` |
| **响应** | `R<List<ChangeDetailVO>>` — 按时间倒序 |

---

**OprContractChange 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| changeCode | String | 变更单号（唯一） |
| contractId | Long | 原合同 ID |
| ledgerId | Long | 关联台账 ID |
| projectId | Long | 项目 ID |
| status | Integer | 状态（0/1/2/3） |
| effectiveDate | LocalDate | 变更生效日期 |
| reason | String | 变更原因 |
| approvalId | String | OA 审批流程实例 ID |
| impactSummary | JsonNode | 变更影响预览暂存（JSON） |

**OprContractChangeType 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| changeId | Long | 变更单 ID |
| changeTypeCode | String | 变更类型编码 |

**OprContractChangeDetail 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| changeId | Long | 变更单 ID |
| fieldName | String | 变更字段名 |
| fieldLabel | String | 字段中文名 |
| oldValue | String | 变更前值 |
| newValue | String | 变更后值 |
| dataType | String | 数据类型（string/decimal/date） |

**OprContractChangeSnapshot 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| changeId | Long | 变更单 ID |
| snapshotType | Integer | 快照类型（1 合同主表/2 费项/3 应收） |
| snapshotData | String | 快照数据（JSON 序列化） |

---

## 20. 营收填报与浮动租金 — OprRevenueReportController `/opr/revenue-reports` + `/opr/floating-rents`

> **营收填报：** 按天录入商铺营业额，支持 Excel 批量导入，日历视图查看
> **浮动租金：** 根据月营业额汇总 + 计租方案计算浮动租金，支持三种计费模式

### RV-01 营收填报分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/revenue-reports` |
| **响应** | `R<IPage<OprRevenueReport>>` |

**查询参数 RevenueReportQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（继承自 PageQuery） |
| pageSize | Integer | 否 | 每页条数（继承自 PageQuery） |
| projectId | Long | 否 | 项目 ID |
| contractId | Long | 否 | 合同 ID |
| merchantId | Long | 否 | 商家 ID |
| shopId | Long | 否 | 商铺 ID |
| reportMonth | String | 否 | 填报月份（YYYY-MM） |
| reportDateFrom | String | 否 | 填报日期起（YYYY-MM-DD） |
| reportDateTo | String | 否 | 填报日期止（YYYY-MM-DD） |
| status | Integer | 否 | 状态（0 待确认/1 已确认） |

### RV-02 营收填报新增

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/revenue-reports` |
| **响应** | `R<Long>` — 新建记录 ID |

**请求体 RevenueReportCreateDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 合同 ID |
| projectId | Long | 否 | - | 项目 ID（自动从合同获取） |
| shopId | Long | 否 | - | 商铺 ID（自动从合同获取） |
| merchantId | Long | 否 | - | 商家 ID（自动从合同获取） |
| reportDate | LocalDate | **是** | @NotNull | 填报日期 |
| revenueAmount | BigDecimal | **是** | @NotNull @DecimalMin("0.00") | 营业额（不能为负数） |

> **业务规则：** reportMonth 由系统根据 reportDate 自动计算。同一合同同一日期唯一（含 is_deleted 的联合唯一键，应用层需额外校验 is_deleted=0）。

### RV-03 营收填报修改

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/opr/revenue-reports/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 仅待确认(0)状态可修改。

### RV-04 Excel 批量导入

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/revenue-reports/import` |
| **Content-Type** | `multipart/form-data` |
| **响应** | `R<?>` — 导入结果（成功/失败条数） |

**请求参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | **是** | Excel 文件 |

### RV-05 导出 Excel

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/revenue-reports/export` |
| **响应** | 直接流式输出 Excel 文件（HttpServletResponse） |

> 查询条件同 RV-01。

### RV-06 下载导入模板

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/revenue-reports/template` |
| **响应** | 直接流式输出 Excel 模板（HttpServletResponse） |

### RV-07 查询指定月份每日明细

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/revenue-reports/daily-detail` |
| **响应** | `R<List<OprRevenueReport>>` — 供日历视图着色 |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contractId | Long | **是** | 合同 ID |
| reportMonth | String | **是** | 填报月份（YYYY-MM） |

### RV-08 触发浮动租金计算

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/revenue-reports/generate-floating-rent` |
| **响应** | `R<Long>` — 浮动租金记录 ID |

**请求体 GenerateFloatingRentDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 合同 ID |
| calcMonth | String | **是** | @NotBlank | 计算月份（YYYY-MM） |

> **业务规则：** 调用 FloatingRentCalculator 引擎，汇总该月所有营收记录，根据合同计租方案（提成/阶梯/取高）计算浮动租金。同一合同同一月份唯一。

### RV-09 营收月度汇总统计

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/revenue-reports/statistics` |
| **响应** | `R<RevenueStatisticsVO>` |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reportMonth | String | **是** | 统计月份（YYYY-MM） |
| projectId | Long | 否 | 项目 ID 过滤 |
| contractId | Long | 否 | 合同 ID 过滤 |

**响应字段 RevenueStatisticsVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| reportMonth | String | 统计月份 |
| totalRevenue | BigDecimal | 月总营业额 |
| reportedContractCount | Integer | 已填报合同数 |
| totalDays | Integer | 当月天数 |
| details | List\<ContractMonthlyVO\> | 按合同汇总明细 |

**嵌套字段 ContractMonthlyVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| contractId | Long | 合同 ID |
| contractCode | String | 合同编号 |
| shopId | Long | 商铺 ID |
| shopCode | String | 商铺编号 |
| merchantName | String | 商家名称 |
| monthlyRevenue | BigDecimal | 月累计营业额 |
| reportDays | Integer | 已填报天数 |
| totalDays | Integer | 当月天数 |
| complete | Boolean | 是否填报完整 |

### RV-10 浮动租金分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/floating-rents` |
| **响应** | `R<IPage<OprFloatingRent>>` |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contractId | Long | 否 | 合同 ID |
| calcMonth | String | 否 | 计算月份（YYYY-MM） |
| pageNum | int | 否 | 页码，默认 1 |
| pageSize | int | 否 | 每页条数，默认 20 |

### RV-11 浮动租金详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/floating-rents/{id}` |
| **响应** | `R<FloatingRentDetailVO>` — 含阶梯明细 |

**响应字段 FloatingRentDetailVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 浮动租金 ID |
| contractId | Long | 合同 ID |
| shopId | Long | 商铺 ID |
| calcMonth | String | 计算月份 |
| monthlyRevenue | BigDecimal | 月营业额 |
| fixedRent | BigDecimal | 固定租金（取高模式比较用） |
| commissionRate | BigDecimal | 提成比例（%） |
| commissionAmount | BigDecimal | 提成金额 |
| floatingRent | BigDecimal | 浮动租金（最终结果） |
| calcFormula | String | 计算公式说明 |
| receivableId | Long | 关联应收记录 ID（未生成则为 NULL） |
| chargeType | Integer | 计费类型（2 固定提成/3 阶梯提成/4 取高） |
| chargeTypeName | String | 计费类型名称 |
| tiers | List\<TierDetailVO\> | 阶梯明细（仅阶梯模式） |

**嵌套字段 TierDetailVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| tierNo | Integer | 档位序号（从 1 开始） |
| revenueFrom | BigDecimal | 本档起始营业额（NULL 表示从 0） |
| revenueTo | BigDecimal | 本档终止营业额（NULL 表示无上限） |
| rate | BigDecimal | 本档提成比例（%） |
| tierAmount | BigDecimal | 本档提成金额 |

> **浮动租金三种计费模式：**
> 1. **固定提成(chargeType=2)：** commissionAmount = monthlyRevenue × commissionRate
> 2. **阶梯累进(chargeType=3)：** 按区间逐档计算，每档明细存 tiers
> 3. **取高(chargeType=4)：** floatingRent = max(fixedRent, commissionAmount)

### RV-12 浮动租金生成应收计划

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/floating-rents/{id}/generate-receivable` |
| **响应** | `R<Long>` — 生成的应收计划 ID |

> **业务规则：** 基于浮动租金记录创建一条 sourceType=3 的应收计划，回填 receivableId。

---

**OprRevenueReport 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目 ID |
| contractId | Long | 合同 ID |
| shopId | Long | 商铺 ID |
| merchantId | Long | 商家 ID |
| reportDate | LocalDate | 填报日期 |
| reportMonth | String | 填报月份（YYYY-MM） |
| revenueAmount | BigDecimal | 营业额 |
| status | Integer | 状态（0 待确认/1 已确认） |

**OprFloatingRent 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| contractId | Long | 合同 ID |
| shopId | Long | 商铺 ID |
| calcMonth | String | 计算月份（YYYY-MM） |
| monthlyRevenue | BigDecimal | 月营业额 |
| fixedRent | BigDecimal | 固定租金 |
| commissionRate | BigDecimal | 提成比例（%） |
| commissionAmount | BigDecimal | 提成金额 |
| floatingRent | BigDecimal | 浮动租金 |
| calcFormula | String | 计算公式说明 |
| receivableId | Long | 关联应收记录 ID |

**OprFloatingRentTier 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| floatingRentId | Long | 浮动租金记录 ID |
| tierNo | Integer | 阶梯档位序号 |
| revenueFrom | BigDecimal | 本档起始营业额 |
| revenueTo | BigDecimal | 本档终止营业额 |
| rate | BigDecimal | 本档提成比例（%） |
| tierAmount | BigDecimal | 本档提成金额 |

---

## 21. 客流填报管理 — OprPassengerFlowController `/opr/passenger-flows`

> **功能：** 按天按位置录入客流人数，支持 Excel 导入导出，提供日/周环比 + 趋势分析

### PF-01 客流分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/passenger-flows` |
| **响应** | `R<IPage<OprPassengerFlow>>` |

**查询参数 PassengerFlowQueryDTO：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20 |
| projectId | Long | 否 | 项目 ID |
| buildingId | Long | 否 | 楼栋 ID |
| floorId | Long | 否 | 楼层 ID |
| startDate | LocalDate | 否 | 填报日期起（含） |
| endDate | LocalDate | 否 | 填报日期止（含） |
| sourceType | Integer | 否 | 数据来源（1 手动/2 导入/3 设备） |

### PF-02 新增客流填报

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/passenger-flows` |
| **响应** | `R<Long>` — 新建记录 ID |

**请求体 PassengerFlowCreateDTO：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | **是** | 项目 ID |
| buildingId | Long | 否 | 楼栋 ID（项目整体录入时不传） |
| floorId | Long | 否 | 楼层 ID（精确到楼层时传入） |
| reportDate | LocalDate | **是** | 填报日期 |
| flowCount | Integer | **是** | 客流人数 |
| sourceType | Integer | 否 | 数据来源（默认 1 手动） |

> **业务规则：** 同一 project+building+floor+date 唯一（联合唯一键）。

### PF-03 编辑客流填报

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/opr/passenger-flows/{id}` |
| **响应** | `R<Void>` |

### PF-04 删除客流填报

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/opr/passenger-flows/{id}` |
| **响应** | `R<Void>` |

### PF-05 批量导入客流

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/passenger-flows/import` |
| **Content-Type** | `multipart/form-data` |
| **响应** | `R<?>` — 导入结果 |

### PF-06 导出客流报表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/passenger-flows/export` |
| **响应** | 直接流式输出 Excel 文件 |

> 查询条件同 PF-01。

### PF-07 客流统计分析

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/passenger-flows/statistics` |
| **响应** | `R<PassengerFlowStatisticsVO>` |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目 ID |
| buildingId | Long | 否 | 楼栋 ID |
| floorId | Long | 否 | 楼层 ID |

**响应字段 PassengerFlowStatisticsVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| todayFlow | Integer | 今日客流 |
| yesterdayFlow | Integer | 昨日客流 |
| dayOverDayRate | Double | 日环比增幅（%，正数增长/负数下降） |
| thisWeekFlow | Integer | 本周合计 |
| lastWeekFlow | Integer | 上周合计 |
| weekOverWeekRate | Double | 周环比增幅（%） |
| last30DaysFlow | Integer | 近 30 天总客流 |
| trendPoints | List\<DailyPoint\> | 近 30 天每日趋势（日期升序） |

**嵌套字段 DailyPoint：**

| 字段 | 类型 | 说明 |
|------|------|------|
| date | String | 日期（yyyy-MM-dd） |
| flowCount | Integer | 客流人数 |

---

**OprPassengerFlow 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| projectId | Long | 项目 ID |
| buildingId | Long | 楼栋 ID |
| floorId | Long | 楼层 ID |
| reportDate | LocalDate | 填报日期 |
| flowCount | Integer | 客流人数 |
| sourceType | Integer | 数据来源（1/2/3） |

---

## 22. 合同解约管理 — OprContractTerminationController `/opr/terminations`

> **状态流转：** 草稿(0) → 审批中(1) → 已生效(2) / 驳回(3)
> **清算引擎：** TerminationSettlementEngine — 根据解约类型自动计算违约金/退款/未结算金额

### TM-01 解约单分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/terminations` |
| **响应** | `R<IPage<OprContractTermination>>` |

**查询参数 TerminationQueryDTO：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20 |
| contractId | Long | 否 | 合同 ID |
| projectId | Long | 否 | 项目 ID |
| terminationType | Integer | 否 | 解约类型（1 到期/2 提前/3 重签） |
| status | Integer | 否 | 状态（0 草稿/1 审批中/2 已生效/3 驳回） |
| terminationCode | String | 否 | 解约单号（模糊查询） |

### TM-02 解约单详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/terminations/{id}` |
| **响应** | `R<TerminationDetailVO>` — 含清算明细 |

**响应字段 TerminationDetailVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 解约单 ID |
| terminationCode | String | 解约单号 |
| contractId | Long | 原合同 ID |
| ledgerId | Long | 台账 ID |
| projectId | Long | 项目 ID |
| merchantId | Long | 商家 ID |
| brandId | Long | 品牌 ID |
| shopId | Long | 商铺 ID |
| terminationType | Integer | 解约类型 |
| terminationDate | LocalDate | 解约日期 |
| reason | String | 解约原因 |
| newContractId | Long | 重签新合同 ID |
| penaltyAmount | BigDecimal | 违约金 |
| refundDeposit | BigDecimal | 退还保证金 |
| unsettledAmount | BigDecimal | 未结算应收 |
| settlementAmount | BigDecimal | 清算总额 |
| status | Integer | 状态 |
| approvalId | String | 审批 ID |
| statusName | String | 状态名称 |
| terminationTypeName | String | 解约类型名称 |
| contractCode | String | 合同编号 |
| contractName | String | 合同名称 |
| merchantName | String | 商家名称 |
| projectName | String | 项目名称 |
| shopCode | String | 商铺编号 |
| settlements | List\<OprTerminationSettlement\> | 清算明细列表 |

### TM-03 新增解约单（草稿）

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/terminations` |
| **响应** | `R<Long>` — 新建解约单 ID |

**请求体 TerminationCreateDTO：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contractId | Long | **是** | 原合同 ID |
| ledgerId | Long | 否 | 关联台账 ID（可选，系统自动查找） |
| terminationType | Integer | **是** | 解约类型（1 到期/2 提前/3 重签） |
| terminationDate | LocalDate | **是** | 解约日期 |
| reason | String | 否 | 解约原因 |
| newContractId | Long | 否 | 重签新合同 ID（解约类型=3 时必填） |
| penaltyRate | BigDecimal | 否 | 违约金比例（提前解约时使用，0~1 之间，如 0.3 表示 30%） |

> **业务规则：** 状态自动置为草稿(0)，系统自动生成解约单号（JY + yyyyMMdd + 4 位流水）。

### TM-04 编辑解约单

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/opr/terminations/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 仅草稿(0)或驳回(3)状态可编辑。

### TM-05 计算清算金额

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/terminations/{id}/calculate-settlement` |
| **响应** | `R<Void>` |

> **业务规则：** 调用 TerminationSettlementEngine 引擎，按解约类型计算：
> - **到期(1)：** 清算剩余未收费用
> - **提前(2)：** 违约金（按日折算当期租金 × penaltyRate）+ 未收费用
> - **重签(3)：** 关联新合同 ID，清空原合同剩余应收
>
> 计算结果写入 opr_termination_settlement 明细表，并汇总到主表（penaltyAmount / refundDeposit / unsettledAmount / settlementAmount）。

### TM-06 提交 OA 审批

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/terminations/{id}/submit-approval` |
| **响应** | `R<Void>` |

> **业务规则：** 状态从草稿(0) → 审批中(1)。

### TM-07 审批回调

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/opr/terminations/{id}/approval-callback` |
| **响应** | `R<Void>` |

**请求体 ApprovalCallbackDTO（复用合同变更模块）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approvalId | String | 否 | OA 审批实例 ID |
| status | Integer | **是** | 审批结果（2 通过/3 驳回） |
| comment | String | 否 | 审批意见 |

> **业务规则：**
> - 通过(2)：执行解约 — 台账状态置为已解约(2)，剩余未收应收计划作废(3)，商铺状态恢复可招商，取消关联预警
> - 驳回(3)：状态回退至草稿(0)

---

**OprContractTermination 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| terminationCode | String | 解约单号（唯一） |
| contractId | Long | 原合同 ID |
| ledgerId | Long | 关联台账 ID |
| projectId | Long | 项目 ID |
| merchantId | Long | 商家 ID |
| brandId | Long | 品牌 ID |
| shopId | Long | 商铺 ID |
| terminationType | Integer | 解约类型（1/2/3） |
| terminationDate | LocalDate | 解约日期 |
| reason | String | 解约原因 |
| newContractId | Long | 重签新合同 ID |
| penaltyAmount | BigDecimal | 违约金（默认 0） |
| refundDeposit | BigDecimal | 退还保证金（默认 0） |
| unsettledAmount | BigDecimal | 未结算应收（默认 0） |
| settlementAmount | BigDecimal | 清算总额（默认 0） |
| status | Integer | 状态（0/1/2/3） |
| approvalId | String | OA 审批流程 ID |

**OprTerminationSettlement 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| terminationId | Long | 解约单 ID |
| itemType | Integer | 明细类型（1 未收租金/2 违约金/3 保证金退还/4 其他费用） |
| itemName | String | 明细名称 |
| amount | BigDecimal | 金额（正数应收/负数应退） |
| remark | String | 备注 |

---

## 23. 预警记录管理 — OprAlertRecordController `/opr/alerts`

> **预警机制：** AlertScheduler 定时扫描合同到期/应收到期，自动生成预警记录
> **防重机制：** 唯一键 (alert_type, target_id, alert_date, channel)
> **解约联动：** 合同解约/续签时将关联预警 sent_status 置为 3（已取消）

### AL-01 分页查询预警记录

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/opr/alerts` |
| **响应** | `R<IPage<OprAlertRecord>>` |

**查询参数 AlertQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（继承自 PageQuery） |
| pageSize | Integer | 否 | 每页条数（继承自 PageQuery） |
| alertType | Integer | 否 | 预警类型（1 合同到期/2 应收到期，空=不过滤） |
| sentStatus | Integer | 否 | 发送状态（0 待发送/1 已发送/2 失败/3 已取消，空=不过滤） |
| targetId | Long | 否 | 预警目标 ID（台账 ID 或应收计划 ID） |
| alertDateFrom | LocalDate | 否 | 预警日期起 |
| alertDateTo | LocalDate | 否 | 预警日期止 |

### AL-02 手动取消预警记录

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/opr/alerts/{id}` |
| **响应** | `R<Void>` |

> **业务规则：** 将 sent_status 置为 3（已取消），非物理删除。

---

**OprAlertRecord 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| alertType | Integer | 预警类型（1 合同到期/2 应收到期） |
| targetId | Long | 预警目标 ID |
| alertDate | LocalDate | 预警触发日期 |
| channel | Integer | 发送渠道（1 站内信/2 邮件/3 短信） |
| sentStatus | Integer | 发送状态（0/1/2/3） |
| sentTime | LocalDateTime | 实际发送时间 |
| remark | String | 备注 |

---

## 营运管理 — 汇总统计

> **数据来源：** `asset-operation` 模块源码（端口 8003） · 生成日期 2026-03-19

| 维度 | 数量 |
|------|------|
| 控制器 | 6 |
| API 端点总数 | **46** |
| 查询 DTO | 6（LedgerQueryDTO, ChangeQueryDTO, RevenueReportQueryDTO, TerminationQueryDTO, PassengerFlowQueryDTO, AlertQueryDTO） |
| 创建/操作 DTO | 7（AuditDTO, OneTimePaymentDTO, ChangeCreateDTO, ApprovalCallbackDTO, RevenueReportCreateDTO, GenerateFloatingRentDTO, TerminationCreateDTO, PassengerFlowCreateDTO） |
| 响应 VO | 8（LedgerDetailVO, LedgerSelectorVO, ChangeDetailVO, ChangeImpactVO, FloatingRentDetailVO, RevenueStatisticsVO, TerminationDetailVO, PassengerFlowStatisticsVO） |
| 实体类 | 14 |
| 数据库表 | 14 张（opr_* 前缀） |

**各控制器端点分布：**

| 控制器 | 端点数 |
|--------|--------|
| OprContractLedgerController | 10 |
| OprContractChangeController | 8 |
| OprRevenueReportController | 12 |
| OprPassengerFlowController | 7 |
| OprContractTerminationController | 7 |
| OprAlertRecordController | 2 |

> **注：** 含 2 类审批流程（合同变更、合同解约），均提供 submit-approval + approval-callback 成对端点。审批回调 DTO 共用 `ApprovalCallbackDTO`（定义于 change 包，termination 复用）。当前阶段审批引擎为 Mock 实现，后续对接真实 OA 系统。

---

## 营运管理 — 代码 vs 技术分析报告差异

| 差异点 | 技术报告设计 | 代码实际实现 |
|--------|------------|------------|
| **路径前缀** | `/api/v1/operation/*` | `/opr/*`（由 Vite 代理添加 `/api` 前缀） |
| **台账创建方式** | MQ 事件驱动自动创建 | `POST /opr/ledgers/from-contract/{contractId}` 手动触发（预留 MQ 入口） |
| **台账审核** | 报告设计为独立审批引擎 | 代码实现为简单审核接口（AuditDTO: 1 通过/2 驳回） |
| **变更类型存储** | 报告设计单字段 | 代码实现为独立关联表 `opr_contract_change_type`，支持多类型复选 |
| **变更影响预览** | 报告未涉及 | 代码新增 `preview-impact` 端点，计算并暂存影响数据 |
| **快照存储** | 报告设计为 JSON 字段 | 代码实现为独立表 `opr_contract_change_snapshot`，支持多类型快照（合同/费项/应收） |
| **营收填报** | 报告设计按月批量提交 | 代码实现为按天单条录入 + Excel 批量导入 |
| **浮动租金控制器** | 报告设计为独立控制器 | 代码合并在 `OprRevenueReportController` 中，路径前缀 `/opr/floating-rents` |
| **客流填报** | 报告设计含设备对接 API | 代码实现 sourceType=3（设备）为预留值，无设备对接接口 |
| **解约执行** | 报告设计为独立 `execute` 端点 | 代码合并在 `approval-callback` 中，通过审批回调触发解约执行 |
| **预警管理** | 报告设计含配置/规则管理 | 代码实现为简化版（仅查询 + 取消），规则硬编码在 AlertScheduler 定时任务中 |
| **ApprovalCallbackDTO** | 报告设计各模块独立 DTO | 代码实现为合同变更定义一份，解约模块直接复用 |

---

# 四、财务管理（asset-finance，端口 8004）

> **模块职责：** 完整的收支管理和财务核算体系 — 应收 → 收款 → 核销 → 凭证 → 保证金/预收款 → 财务看板
> **控制器路径前缀：** `/fin/*`
> **数据库表前缀：** `fin_`（13 张表）
> **数据来源：** `asset-finance` 模块源码 · 生成日期 2026-03-19

---

## 财务管理 — 状态枚举字典

### 应收状态 ReceivableStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | PENDING | 待收 |
| 1 | PARTIAL | 部分收款 |
| 2 | RECEIVED | 已收 |
| 3 | VOID | 已作废 |
| 4 | REDUCED | 已减免 |

### 收款单状态 ReceiptStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | PENDING | 待核销 |
| 1 | PARTIAL | 部分核销 |
| 2 | DONE | 已核销 |
| 3 | VOID | 已作废 |

### 核销单状态

| 值 | 含义 |
|----|------|
| 0 | 待审核 |
| 1 | 审核通过 |
| 2 | 驳回 |

### 核销类型 WriteOffType

| 值 | 枚举 | 含义 |
|----|------|------|
| 1 | NORMAL | 正常核销 |
| 2 | NEGATIVE | 负数核销 |
| 3 | BALANCE | 余额处理 |

### 凭证状态 VoucherStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | DRAFT | 草稿 |
| 1 | AUDITED | 已审核 |
| 2 | POSTED | 已过账 |

### 保证金操作类型 DepositOperationType

| 值 | 枚举 | 含义 |
|----|------|------|
| 1 | PAY_IN | 缴纳 |
| 2 | DEDUCT | 冲抵应收 |
| 3 | REFUND | 退款 |
| 4 | FORFEIT | 罚没 |

### 预收款操作类型 PrepaymentOperationType

| 值 | 枚举 | 含义 |
|----|------|------|
| 1 | RECEIVE | 暂存入账 |
| 2 | DEDUCT | 抵冲应收 |
| 3 | REFUND | 退款 |

### 减免/调整审批状态

| 值 | 含义 |
|----|------|
| 0 | 待审批 |
| 1 | 通过 |
| 2 | 驳回 |

---

## 财务管理 — 业务类型字典

### 收款方式 paymentMethod

| 值 | 含义 |
|----|------|
| 1 | 银行转账 |
| 2 | 现金 |
| 3 | 支票 |
| 4 | POS |

### 凭证收付类型 payType

| 值 | 含义 |
|----|------|
| 1 | 收款 |
| 2 | 付款 |

### 凭证分录来源类型 sourceType

| 值 | 含义 |
|----|------|
| 1 | 收款单 |
| 2 | 核销单 |
| 3 | 应收单 |

### 保证金流水交易类型 transType

| 值 | 含义 |
|----|------|
| 1 | 收入（缴纳） |
| 2 | 冲抵应收 |
| 3 | 退款 |
| 4 | 罚没 |

### 预收款流水交易类型 transType

| 值 | 含义 |
|----|------|
| 1 | 转入（超额转预存/手动录入） |
| 2 | 抵冲应收 |
| 3 | 退款 |

### 调整类型 adjustType

| 值 | 含义 |
|----|------|
| 1 | 增加 |
| 2 | 减少 |

---

## 24. 应收管理 — FinReceivableController `/fin/receivables`

> **核心公式：**
> - `actual_amount = original_amount + adjust_amount - deduction_amount`
> - `outstanding_amount = actual_amount - received_amount`
> - 全额减免时 actual_amount=0，status 置为 4（已减免）
>
> **状态流转：** 待收(0) → 部分收款(1) → 已收(2) / 已作废(3) / 已减免(4)

### RC-01 应收明细分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/receivables` |
| **响应** | `R<IPage<ReceivableDetailVO>>` |

**查询参数 ReceivableQueryDTO：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20 |
| contractId | Long | 否 | 合同 ID |
| projectId | Long | 否 | 项目 ID |
| merchantId | Long | 否 | 商家 ID |
| feeItemId | Long | 否 | 费项 ID |
| status | Integer | 否 | 状态（0/1/2/3/4） |
| accrualMonth | String | 否 | 权责月（YYYY-MM） |
| dueDateFrom | LocalDate | 否 | 应收日期起 |
| dueDateTo | LocalDate | 否 | 应收日期止 |
| overdue | Boolean | 否 | true=仅查逾期记录 |
| receivableCode | String | 否 | 应收编码（模糊查询） |

**响应字段 ReceivableDetailVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 应收 ID |
| receivableCode | String | 应收编码 |
| contractId | Long | 合同 ID |
| contractCode | String | 合同编号 |
| contractName | String | 合同名称 |
| projectId | Long | 项目 ID |
| projectName | String | 项目名称 |
| merchantId | Long | 商家 ID |
| merchantName | String | 商家名称 |
| feeItemId | Long | 费项 ID |
| feeName | String | 费项名称 |
| billingStart | LocalDate | 账期开始 |
| billingEnd | LocalDate | 账期结束 |
| dueDate | LocalDate | 应收日期 |
| accrualMonth | String | 权责月 |
| originalAmount | BigDecimal | 原始应收金额 |
| adjustAmount | BigDecimal | 累计调整金额 |
| deductionAmount | BigDecimal | 累计减免金额 |
| actualAmount | BigDecimal | 实际应收 |
| receivedAmount | BigDecimal | 已收金额 |
| outstandingAmount | BigDecimal | 欠费金额 |
| status | Integer | 状态 |
| statusName | String | 状态名称 |
| overdueDays | Integer | 逾期天数 |
| isOverdue | Boolean | 是否逾期 |
| isPrinted | Integer | 是否已打印（0/1） |
| isInvoiced | Integer | 是否已开票（0/1） |

### RC-02 应收详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/receivables/{id}` |
| **响应** | `R<FinReceivable>` |

### RC-03 应收合同汇总

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/receivables/summary` |
| **响应** | `R<List<ReceivableSummaryVO>>` — 按合同/项目维度汇总 |

**响应字段 ReceivableSummaryVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| contractId | Long | 合同 ID |
| contractCode | String | 合同编号 |
| contractName | String | 合同名称 |
| merchantId | Long | 商家 ID |
| merchantName | String | 商家名称 |
| projectId | Long | 项目 ID |
| projectName | String | 项目名称 |
| totalOriginal | BigDecimal | 原始应收合计 |
| totalActual | BigDecimal | 实际应收合计 |
| totalReceived | BigDecimal | 已收合计 |
| totalDeduction | BigDecimal | 减免合计 |
| totalOutstanding | BigDecimal | 未收合计 |
| overdueCount | Integer | 逾期条数 |
| overdueAmount | BigDecimal | 逾期金额 |

### RC-04 欠费统计

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/receivables/overdue-statistics` |
| **响应** | `R<OverdueStatisticsVO>` — 三档欠费统计 + TOP10 |

**响应字段 OverdueStatisticsVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| overdue30Amount | BigDecimal | 逾期 30 天内金额 |
| overdue30To90Amount | BigDecimal | 逾期 30~90 天金额 |
| overdueOver90Amount | BigDecimal | 逾期 90 天以上金额 |
| totalOverdueAmount | BigDecimal | 逾期总金额 |
| totalOverdueCount | Integer | 逾期总条数 |
| topDebtors | List\<ReceivableSummaryVO\> | 欠费租户 TOP10 |

### RC-05 导出应收 Excel

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/receivables/export` |
| **响应** | 直接流式输出 Excel 文件（HttpServletResponse） |

> 查询条件同 RC-01。使用 EasyExcel 生成。

### RC-06 从营运计划同步应收

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/receivables/sync-from-plan/{planId}` |
| **响应** | `R<Long>` — 应收记录 ID（幂等，已存在则返回现有 ID） |

> **业务规则：** 从营运模块应收计划（opr_receivable_plan）同步数据创建 fin_receivable 记录。基于 planId 幂等，重复调用不会产生重复记录。

### RC-07 刷新逾期天数

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/receivables/refresh-overdue` |
| **响应** | `R<Void>` |

> **业务规则：** 手动触发全量刷新应收记录的逾期天数。也可由定时任务自动执行。

### RC-08 提交减免申请

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/receivables/deduction` |
| **响应** | `R<Long>` — 减免单 ID |

**请求体 DeductionCreateDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| receivableId | Long | **是** | @NotNull | 应收记录 ID |
| deductionAmount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 减免金额（正数，不超过欠费额） |
| reason | String | **是** | @NotBlank | 减免原因 |

> **业务规则：** 创建减免单（status=0 待审批），自动提交 OA 审批（bizType="FIN_DEDUCTION"）。减免金额不得超过 outstandingAmount。

### RC-09 减免审批回调

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/fin/receivables/deduction/{id}/callback` |
| **响应** | `R<Void>` |

> **业务规则：**
> - 通过：更新减免单 status=1，累加应收记录 deductionAmount，调用 recalcAmounts() 重算 actualAmount/outstandingAmount/status
> - 驳回：更新减免单 status=2
> - 通过 approvalId 查找单据（selectByApprovalIdForUpdate）

### RC-10 提交调整申请

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/receivables/adjustment` |
| **响应** | `R<Long>` — 调整单 ID |

**请求体 AdjustmentCreateDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| receivableId | Long | **是** | @NotNull | 应收记录 ID |
| adjustType | Integer | **是** | @NotNull | 调整类型（1 增加/2 减少） |
| adjustAmount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 调整金额（正数） |
| reason | String | **是** | @NotBlank | 调整原因 |

> **业务规则：** 创建调整单（status=0 待审批），自动提交 OA 审批（bizType="FIN_ADJUSTMENT"）。减少型调整金额不得超过 actualAmount。

### RC-11 调整审批回调

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/fin/receivables/adjustment/{id}/callback` |
| **响应** | `R<Void>` |

> **业务规则：**
> - 通过：更新调整单 status=1，根据 adjustType 更新应收记录 adjustAmount（增加则+，减少则-），调用 recalcAmounts() 重算
> - 驳回：更新调整单 status=2

### RC-12 批量标记已打印

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/receivables/mark-printed` |
| **响应** | `R<Void>` |

**请求体：** `List<Long>` — 应收记录 ID 列表

> **业务规则：** 将指定应收记录的 isPrinted 置为 1。

---

**FinReceivable 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| receivableCode | String | 应收编码 |
| contractId | Long | 合同 ID |
| ledgerId | Long | 合同台账 ID |
| projectId | Long | 项目 ID |
| merchantId | Long | 商家 ID |
| shopId | Long | 商铺 ID |
| feeItemId | Long | 费项 ID |
| feeName | String | 费项名称（冗余） |
| billingStart | LocalDate | 账期开始 |
| billingEnd | LocalDate | 账期结束 |
| accrualMonth | String | 权责月（YYYY-MM） |
| dueDate | LocalDate | 应收日期 |
| originalAmount | BigDecimal | 原始应收金额（不可修改） |
| adjustAmount | BigDecimal | 累计调整金额 |
| deductionAmount | BigDecimal | 累计减免金额 |
| actualAmount | BigDecimal | 实际应收 = original + adjust - deduction |
| receivedAmount | BigDecimal | 已收金额 |
| outstandingAmount | BigDecimal | 欠费金额 = actual - received |
| status | Integer | 状态（0/1/2/3/4） |
| isPrinted | Integer | 是否已打印（0/1） |
| isInvoiced | Integer | 是否已开票（0/1） |
| version | Integer | 乐观锁版本号 |

**FinReceivableDeduction 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| deductionCode | String | 减免单号 |
| receivableId | Long | 应收记录 ID |
| contractId | Long | 合同 ID |
| deductionAmount | BigDecimal | 减免金额 |
| reason | String | 减免原因 |
| status | Integer | 状态（0 待审批/1 通过/2 驳回） |
| approvalId | String | OA 审批流程 ID |
| version | Integer | 乐观锁版本号 |

**FinReceivableAdjustment 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| adjustmentCode | String | 调整单号 |
| receivableId | Long | 应收记录 ID |
| contractId | Long | 合同 ID |
| adjustType | Integer | 调整类型（1 增加/2 减少） |
| adjustAmount | BigDecimal | 调整金额（正数） |
| reason | String | 调整原因 |
| status | Integer | 状态（0 待审批/1 通过/2 驳回） |
| approvalId | String | OA 审批流程 ID |
| version | Integer | 乐观锁版本号 |

---

## 25. 收款管理 — FinReceiptController `/fin/receipts`

> **状态流转：** 待核销(0) → 部分核销(1) → 已核销(2) / 已作废(3)
> **关键字段：** writeOffAmount（已核销金额）、prepayAmount（转预存金额）
> **未名款项：** isUnnamed=1 表示尚未绑定合同的款项，需后续通过 bind 接口关联

### RT-01 收款单分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/receipts` |
| **响应** | `R<IPage<FinReceipt>>` |

**查询参数 ReceiptQueryDTO：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20 |
| contractId | Long | 否 | 合同 ID |
| merchantId | Long | 否 | 商家 ID |
| projectId | Long | 否 | 项目 ID |
| status | Integer | 否 | 核销状态（0/1/2/3） |
| isUnnamed | Integer | 否 | 是否未名款项（0/1） |
| paymentMethod | Integer | 否 | 收款方式（1/2/3/4） |
| receiptDateFrom | LocalDate | 否 | 收款日期起 |
| receiptDateTo | LocalDate | 否 | 收款日期止 |
| receiptCode | String | 否 | 收款单号（模糊查询） |

### RT-02 收款单详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/receipts/{id}` |
| **响应** | `R<ReceiptDetailVO>` — 含费项拆分明细 |

**响应字段 ReceiptDetailVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 收款单 ID |
| receiptCode | String | 收款单号 |
| contractId | Long | 合同 ID |
| contractCode | String | 合同编号 |
| contractName | String | 合同名称 |
| projectId | Long | 项目 ID |
| projectName | String | 项目名称 |
| merchantId | Long | 商家 ID |
| merchantName | String | 商家名称 |
| brandId | Long | 品牌 ID |
| shopCode | String | 店铺编号 |
| totalAmount | BigDecimal | 实收总金额 |
| paymentMethod | Integer | 收款方式 |
| paymentMethodName | String | 收款方式名称 |
| bankSerialNo | String | 银行流水号 |
| payerName | String | 付款方名称 |
| bankName | String | 收款银行 |
| bankAccount | String | 收款账号 |
| isUnnamed | Integer | 是否未名款项 |
| accountingEntity | String | 核算主体 |
| receiptDate | LocalDate | 收款日期 |
| receiver | String | 收款人 |
| status | Integer | 状态 |
| statusName | String | 状态名称 |
| writeOffAmount | BigDecimal | 已核销金额 |
| prepayAmount | BigDecimal | 转预存款金额 |
| details | List\<FinReceiptDetail\> | 费项拆分明细列表 |

### RT-03 新增收款单

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/receipts` |
| **响应** | `R<Long>` — 新建收款单 ID |

**请求体 ReceiptCreateDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | 否 | - | 合同 ID（未名款项时可为空） |
| brandId | Long | 否 | - | 品牌 ID |
| shopCode | String | 否 | - | 店铺编号 |
| totalAmount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 实收总金额 |
| paymentMethod | Integer | 否 | - | 收款方式（默认 1 银行转账） |
| bankSerialNo | String | 否 | - | 银行流水号 |
| payerName | String | 否 | - | 付款方名称 |
| bankName | String | 否 | - | 收款银行 |
| bankAccount | String | 否 | - | 收款账号 |
| isUnnamed | Integer | 否 | - | 是否未名款项（默认 0） |
| accountingEntity | String | 否 | - | 核算主体 |
| receiptDate | LocalDate | **是** | @NotNull | 收款日期 |
| receiver | String | 否 | - | 收款人 |
| details | List\<ReceiptDetailItemDTO\> | 否 | @Valid @Size(max=20) | 费项拆分明细（合计须等于 totalAmount；为空时后端自动创建一条全额明细） |

**嵌套字段 ReceiptDetailItemDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| feeItemId | Long | 否 | - | 费项 ID |
| feeName | String | 否 | - | 费项名称 |
| amount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 拆分金额 |
| remark | String | 否 | - | 备注 |

> **业务规则：** 系统自动生成收款单号，初始状态为待核销(0)。从合同自动关联 projectId/merchantId。

### RT-04 编辑收款单

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/fin/receipts/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 仅待核销(0)状态可编辑。

### RT-05 作废收款单

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/fin/receipts/{id}/cancel` |
| **响应** | `R<Void>` |

> **业务约束：** 仅待核销(0)状态可作废，status 置为 3。

### RT-06 未名款项绑定合同

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/fin/receipts/{id}/bind` |
| **响应** | `R<Void>` |

**请求体：** `FinReceipt`（需包含 contractId 等关联字段）

> **业务规则：** 将 isUnnamed=1 的收款单绑定到合同，同时更新关联的 projectId/merchantId。

---

**FinReceipt 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| receiptCode | String | 收款单号 |
| contractId | Long | 合同 ID |
| projectId | Long | 项目 ID |
| merchantId | Long | 商家 ID |
| brandId | Long | 品牌 ID |
| shopCode | String | 店铺编号 |
| totalAmount | BigDecimal | 实收总金额 |
| paymentMethod | Integer | 收款方式（1/2/3/4） |
| bankSerialNo | String | 银行流水号 |
| payerName | String | 付款方名称 |
| bankName | String | 收款银行 |
| bankAccount | String | 收款账号 |
| isUnnamed | Integer | 是否未名款项（0/1） |
| accountingEntity | String | 核算主体 |
| receiptDate | LocalDate | 收款日期 |
| receiver | String | 收款人 |
| status | Integer | 状态（0/1/2/3） |
| writeOffAmount | BigDecimal | 已核销金额 |
| prepayAmount | BigDecimal | 转预存款金额 |
| version | Integer | 乐观锁版本号 |

**FinReceiptDetail 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| receiptId | Long | 收款单 ID |
| feeItemId | Long | 费项 ID |
| feeName | String | 费项名称（冗余） |
| amount | BigDecimal | 拆分金额 |
| remark | String | 备注 |

---

## 26. 核销管理 — FinWriteOffController `/fin/write-offs`

> **核销流程：** 选择收款单 → 选择可核销应收 → 逐行分配核销金额 → 提交 OA → 回调逐行更新应收 → 超额转预存
> **并发控制：** 应收记录使用乐观锁（version），收款单使用乐观锁

### WO-01 核销单分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/write-offs` |
| **响应** | `R<IPage<FinWriteOff>>` |

**查询参数 WriteOffQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（继承自 PageQuery） |
| pageSize | Integer | 否 | 每页条数（继承自 PageQuery） |
| writeOffCode | String | 否 | 核销单号 |
| receiptId | Long | 否 | 收款单 ID |
| contractId | Long | 否 | 合同 ID |
| merchantId | Long | 否 | 商家 ID |
| projectId | Long | 否 | 项目 ID |
| writeOffType | Integer | 否 | 核销类型（1 正常/2 负数/3 余额处理） |
| status | Integer | 否 | 状态（0 待审核/1 通过/2 驳回） |

### WO-02 查询可核销应收列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/write-offs/writable-receivables` |
| **响应** | `R<List<WritableReceivableVO>>` — 供核销弹窗选择 |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contractId | Long | **是** | 合同 ID |

**响应字段 WritableReceivableVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 应收记录 ID |
| receivableCode | String | 应收编码 |
| feeItemId | Long | 费项 ID |
| feeName | String | 费项名称 |
| accrualMonth | String | 权责月 |
| billingStart | LocalDate | 账期开始 |
| billingEnd | LocalDate | 账期结束 |
| dueDate | LocalDate | 应收日期 |
| actualAmount | BigDecimal | 实际应收金额 |
| receivedAmount | BigDecimal | 已收金额 |
| outstandingAmount | BigDecimal | 欠费金额（待核销余额） |
| status | Integer | 状态（0 待收/1 部分收款） |

### WO-03 提交核销申请

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/write-offs` |
| **响应** | `R<Long>` — 核销单 ID |

**请求体 SubmitWriteOffRequest（Controller 内部类）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| receiptId | Long | **是** | 收款单 ID |
| writeOffType | Integer | 否 | 核销类型（默认 1 正常核销） |
| items | List\<WriteOffDetailItemDTO\> | **是** | 核销明细行列表 |

**嵌套字段 WriteOffDetailItemDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| receivableId | Long | **是** | @NotNull | 应收记录 ID |
| feeItemId | Long | 否 | - | 费项 ID（从应收带出） |
| accrualMonth | String | 否 | - | 权责月（从应收带出） |
| writeOffAmount | BigDecimal | **是** | @NotNull | 本次核销金额（负数核销时为负值） |

> **业务规则：**
> - 校验收款单余额 ≥ 核销明细总金额
> - 创建核销单（status=0 待审核），自动提交 OA 审批（bizType="FIN_WRITE_OFF"）
> - 核销明细总金额不得超过收款单的（totalAmount - writeOffAmount - prepayAmount）

### WO-04 核销审批回调

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/write-offs/approval-callback` |
| **响应** | `R<Void>` |

> **业务规则（通过时）：**
> 1. 核销单 status → 1
> 2. 逐行更新应收记录：receivedAmount += writeOffAmount，调用 recalcAmounts()
> 3. 超额部分（writeOffAmount > outstandingAmount）转入预存款（fin_prepay_account）
> 4. 更新收款单：writeOffAmount 累加，prepayAmount 累加超额部分
> 5. 收款单状态机：全部核销 → status=2，部分核销 → status=1

### WO-05 取消核销单

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/fin/write-offs/{id}/cancel` |
| **响应** | `R<Void>` |

> **业务约束：** 仅 status=0（待审核）状态可取消。

### WO-06 核销单详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/write-offs/{id}` |
| **响应** | `R<WriteOffDetailVO>` |

**响应字段 WriteOffDetailVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 核销单 ID |
| writeOffCode | String | 核销单号 |
| receiptId | Long | 收款单 ID |
| receiptCode | String | 收款单号 |
| contractId | Long | 合同 ID |
| contractCode | String | 合同编号 |
| contractName | String | 合同名称 |
| merchantId | Long | 商家 ID |
| merchantName | String | 商家名称 |
| projectId | Long | 项目 ID |
| projectName | String | 项目名称 |
| writeOffType | Integer | 核销类型 |
| writeOffTypeName | String | 核销类型名称 |
| totalAmount | BigDecimal | 核销总金额 |
| status | Integer | 状态 |
| statusName | String | 状态名称 |
| approvalId | String | OA 审批流程 ID |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| details | List\<FinWriteOffDetail\> | 核销明细行 |

---

**FinWriteOff 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| writeOffCode | String | 核销单号 |
| receiptId | Long | 关联收款单 ID |
| contractId | Long | 合同 ID |
| merchantId | Long | 商家 ID |
| projectId | Long | 项目 ID |
| writeOffType | Integer | 核销类型（1/2/3） |
| totalAmount | BigDecimal | 核销总金额 |
| status | Integer | 状态（0/1/2） |
| uploadStatus | Integer | 上传状态（0/1） |
| uploadTime | LocalDateTime | 上传时间 |
| approvalId | String | OA 审批流程 ID |
| version | Integer | 乐观锁版本号 |

**FinWriteOffDetail 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| writeOffId | Long | 核销单 ID |
| receivableId | Long | 应收记录 ID |
| feeItemId | Long | 费项 ID |
| accrualMonth | String | 权责月 |
| writeOffAmount | BigDecimal | 本次核销金额 |
| overpayAmount | BigDecimal | 超出转预存款金额 |

---

## 27. 凭证管理 — FinVoucherController `/fin/vouchers`

> **状态流转：** 草稿(0) → 已审核(1) → 已过账(2)
> **核心约束：** 分录至少 2 条，借贷必须平衡（totalDebit = totalCredit）

### VC-01 凭证分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/vouchers` |
| **响应** | `R<IPage<FinVoucher>>` |

**查询参数 VoucherQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（继承自 PageQuery） |
| pageSize | Integer | 否 | 每页条数（继承自 PageQuery） |
| voucherCode | String | 否 | 凭证编号（模糊查询） |
| projectId | Long | 否 | 项目 ID |
| accountSet | String | 否 | 账套 |
| payType | Integer | 否 | 收付类型（1 收款/2 付款） |
| status | Integer | 否 | 状态（0/1/2） |
| dateFrom | LocalDate | 否 | 凭证日期起 |
| dateTo | LocalDate | 否 | 凭证日期止 |

### VC-02 凭证详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/vouchers/{id}` |
| **响应** | `R<VoucherDetailVO>` — 含分录列表 |

**响应字段 VoucherDetailVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 凭证 ID |
| voucherCode | String | 凭证编号 |
| projectId | Long | 项目 ID |
| projectName | String | 项目名称 |
| accountSet | String | 账套 |
| payType | Integer | 收付类型 |
| payTypeName | String | 收付类型名称 |
| voucherDate | LocalDate | 凭证日期 |
| totalDebit | BigDecimal | 借方合计 |
| totalCredit | BigDecimal | 贷方合计 |
| status | Integer | 状态 |
| statusName | String | 状态名称 |
| uploadTime | LocalDateTime | 上传时间 |
| remark | String | 摘要 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |
| entries | List\<FinVoucherEntry\> | 分录列表 |

### VC-03 手动创建凭证

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/vouchers` |
| **响应** | `R<Long>` — 新建凭证 ID |

**请求体 VoucherCreateDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| projectId | Long | **是** | @NotNull | 项目 ID |
| accountSet | String | 否 | - | 账套（默认"默认账套"） |
| payType | Integer | **是** | @NotNull | 收付类型（1 收款/2 付款） |
| voucherDate | LocalDate | **是** | @NotNull | 凭证日期 |
| remark | String | 否 | - | 摘要 |
| entries | List\<VoucherEntryDTO\> | **是** | @NotEmpty @Valid | 分录列表（至少 2 条，借贷须平衡） |

**嵌套字段 VoucherEntryDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| sourceType | Integer | 否 | - | 来源类型（1 收款单/2 核销单/3 应收单） |
| sourceId | Long | 否 | - | 来源单据 ID |
| accountCode | String | **是** | @NotBlank | 会计科目编码 |
| accountName | String | **是** | @NotBlank | 会计科目名称 |
| debitAmount | BigDecimal | **是** | @NotNull | 借方金额（与贷方二选一，另一方为 0） |
| creditAmount | BigDecimal | **是** | @NotNull | 贷方金额 |
| summary | String | 否 | - | 分录摘要 |

> **业务规则：** 系统自动生成凭证编号，初始状态为草稿(0)。自动计算 totalDebit/totalCredit。

### VC-04 从收款单自动生成凭证

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/vouchers/generate-from-receipt/{receiptId}` |
| **响应** | `R<Long>` — 生成的凭证 ID |

> **业务规则：** 根据收款单信息自动生成凭证及分录（借：银行存款 = totalAmount，贷：应收账款 = totalAmount），初始状态为草稿(0)。

### VC-05 审核凭证

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/vouchers/{id}/audit` |
| **响应** | `R<Void>` |

> **业务规则：** 状态从草稿(0) → 已审核(1)。仅草稿状态可审核。

### VC-06 上传至财务系统

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/vouchers/{id}/upload` |
| **响应** | `R<Void>` |

> **业务规则：** 状态从已审核(1) → 已过账(2)，记录 uploadTime。仅已审核状态可上传。

### VC-07 删除凭证

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/fin/vouchers/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 仅草稿(0)状态可删除。

---

**FinVoucher 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| voucherCode | String | 凭证编号 |
| projectId | Long | 项目 ID |
| accountSet | String | 账套 |
| payType | Integer | 收付类型（1/2） |
| voucherDate | LocalDate | 凭证日期 |
| totalDebit | BigDecimal | 借方合计 |
| totalCredit | BigDecimal | 贷方合计 |
| status | Integer | 状态（0/1/2） |
| uploadTime | LocalDateTime | 上传时间 |
| remark | String | 摘要 |
| version | Integer | 乐观锁版本号 |

**FinVoucherEntry 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| voucherId | Long | 凭证 ID |
| sourceType | Integer | 来源类型（1/2/3） |
| sourceId | Long | 来源单据 ID |
| accountCode | String | 会计科目编码 |
| accountName | String | 会计科目名称 |
| debitAmount | BigDecimal | 借方金额 |
| creditAmount | BigDecimal | 贷方金额 |
| summary | String | 分录摘要 |

---

## 28. 保证金管理 — FinDepositController `/fin/deposits`

> **账户模型：** 一个合同对应一个保证金账户（fin_deposit_account），所有操作以流水形式记录在 fin_deposit_transaction
> **余额恒等式：** `balance = total_in - total_offset - total_refund - total_forfeit`
> **并发控制：** 余额更新使用 `SELECT FOR UPDATE` 行级锁 + 乐观锁（version）
> **审批机制：** 缴纳直接生效；冲抵/退款/罚没需 OA 审批

### DP-01 查询保证金账户余额

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/deposits/account` |
| **响应** | `R<DepositAccountVO>` — 余额卡片数据 |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contractId | Long | **是** | 合同 ID |

**响应字段 DepositAccountVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 账户 ID |
| contractId | Long | 合同 ID |
| contractCode | String | 合同编号 |
| contractName | String | 合同名称 |
| merchantId | Long | 商家 ID |
| merchantName | String | 商家名称 |
| projectId | Long | 项目 ID |
| projectName | String | 项目名称 |
| feeItemId | Long | 费项 ID |
| feeItemName | String | 费项名称 |
| balance | BigDecimal | 当前可用余额 |
| totalIn | BigDecimal | 累计收入 |
| totalOffset | BigDecimal | 累计冲抵 |
| totalRefund | BigDecimal | 累计退款 |
| totalForfeit | BigDecimal | 累计罚没 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### DP-02 保证金流水分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/deposits/transactions` |
| **响应** | `R<IPage<FinDepositTransaction>>` |

**查询参数 DepositQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（继承自 PageQuery） |
| pageSize | Integer | 否 | 每页条数（继承自 PageQuery） |
| contractId | Long | 否 | 合同 ID |
| accountId | Long | 否 | 保证金账户 ID |
| transType | Integer | 否 | 交易类型（1 收入/2 冲抵/3 退款/4 罚没） |
| status | Integer | 否 | 状态（0 待审核/1 已审核/2 驳回） |

### DP-03 保证金缴纳

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/deposits/pay-in` |
| **响应** | `R<Long>` — 流水记录 ID |

**请求体 DepositPayInDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 合同 ID |
| amount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 缴纳金额 |
| sourceCode | String | 否 | - | 关联收款单号 |
| reason | String | 否 | - | 备注 |

> **业务规则：** 直接生效（无需审批）。balance += amount，totalIn += amount。账户不存在时自动创建。

### DP-04 保证金冲抵应收

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/deposits/offset` |
| **响应** | `R<Long>` — 流水记录 ID |

**请求体 DepositOffsetDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 合同 ID（定位账户） |
| receivableId | Long | **是** | @NotNull | 应收记录 ID（冲抵目标） |
| amount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 冲抵金额 |
| reason | String | 否 | - | 冲抵原因 |

> **业务规则：** 需 OA 审批。冲抵金额不得超过账户余额和应收欠费额的较小值。审批通过后：balance -= amount，totalOffset += amount，应收 receivedAmount += amount。

### DP-05 保证金退款

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/deposits/refund` |
| **响应** | `R<Long>` — 流水记录 ID |

**请求体 DepositRefundDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 合同 ID |
| amount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 退款金额 |
| reason | String | 否 | - | 退款原因 |
| bankName | String | 否 | - | 退款银行 |
| bankAccount | String | 否 | - | 退款账号 |
| payee | String | 否 | - | 收款人 |

> **业务规则：** 需 OA 审批。退款金额不得超过账户余额。审批通过后：balance -= amount，totalRefund += amount。

### DP-06 保证金罚没

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/deposits/forfeit` |
| **响应** | `R<Long>` — 流水记录 ID |

**请求体 DepositForfeitDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 合同 ID |
| amount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 罚没金额 |
| reason | String | **是** | @NotBlank | 罚没原因（违约事项说明） |

> **业务规则：** 需 OA 审批。罚没金额不得超过账户余额。审批通过后：balance -= amount，totalForfeit += amount。

### DP-07 保证金审批回调

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/deposits/approval-callback` |
| **响应** | `R<Void>` |

**请求体 ApprovalCallbackRequest（Controller 内部类）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| approvalId | String | **是** | OA 审批实例 ID |
| approved | boolean | **是** | 审批结果（true 通过/false 驳回） |

> **业务规则：** 通过 approvalId 查找待审核流水记录，通过则执行余额变动 + 状态更新，驳回则仅更新状态。

---

**FinDepositAccount 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| contractId | Long | 合同 ID |
| merchantId | Long | 商家 ID |
| projectId | Long | 项目 ID |
| feeItemId | Long | 保证金费项 ID |
| balance | BigDecimal | 当前余额 |
| totalIn | BigDecimal | 累计收入 |
| totalOffset | BigDecimal | 累计冲抵 |
| totalRefund | BigDecimal | 累计退款 |
| totalForfeit | BigDecimal | 累计罚没 |
| version | Integer | 乐观锁版本号 |

**FinDepositTransaction 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| accountId | Long | 保证金账户 ID |
| transType | Integer | 交易类型（1/2/3/4） |
| amount | BigDecimal | 交易金额 |
| balanceAfter | BigDecimal | 交易后余额 |
| transDate | LocalDate | 交易日期 |
| sourceCode | String | 关联单据号 |
| reason | String | 原因说明 |
| status | Integer | 状态（0 待审核/1 已审核） |
| approvalId | String | OA 审批流程 ID |

---

## 29. 预收款管理 — FinPrepaymentController `/fin/prepayments`

> **账户模型：** 一个合同对应一个预收款账户（fin_prepay_account），核销超额自动转入
> **操作特点：** 所有操作直接生效（无需 OA 审批），简化流程
> **来源：** 核销超额自动转入 / 手动录入

### PP-01 查询预收款账户余额

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/prepayments/account` |
| **响应** | `R<PrepayAccountVO>` — 余额卡片数据 |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| contractId | Long | **是** | 合同 ID |

**响应字段 PrepayAccountVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 账户 ID |
| contractId | Long | 合同 ID |
| contractCode | String | 合同编号 |
| contractName | String | 合同名称 |
| merchantId | Long | 商家 ID |
| merchantName | String | 商家名称 |
| projectId | Long | 项目 ID |
| projectName | String | 项目名称 |
| feeItemId | Long | 费项 ID（空表示通用账户） |
| balance | BigDecimal | 当前可用余额 |
| createTime | LocalDateTime | 创建时间 |
| updateTime | LocalDateTime | 更新时间 |

### PP-02 预收款流水分页列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/prepayments/transactions` |
| **响应** | `R<IPage<FinPrepayTransaction>>` |

**查询参数 PrepayQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码（继承自 PageQuery） |
| pageSize | Integer | 否 | 每页条数（继承自 PageQuery） |
| contractId | Long | 否 | 合同 ID |
| accountId | Long | 否 | 预收款账户 ID |
| transType | Integer | 否 | 交易类型（1 转入/2 抵冲/3 退款） |

### PP-03 手动录入预收款

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/prepayments/deposit` |
| **响应** | `R<Long>` — 流水记录 ID |

**请求体 PrepayDepositDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 合同 ID |
| amount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 录入金额 |
| sourceCode | String | 否 | - | 关联收款单号 |
| remark | String | 否 | - | 备注 |

> **业务规则：** 直接生效。balance += amount。账户不存在时自动创建。

### PP-04 预收款抵冲应收

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/prepayments/offset` |
| **响应** | `R<Long>` — 流水记录 ID |

**请求体 PrepayOffsetDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 合同 ID（定位账户） |
| receivableId | Long | **是** | @NotNull | 应收记录 ID |
| amount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 抵冲金额 |
| remark | String | 否 | - | 备注 |

> **业务规则：** 直接生效。抵冲金额不得超过账户余额和应收欠费额的较小值。balance -= amount，应收 receivedAmount += amount。

### PP-05 预收款退款

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/fin/prepayments/refund` |
| **响应** | `R<Long>` — 流水记录 ID |

**请求体 PrepayRefundDTO：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| contractId | Long | **是** | @NotNull | 合同 ID |
| amount | BigDecimal | **是** | @NotNull @DecimalMin("0.01") | 退款金额 |
| bankName | String | 否 | - | 退款银行 |
| bankAccount | String | 否 | - | 退款账号 |
| payee | String | 否 | - | 收款人 |
| remark | String | 否 | - | 备注 |

> **业务规则：** 直接生效。退款金额不得超过账户余额。balance -= amount。

---

**FinPrepayAccount 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| contractId | Long | 合同 ID |
| merchantId | Long | 商家 ID |
| projectId | Long | 项目 ID |
| feeItemId | Long | 费项 ID |
| balance | BigDecimal | 当前余额 |
| version | Integer | 乐观锁版本号 |

**FinPrepayTransaction 实体字段：**

| 字段 | 类型 | 含义 |
|------|------|------|
| id | Long | 主键 |
| accountId | Long | 预收款账户 ID |
| transType | Integer | 类型（1 转入/2 抵冲/3 退款） |
| amount | BigDecimal | 金额 |
| balanceAfter | BigDecimal | 交易后余额 |
| transDate | LocalDate | 交易日期 |
| sourceCode | String | 关联单据号 |
| remark | String | 备注 |

---

## 30. 财务看板 — FinDashboardController `/fin/dashboard`

> **功能：** 提供财务全局视角 — 四张统计卡片 + 两个饼图 + 收款趋势折线图 + 欠费 TOP10 横向柱图

### DB-01 看板汇总数据

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/dashboard/summary` |
| **响应** | `R<DashboardSummaryVO>` — 四卡片 + 两饼图 |

**响应字段 DashboardSummaryVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| monthReceivable | BigDecimal | 本月应收合计 |
| monthReceived | BigDecimal | 本月已收合计 |
| currentOverdue | BigDecimal | 当前欠费合计（已逾期应收） |
| monthWriteOffCount | Long | 本月核销笔数 |
| feeTypeDistribution | List\<NameValueVO\> | 应收费项分布（饼图数据） |
| writeOffTypeDistribution | List\<NameValueVO\> | 核销方式分布（饼图数据） |

**嵌套字段 NameValueVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 名称 |
| value | BigDecimal | 数值 |

### DB-02 近 12 个月收款趋势

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/dashboard/receipt-trend` |
| **响应** | `R<List<ReceiptTrendVO>>` — 折线图数据 |

**响应字段 ReceiptTrendVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| month | String | 月份（YYYY-MM） |
| amount | BigDecimal | 当月实收金额 |

### DB-03 欠费 TOP10 商家

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/fin/dashboard/overdue-top` |
| **响应** | `R<List<OverdueTopVO>>` — 横向柱图数据 |

**响应字段 OverdueTopVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| merchantId | Long | 商家 ID |
| merchantName | String | 商家名称 |
| overdueAmount | BigDecimal | 欠费金额 |

---

## 财务管理 — 汇总统计

> **数据来源：** `asset-finance` 模块源码（端口 8004） · 生成日期 2026-03-19

| 维度 | 数量 |
|------|------|
| 控制器 | 7 |
| API 端点总数 | **46** |
| 查询 DTO | 6（ReceivableQueryDTO, ReceiptQueryDTO, WriteOffQueryDTO, VoucherQueryDTO, DepositQueryDTO, PrepayQueryDTO） |
| 创建/操作 DTO | 12（DeductionCreateDTO, AdjustmentCreateDTO, ReceiptCreateDTO, ReceiptDetailItemDTO, WriteOffDetailItemDTO, VoucherCreateDTO, VoucherEntryDTO, DepositPayInDTO, DepositOffsetDTO, DepositRefundDTO, DepositForfeitDTO, PrepayDepositDTO, PrepayOffsetDTO, PrepayRefundDTO） |
| 响应 VO | 10（ReceivableDetailVO, ReceivableSummaryVO, OverdueStatisticsVO, ReceiptDetailVO, WritableReceivableVO, WriteOffDetailVO, VoucherDetailVO, DepositAccountVO, PrepayAccountVO, DashboardSummaryVO, ReceiptTrendVO, OverdueTopVO） |
| 实体类 | 13 |
| 数据库表 | 13 张（fin_* 前缀） |
| 枚举 | 6（ReceivableStatus, ReceiptStatus, WriteOffType, VoucherStatus, DepositOperationType, PrepaymentOperationType） |

**各控制器端点分布：**

| 控制器 | 端点数 |
|--------|--------|
| FinReceivableController | 12 |
| FinReceiptController | 6 |
| FinWriteOffController | 6 |
| FinVoucherController | 7 |
| FinDepositController | 7 |
| FinPrepaymentController | 5 |
| FinDashboardController | 3 |

> **注：** 含 4 类审批流程（减免申请、调整申请、核销申请、保证金冲抵/退款/罚没），其中核销和保证金通过 OA 回调端点触发执行，减免/调整通过独立回调端点。预收款操作全部直接生效，无需审批。核心交易表均使用乐观锁（@Version），保证金/预收款余额更新额外使用行级锁（SELECT FOR UPDATE）。

---

## 财务管理 — 代码 vs 技术分析报告差异

| 差异点 | 技术报告设计 | 代码实际实现 |
|--------|------------|------------|
| **路径前缀** | `/api/v1/finance/*` | `/fin/*`（由 Vite 代理添加 `/api` 前缀） |
| **应收状态值** | 报告设计 0/1/2/3 四种 | 代码实际 0/1/2/3/4 五种（增加 4=已减免） |
| **核销类型** | 报告设计收款核销/保证金核销/预收款核销 | 代码实现 1 正常核销/2 负数核销/3 余额处理（WriteOffType 枚举） |
| **保证金/预收款实体** | 报告设计 FinDeposit/FinDepositRecord | 代码重构为 FinDepositAccount + FinDepositTransaction（账户+流水分离） |
| **预收款审批** | 报告设计需 OA 审批 | 代码实现全部直接生效（简化流程） |
| **保证金缴纳** | 报告设计需审批 | 代码实现直接生效（仅冲抵/退款/罚没需审批） |
| **凭证状态** | 报告设计草稿/已审核/已上传 | 代码实现 0 草稿/1 已审核/2 已过账（VoucherStatus 枚举名 POSTED） |
| **减免模块** | 报告设计独立 FinReduction 模块 | 代码拆入 receivable 子包（FinReceivableDeduction + FinReceivableAdjustment），废弃原 reduction 包 |
| **核销提交请求** | 报告设计独立 DTO | 代码使用 Controller 内部类 SubmitWriteOffRequest |
| **保证金审批回调** | 报告设计独立 DTO | 代码使用 Controller 内部类 ApprovalCallbackRequest（字段：approvalId + approved boolean） |
| **收款单拆分明细** | 报告未强调 | 代码支持最多 20 条费项拆分（@Size(max=20)），为空时自动创建全额明细 |
| **财务看板** | 报告设计 4 个卡片 | 代码实现 4 卡片 + 2 饼图（费项分布 + 核销方式分布） + 折线图 + 柱图 |

---

# 五、报表管理（asset-report，端口 8005）

> **独立数据库：** `asset_report`（与业务主库 `asset_db` 分离）
> **数据来源：** ETL T+1 预计算聚合表（rpt_*），由 XXL-Job 或手动触发
> **数据权限：** `@RptDataScope` AOP 注入 `permittedProjectIds`
> **财务脱敏：** `FinanceDataMaskUtil.shouldMask()` 控制绝对金额字段置 null

---

## 报表管理 — 通用查询参数

所有报表查询接口（31~34 节）共享 `ReportQueryParam` 参数对象，通过 `@RptDataScope` 切面自动注入数据权限。

### ReportQueryParam 字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 指定单个项目ID（叠加到数据权限之上） |
| buildingId | Long | 否 | 楼栋ID（0/null=不过滤） |
| floorId | Long | 否 | 楼层ID（0/null=不过滤） |
| formatType | String | 否 | 业态类型（null/空=不过滤） |
| merchantId | Long | 否 | 商家ID（财务/账龄报表用） |
| feeItemId | Long | 否 | 费项ID（财务报表用） |
| feeItemType | String | 否 | 费项类型（如租金/物业费，null=全费项汇总） |
| investmentManagerId | Long | 否 | 招商负责人ID（招商报表用） |
| startDate | LocalDate | 否 | 统计开始日期（日报用，含边界） |
| endDate | LocalDate | 否 | 统计结束日期（日报用，含边界） |
| startMonth | String | 否 | 起始月份（月报用，YYYY-MM） |
| endMonth | String | 否 | 结束月份（月报用，YYYY-MM） |
| statDate | LocalDate | 否 | 单个统计日期（精确查询） |
| statMonth | String | 否 | 单个统计月份（精确查询，YYYY-MM） |
| timeUnit | enum | 否 | 时间聚合维度：`DAY`(默认)/`WEEK`/`MONTH`/`YEAR` |
| compareMode | enum | 否 | 对比模式：`NONE`(默认)/`YOY`(同比)/`MOM`(环比) |
| orderBy | String | 否 | 排序字段 |
| pageNum | Integer | 否 | 页码（从1开始，默认1） |
| pageSize | Integer | 否 | 每页条数（默认20） |
| permittedProjectIds | List\<Long\> | — | **AOP 自动注入**，业务层只读。null=管理员无限制，空=无权限返空 |

---

## 报表管理 — 数据权限与脱敏机制

### 数据权限（@RptDataScope）

| 维度 | 说明 |
|------|------|
| **注解** | `@RptDataScope`，标注在 Controller 方法上 |
| **切面** | `ReportDataPermissionAspect`，拦截带注解方法 |
| **注入目标** | `ReportQueryParam.permittedProjectIds` |
| **上下文** | `ReportPermissionContext`（ThreadLocal） |
| **权限语义** | `null`=管理员无限制；空列表=无权限返空；非空列表=IN 过滤 |

### 财务数据脱敏

| 维度 | 说明 |
|------|------|
| **工具类** | `FinanceDataMaskUtil` |
| **判断方法** | `shouldMask()` — 非管理员 + 无财务查看权限时返回 true |
| **脱敏策略** | 绝对金额字段置 `null`，保留同比/环比趋势百分比 |
| **影响接口** | FR-01（财务看板）、FR-02（应收汇总） |

---

## 报表管理 — 业务类型字典

### ETL 汇总粒度

| 聚合表 | 粒度维度 | 更新频率 |
|--------|----------|----------|
| rpt_asset_daily | 项目/楼栋/楼层/业态 | 每日 T+1 |
| rpt_investment_daily | 项目/业态/招商负责人 | 每日 T+1 |
| rpt_operation_monthly | 项目/楼栋/业态 | 每月 T+1 |
| rpt_finance_monthly | 项目/费项 | 每月 T+1 |
| rpt_aging_analysis | 项目/商家/合同/费项 | 每日 T+1 |

### 汇总行约定

| 约定 | 值 | 含义 |
|------|-----|------|
| buildingId=0 | 项目级 | 不区分楼栋的汇总行 |
| floorId=0 | 楼栋级 | 不区分楼层的汇总行 |
| formatType='' | 全业态 | 不区分业态的汇总行 |
| investmentManagerId=0 | 全员 | 不区分招商负责人的汇总行 |
| feeItemId=0 | 全费项 | 不区分费项的汇总行 |

### 报表分类

| 分类值 | 名称 | 说明 |
|--------|------|------|
| 1 | 资产类 | 空置率、出租率、开业率、面积统计 |
| 2 | 招商类 | 意向统计、漏斗、合同、业绩 |
| 3 | 营运类 | 营收、变更、客流、到期、解约 |
| 4 | 财务类 | 应收、收款、欠款、账龄、收缴率 |

### 导出状态

| 状态值 | 常量 | 说明 |
|--------|------|------|
| 0 | STATUS_FAIL | 导出失败 |
| 1 | STATUS_SUCCESS | 导出成功（可下载） |
| 2 | STATUS_PENDING | 导出进行中（轮询） |

### 定时任务状态

| 状态值 | 常量 | 说明 |
|--------|------|------|
| 0 | STATUS_DISABLED | 禁用 |
| 1 | STATUS_ENABLED | 启用 |

### 导出格式

| 值 | 说明 |
|-----|------|
| EXCEL | Excel 文件 |
| PDF | PDF 文件 |

### 生成类型

| 值 | 说明 |
|-----|------|
| MANUAL | 手动触发 |
| SCHEDULE | 定时推送 |

---

## 31. 资产类报表 — ReportAssetController `/rpt/asset`

> **Knife4j 分组：** 01-资产类报表
> **数据来源：** `rpt_asset_daily`（ETL T+1 日汇总）
> **端点前缀编码：** RA-

### RA-01 资产数据看板

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/dashboard` |
| **权限注解** | `@RptDataScope` |
| **功能** | 聚合接口，一次返回核心指标摘要 + 同比/环比 + 30天趋势折线图 + 项目对比柱状图 |
| **响应时间目标** | < 3s |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| compareMode | enum | 否 | NONE/YOY/MOM |
| startDate | LocalDate | 否 | 开始日期 |
| endDate | LocalDate | 否 | 结束日期 |

**响应 `R<AssetDashboardVO>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| summary | Object | 核心指标摘要（总商铺/已租/空置/装修/已开业/三率） |
| trends | List | 30天趋势折线图数据 |
| projectCompare | List | 项目对比柱状图数据 |

---

### RA-02 空置率统计

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/vacancy-rate` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按时间维度返回空置率趋势，compareMode=YOY/MOM 时附带增长率 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| buildingId | Long | 否 | 楼栋ID |
| floorId | Long | 否 | 楼层ID |
| formatType | String | 否 | 业态类型 |
| startDate | LocalDate | 否 | 开始日期 |
| endDate | LocalDate | 否 | 结束日期 |
| timeUnit | enum | 否 | DAY/WEEK/MONTH/YEAR |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<RateTrendVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statDate | String | 统计日期/时段 |
| value | BigDecimal | 空置率（%） |
| growthRate | BigDecimal | 增长率（compareMode 非 NONE 时） |

---

### RA-03 出租率统计

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/rental-rate` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按时间维度返回出租率趋势，参数同 RA-02 |

**响应 `R<List<RateTrendVO>>`** — 结构同 RA-02

---

### RA-04 开业率统计

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/opening-rate` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按时间维度返回开业率趋势，参数同 RA-02 |

**响应 `R<List<RateTrendVO>>`** — 结构同 RA-02

---

### RA-05 商铺租赁信息报表

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/shop-rental` |
| **权限注解** | `@RptDataScope` |
| **功能** | 楼栋/楼层粒度租赁状态统计，支持多维筛选，分页 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| buildingId | Long | 否 | 楼栋ID |
| floorId | Long | 否 | 楼层ID |
| formatType | String | 否 | 业态类型 |
| statDate | LocalDate | 否 | 统计日期（不传取最新） |
| pageNum | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页条数 |

**响应 `R<IPage<ShopRentalVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectName | String | 项目名称 |
| buildingName | String | 楼栋名称 |
| floorName | String | 楼层名称 |
| totalShops | Integer | 商铺总数 |
| rentedShops | Integer | 已租商铺数 |
| vacantShops | Integer | 空置商铺数 |
| decoratingShops | Integer | 装修中商铺数 |
| openedShops | Integer | 已开业商铺数 |
| vacancyRate | BigDecimal | 空置率（%） |
| rentalRate | BigDecimal | 出租率（%） |
| openingRate | BigDecimal | 开业率（%） |

---

### RA-06 品牌/业态分布报表

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/brand-distribution` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按业态分组统计商铺数量、面积及占比 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<BrandDistributionVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| formatType | String | 业态类型 |
| shopCount | Integer | 商铺数量 |
| totalArea | BigDecimal | 面积 |
| rentalRate | BigDecimal | 出租率 |
| percentage | BigDecimal | 占比（%） |

---

### RA-07 商铺拆分合并报表（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/shop-split-merge` |
| **权限注解** | `@RptDataScope` |
| **功能** | 展示有拆分/合并关系的商铺记录，分页 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| buildingId | Long | 否 | 楼栋ID |
| pageNum | Integer | 否 | 页码 |
| pageSize | Integer | 否 | 每页条数 |

**响应 `R<IPage<ShopSplitMergeVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| shopId | Long | 商铺ID |
| shopCode | String | 商铺编码 |
| operationType | String | SPLIT/MERGE/ORIGINAL |
| parentShopId | Long | 父商铺ID |
| originalArea | BigDecimal | 原始面积 |
| currentArea | BigDecimal | 当前面积 |

---

### RA-08 商家分布报表（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/merchant-distribution` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按楼栋/楼层/业态展示商家入驻分布 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| buildingId | Long | 否 | 楼栋ID |
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<MerchantDistributionVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| buildingName | String | 楼栋名称 |
| floorName | String | 楼层名称 |
| formatType | String | 业态类型 |
| merchantCount | Integer | 已签约商家数 |
| rentalRate | BigDecimal | 出租率 |

---

### RA-09 区域归属报表（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/region-summary` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按省/市维度汇总资产指标 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<RegionSummaryVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| province | String | 省份 |
| city | String | 城市 |
| projectCount | Integer | 项目数量 |
| shopCount | Integer | 商铺数量 |
| totalArea | BigDecimal | 总面积 |
| vacancyRate | BigDecimal | 空置率 |
| rentalRate | BigDecimal | 出租率 |
| openingRate | BigDecimal | 开业率 |

---

### RA-10 经营面积统计（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/asset/area-summary` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按项目汇总各维度面积数据，附带已租面积同比增长率 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<AreaSummaryVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectId | Long | 项目ID |
| projectName | String | 项目名称 |
| totalArea | BigDecimal | 总面积（㎡） |
| rentedArea | BigDecimal | 已租面积 |
| vacantArea | BigDecimal | 空置面积 |
| decorationArea | BigDecimal | 装修中面积 |
| rentedAreaGrowthRate | BigDecimal | 已租面积同比增长率 |

---

## 32. 招商类报表 — ReportInvestmentController `/rpt/inv`

> **Knife4j 分组：** 02-招商类报表
> **数据来源：** `rpt_investment_daily`（ETL T+1 日汇总）
> **端点前缀编码：** RI-

### RI-01 招商数据看板

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/inv/dashboard` |
| **权限注解** | `@RptDataScope` |
| **功能** | 聚合接口，一次返回核心指标摘要 + 同比 + 漏斗 + 30天趋势 + 项目业绩对比 |
| **响应时间目标** | < 3s |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| startDate | LocalDate | 否 | 开始日期 |
| endDate | LocalDate | 否 | 结束日期 |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<InvDashboardVO>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| summary | Object | 核心指标（意向数/合同数/签约面积/转化率） |
| funnel | List | 漏斗数据 |
| trends | List | 近30天趋势 |
| performanceCompare | List | 项目业绩对比 |

---

### RI-02 意向客户统计

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/inv/intention-stats` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按时间维度返回意向数量趋势及签约率 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| formatType | String | 否 | 业态类型 |
| investmentManagerId | Long | 否 | 招商负责人ID |
| startDate | LocalDate | 否 | 开始日期 |
| endDate | LocalDate | 否 | 结束日期 |
| timeUnit | enum | 否 | DAY/WEEK/MONTH/YEAR |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<IntentionStatsVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statDate | String | 统计时段 |
| intentionCount | Integer | 意向协议数 |
| intentionSigned | Integer | 已签意向数 |
| newIntention | Integer | 新增意向 |
| conversionRate | BigDecimal | 转化率 |
| growthRate | BigDecimal | 增长率 |

---

### RI-03 客户跟进漏斗

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/inv/funnel` |
| **权限注解** | `@RptDataScope` |
| **功能** | 返回三阶段漏斗：意向登记 → 已签意向 → 已签合同 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<FunnelVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| stage | String | 阶段名称 |
| count | Integer | 数量 |
| conversionRate | BigDecimal | 转化率（%） |

---

### RI-04 合同租赁情况

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/inv/contract-stats` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按时间维度返回合同数量/签约面积/金额/转化率趋势 |

**请求参数** — 同 RI-02

**响应 `R<List<ContractStatsVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statDate | String | 统计时段 |
| contractCount | Integer | 合同数量 |
| contractArea | BigDecimal | 签约面积 |
| contractAmount | BigDecimal | 合同金额 |
| newContract | Integer | 新增合同 |
| conversionRate | BigDecimal | 转化率 |
| growthRate | BigDecimal | 增长率 |

---

### RI-05 招商业绩显差看板

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/inv/performance` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按项目/招商负责人维度对比业绩指标 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| investmentManagerId | Long | 否 | 指定人员（不传取项目整体） |
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<PerformanceVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectName | String | 项目名称 |
| managerName | String | 招商负责人 |
| intentionCount | Integer | 意向数 |
| contractCount | Integer | 合同数 |
| conversionRate | BigDecimal | 转化率 |
| contractArea | BigDecimal | 签约面积 |

---

### RI-06 租金水平分析（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/inv/rent-level` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按项目+业态展示平均租金单价及同比增长 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| investmentManagerId | Long | 否 | 招商负责人ID |
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<RentLevelVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectName | String | 项目名称 |
| formatType | String | 业态类型 |
| avgRentPrice | BigDecimal | 平均租金单价（元/㎡/月） |
| growthRate | BigDecimal | 同比增长率 |

---

### RI-07 租决政策执行报表（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/inv/policy-execution` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按项目+业态展示实际租金执行情况及偏差率 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<PolicyExecutionVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectName | String | 项目名称 |
| formatType | String | 业态类型 |
| avgRentPrice | BigDecimal | 实际平均租金 |
| lastYearAvgRentPrice | BigDecimal | 去年同期平均租金 |
| deviationRate | BigDecimal | 偏差率（正=上涨，负=下降） |

---

### RI-08 品牌签约排行（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/inv/brand-ranking` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按业态分组统计签约数据并排名 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<BrandRankingVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| formatType | String | 业态类型 |
| contractCount | Integer | 签约合同数 |
| contractArea | BigDecimal | 签约面积 |
| contractAmount | BigDecimal | 签约金额 |
| areaPercentage | BigDecimal | 面积占比（%） |
| countPercentage | BigDecimal | 数量占比（%） |

---

## 33. 营运类报表 — ReportOperationController `/rpt/opr`

> **Knife4j 分组：** 03-营运类报表
> **数据来源：** `rpt_operation_monthly`（ETL T+1 月汇总）
> **端点前缀编码：** RO-

### RO-01 营运数据看板

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/dashboard` |
| **权限注解** | `@RptDataScope` |
| **功能** | 聚合接口，一次返回核心指标 + 同比 + 到期预警 + 12月趋势 + 项目对比 |
| **响应时间目标** | < 3s |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| statMonth | String | 否 | 统计月份（YYYY-MM） |

**响应 `R<OprDashboardVO>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| summary | Object | 核心指标（营收/客流/坪效/变更/到期/解约） |
| expiringContracts | List | 到期预警数据 |
| trends | List | 近12月趋势 |
| regionCompare | List | 项目对比 |

---

### RO-02 营收填报汇总

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/revenue-summary` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份维度返回营收/浮动租金/坪效趋势 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| formatType | String | 否 | 业态类型 |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<OprRevenueSummaryVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| revenueAmount | BigDecimal | 营收总额 |
| floatingRentAmount | BigDecimal | 浮动租金总额 |
| avgRevenuePerSqm | BigDecimal | 坪效 |
| growthRate | BigDecimal | 增长率 |

---

### RO-03 合同变更统计

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/contract-changes` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份维度返回变更次数和租金影响额 |

**请求参数** — 同 RO-02

**响应 `R<List<OprContractChangeVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| changeCount | Integer | 变更次数 |
| changeRentImpact | BigDecimal | 变更租金影响额 |
| growthRate | BigDecimal | 变更次数增长率 |

---

### RO-04 租金变更分析

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/rent-changes` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份维度返回变更租金影响额和单次均值 |

**请求参数** — 同 RO-02

**响应 `R<List<OprRentChangeVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| changeRentImpact | BigDecimal | 变更租金影响额汇总 |
| avgRentImpact | BigDecimal | 单次均值 |
| growthRate | BigDecimal | 增长率 |

---

### RO-05 合同到期预警

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/expiring-contracts` |
| **权限注解** | `@RptDataScope` |
| **功能** | 返回各项目 30/60/90 天内到期合同数（分档预警） |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| statMonth | String | 否 | 统计月份（不传取最新） |

**响应 `R<List<OprExpiringContractVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectName | String | 项目名称 |
| within30 | Integer | 30天内到期 |
| within60 | Integer | 60天内到期 |
| within90 | Integer | 90天内到期 |
| total | Integer | 合计 |

---

### RO-06 地区业务对比

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/region-compare` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按项目维度返回多维运营指标（含雷达图评分） |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| statMonth | String | 否 | 统计月份（不传取最新） |

**响应 `R<List<OprRegionCompareVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| projectName | String | 项目名称 |
| revenueAmount | BigDecimal | 营收 |
| passengerFlow | Long | 客流 |
| avgRevenuePerSqm | BigDecimal | 坪效 |
| changeCount | Integer | 变更次数 |
| terminatedContracts | Integer | 解约数 |
| expiringContracts | Integer | 到期数 |
| scores | Map | 百分位归一化评分（雷达图用） |

---

### RO-07 客流数据分析（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/passenger-flow` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份维度返回客流量/日均客流 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<OprPassengerFlowVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| passengerFlow | Long | 月客流总量 |
| avgDailyPassenger | Integer | 日均客流 |
| growthRate | BigDecimal | 增长率 |

---

### RO-08 解约统计（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/termination-stats` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份维度返回解约合同数 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| formatType | String | 否 | 业态类型 |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<OprTerminationStatsVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| terminatedContracts | Integer | 解约合同数 |
| growthRate | BigDecimal | 增长率 |

---

### RO-09 浮动租金统计（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/floating-rent` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份维度返回浮动租金金额 |

**请求参数** — 同 RO-08

**响应 `R<List<OprFloatingRentVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| floatingRentAmount | BigDecimal | 浮动租金金额 |
| growthRate | BigDecimal | 增长率 |

---

### RO-10 合同台账变动（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/opr/ledger-changes` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份维度汇总变更次数/租金影响额/解约数/到期数 |

**请求参数** — 同 RO-08

**响应 `R<List<OprLedgerChangeVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| changeCount | Integer | 合同变更次数 |
| changeRentImpact | BigDecimal | 变更租金影响额 |
| terminatedContracts | Integer | 解约合同数 |
| expiringContracts | Integer | 即将到期合同数 |
| growthRate | BigDecimal | 变更次数增长率 |

---

## 34. 财务类报表 — ReportFinanceController `/rpt/fin`

> **Knife4j 分组：** 04-财务类报表
> **数据来源：** `rpt_finance_monthly`（ETL T+1 月汇总）/ `rpt_aging_analysis`（ETL T+1 日预计算）/ `fin_voucher`（实时）
> **端点前缀编码：** FR-
> **脱敏：** FR-01、FR-02 对非管理员 + 无财务权限用户脱敏绝对金额

### FR-01 财务数据看板

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/dashboard` |
| **权限注解** | `@RptDataScope` |
| **功能** | 聚合接口，返回应收/已收/欠款/逾期指标 + 12月趋势 + 账龄分布 + 欠款TOP10 |
| **脱敏** | 普通用户：totalReceivable/totalReceived/totalOutstanding/totalOverdue/totalDepositBalance/totalPrepayBalance 置 null |
| **响应时间目标** | < 3s |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| statMonth | String | 否 | 统计月份 |

**响应 `R<FinDashboardVO>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| totalReceivable | BigDecimal | 应收总额（脱敏时 null） |
| totalReceived | BigDecimal | 已收总额（脱敏时 null） |
| totalOutstanding | BigDecimal | 欠款总额（脱敏时 null） |
| totalOverdue | BigDecimal | 逾期总额（脱敏时 null） |
| totalDepositBalance | BigDecimal | 保证金余额（脱敏时 null） |
| totalPrepayBalance | BigDecimal | 预收款余额（脱敏时 null） |
| collectionRate | BigDecimal | 收缴率（%，始终可见） |
| overdueRate | BigDecimal | 逾期率（%，始终可见） |
| trends | List | 近12月趋势 |
| agingDistribution | List | 账龄分布 |
| overdueTop10 | List | 欠款TOP10商家 |

---

### FR-02 应收汇总报表

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/receivable-summary` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份/项目/费项维度汇总应收/已收/欠款/减免/收缴率 |
| **脱敏** | 普通用户：receivableAmount/receivedAmount/outstandingAmount/deductionAmount/adjustmentAmount 置 null |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| feeItemType | String | 否 | 费项类型（不传=全费项汇总） |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<FinReceivableSummaryVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| receivableAmount | BigDecimal | 应收总额 |
| receivedAmount | BigDecimal | 已收总额 |
| outstandingAmount | BigDecimal | 欠款总额 |
| deductionAmount | BigDecimal | 减免总额 |
| adjustmentAmount | BigDecimal | 调整总额 |
| collectionRate | BigDecimal | 收缴率（%） |
| growthRate | BigDecimal | 增长率 |

---

### FR-03 收款汇总报表

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/receipt-summary` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份/项目/费项维度汇总已收金额和收缴率 |

**请求参数** — 同 FR-02

**响应 `R<List<FinReceiptSummaryVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| receivedAmount | BigDecimal | 已收总额 |
| collectionRate | BigDecimal | 收缴率（%） |
| growthRate | BigDecimal | 增长率 |

---

### FR-04 欠款统计报表

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/outstanding-summary` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份/项目/费项维度汇总欠款/逾期金额及逾期率 |

**请求参数** — 同 FR-02

**响应 `R<List<FinOutstandingSummaryVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| outstandingAmount | BigDecimal | 欠款总额 |
| overdueAmount | BigDecimal | 逾期金额 |
| overdueRate | BigDecimal | 逾期率（%） |
| growthRate | BigDecimal | 增长率 |

---

### FR-05 账龄分析报表

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/aging-analysis` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按商家/合同维度展示欠款账龄分档（30/60/90/180/365天） |
| **数据来源** | `rpt_aging_analysis` 预计算表 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| merchantId | Long | 否 | 商家ID（不传=所有商家） |
| statDate | LocalDate | 否 | 统计日期（不传取最新） |

**响应 `R<List<FinAgingAnalysisVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| merchantName | String | 商家名称 |
| contractCode | String | 合同编号 |
| within30 | BigDecimal | 30天内欠款 |
| days31_60 | BigDecimal | 31-60天欠款 |
| days61_90 | BigDecimal | 61-90天欠款 |
| days91_180 | BigDecimal | 91-180天欠款 |
| days181_365 | BigDecimal | 181-365天欠款 |
| over365 | BigDecimal | 365天以上欠款 |
| totalOutstanding | BigDecimal | 欠款合计 |

---

### FR-06 逾期率统计

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/overdue-rate` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份/项目维度展示逾期金额和逾期率趋势 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<FinOverdueRateVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| overdueAmount | BigDecimal | 逾期金额 |
| overdueRate | BigDecimal | 逾期率（%） |
| growthRate | BigDecimal | 增长率 |

---

### FR-07 收缴率统计

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/collection-rate` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份/项目/费项维度展示收缴率趋势 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| feeItemType | String | 否 | 费项类型 |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<FinCollectionRateVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| collectionRate | BigDecimal | 收缴率（%） |
| growthRate | BigDecimal | 增长率 |

---

### FR-08 凭证统计（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/voucher-stats` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份/项目维度汇总凭证总数、各状态数量及借贷方合计 |
| **数据来源** | `fin_voucher` 业务表（实时查询） |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |

**响应 `R<List<FinVoucherStatsVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| totalCount | Integer | 凭证总数 |
| draftCount | Integer | 待审核数 |
| approvedCount | Integer | 已审核数 |
| postedCount | Integer | 已过账数 |
| debitTotal | BigDecimal | 借方合计 |
| creditTotal | BigDecimal | 贷方合计 |

---

### FR-09 保证金汇总（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/deposit-summary` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份/项目维度展示保证金余额月末快照趋势 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<FinDepositSummaryVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| depositBalance | BigDecimal | 保证金余额 |
| growthRate | BigDecimal | 增长率 |

---

### FR-10 预收款汇总（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/prepay-summary` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份/项目维度展示预收款余额月末趋势 |

**请求参数** — 同 FR-09

**响应 `R<List<FinPrepaySummaryVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| prepayBalance | BigDecimal | 预收款余额 |
| growthRate | BigDecimal | 增长率 |

---

### FR-11 减免/调整统计（P1）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/fin/deduction-adjustment` |
| **权限注解** | `@RptDataScope` |
| **功能** | 按月份/项目/费项维度统计减免额和调整额及占应收比 |

**请求参数（ReportQueryParam）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| projectId | Long | 否 | 项目ID |
| feeItemType | String | 否 | 费项类型 |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |
| compareMode | enum | 否 | NONE/YOY/MOM |

**响应 `R<List<FinDeductionAdjustmentVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| statMonth | String | 统计月份 |
| deductionAmount | BigDecimal | 减免金额 |
| adjustmentAmount | BigDecimal | 调整金额 |
| deductionRatio | BigDecimal | 减免占应收比（%） |
| adjustmentRatio | BigDecimal | 调整占应收比（%） |
| growthRate | BigDecimal | 增长率 |

---

## 35. 报表导出 — ReportExportController `/rpt/common/export`

> **调用流程：** 提交 → 轮询 → 下载（三步异步模式）
> **端点前缀编码：** EX-

### EX-01 提交导出任务

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/common/export` |
| **权限注解** | `@RptDataScope` |
| **功能** | 提交导出任务，返回 logCode，30分钟内相同参数命中缓存 |

**请求体 `ExportTaskDTO`**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reportCode | String | 是 | 报表编码（如 FIN_RECEIVABLE_SUMMARY） |
| format | String | 否 | 文件格式：EXCEL / PDF |
| params | Map\<String, Object\> | 否 | 查询参数（与报表筛选栏参数一致） |

**响应 `R<String>`** — logCode 字符串

**业务规则**

- reportCode 不能为空，否则返回 `R.fail("reportCode 不能为空")`
- 相同 reportCode + format + params 在 30 分钟内命中缓存直接返回已有 logCode

---

### EX-02 查询导出状态

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/common/export/{logCode}/status` |
| **功能** | 轮询导出状态（建议每 2 秒一次） |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| logCode | String | 任务流水号 |

**响应 `R<ExportTaskStatusVO>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| logCode | String | 任务流水号 |
| status | Integer | 0=失败，1=成功，2=进行中 |
| statusName | String | PENDING / SUCCESS / FAIL |
| fileName | String | 文件名称（成功时） |
| dataCount | Integer | 导出数据条数（成功时） |
| durationMs | Integer | 耗时（毫秒，成功时） |
| errorMsg | String | 错误信息（失败时） |
| downloadUrl | String | 下载地址（成功时）：/rpt/common/export/{logCode}/download |

---

### EX-03 下载导出文件

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/common/export/{logCode}/download` |
| **功能** | 下载文件（仅 status=1 时有效），Content-Disposition: attachment |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| logCode | String | 任务流水号 |

**响应** — 二进制文件流（application/octet-stream）

**异常**

| HTTP 状态码 | 说明 |
|------------|------|
| 404 | 文件不存在或尚未就绪 |
| 410 | 文件已过期，需重新导出 |

---

### EX-04 我的导出记录

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/common/export/my-logs` |
| **功能** | 返回当前登录用户最近 20 条导出历史 |

**响应 `R<List<RptGenerationLog>>`** — 完整实体列表

---

## 36. 数据钻取 — DrillDownController `/rpt/common/drill-down`

> **端点前缀编码：** DD-
> **钻取域：** 资产域四层（项目→楼栋→楼层→商铺）、财务域三层（项目→费项→应收明细）

### DD-01 执行数据钻取（POST）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/common/drill-down` |
| **权限注解** | `@RptDataScope` |
| **功能** | 从父级节点下钻到子级，返回下一层表格结构与数据 |

**请求体 `DrillDownRequestDTO`**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reportCode | String | 是 | 报表编码（ASSET_*/FIN_*/OPR_*/INV_*） |
| fromLevel | Integer | 是 | 当前层级：1=项目, 2=楼栋/费项, 3=楼层, 4=商铺（叶子） |
| dimensionId | Long | 是 | 当前层级的维度ID |
| statDate | String | 否 | 统计日期（资产域用，yyyy-MM-dd） |
| startMonth | String | 否 | 起始月份（财务域用，yyyy-MM） |
| endMonth | String | 否 | 结束月份 |
| feeItemType | String | 否 | 费项类型过滤 |
| extra | Map\<String, Object\> | 否 | 额外过滤参数 |

**响应 `R<DrillDownResultVO>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| currentLevel | int | 返回数据所在层级（fromLevel+1） |
| levelName | String | 当前层级名称（如「楼栋」「楼层」） |
| nextLevelName | String | 下一层名称（null=叶子节点） |
| canDrillDown | boolean | 是否可继续下钻 |
| parentId | Long | 父节点ID（面包屑用） |
| parentName | String | 父节点显示名称 |
| columns | List\<DrillColumnVO\> | 列定义列表 |
| rows | List\<Map\> | 数据行列表（key=列prop, value=单元格值） |
| total | int | 数据总行数 |

**业务规则**

- reportCode 为空返回 `R.fail("reportCode 不能为空")`
- fromLevel >= 4 返回 `R.fail("已到最深层级，无法继续钻取")`

---

### DD-02 执行数据钻取（GET）

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/common/drill-down` |
| **权限注解** | `@RptDataScope` |
| **功能** | GET 方式钻取，适合 ECharts click 事件直接携带参数跳转 |

**请求参数（Query）**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reportCode | String | 是 | 报表编码 |
| fromLevel | Integer | 否 | 当前层级（默认1） |
| dimensionId | Long | 是 | 维度ID |
| statDate | String | 否 | 统计日期 |
| startMonth | String | 否 | 起始月份 |
| endMonth | String | 否 | 结束月份 |
| feeItemType | String | 否 | 费项类型 |

**响应** — 同 DD-01

---

## 37. 报表收藏 — FavoriteController `/rpt/common/favorites`

> **端点前缀编码：** FV-

### FV-01 我的收藏列表

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/common/favorites` |
| **功能** | 返回当前登录用户的全部报表收藏，按 sort_order 升序 |

**响应 `R<List<FavoriteVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 收藏记录ID |
| reportCode | String | 报表编码 |
| reportName | String | 报表名称 |
| routePath | String | 前端路由路径 |
| category | Integer | 分类：1=资产,2=招商,3=营运,4=财务 |
| sortOrder | Integer | 排序值 |
| quickAccess | Boolean | 是否快捷入口 |

---

### FV-02 收藏报表

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/common/favorites` |
| **功能** | 收藏指定报表，重复收藏返回已有 ID |

**请求体 `FavoriteAddDTO`**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| reportCode | String | 是 | @NotBlank | 报表编码 |
| reportName | String | 是 | @NotBlank | 报表名称 |
| routePath | String | 是 | @NotBlank | 前端路由路径 |
| category | Integer | 是 | @NotNull | 分类：1=资产,2=招商,3=营运,4=财务 |
| quickAccess | Boolean | 否 | — | 是否快捷入口（默认 false） |

**响应 `R<Long>`** — 收藏记录 ID

---

### FV-03 取消收藏

| 项目 | 说明 |
|------|------|
| **Method / Path** | `DELETE /rpt/common/favorites/{id}` |
| **功能** | 取消收藏 |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 收藏记录 ID |

**响应 `R<Void>`**

---

### FV-04 更新排序

| 项目 | 说明 |
|------|------|
| **Method / Path** | `PUT /rpt/common/favorites/sort` |
| **功能** | 拖拽排序，按 ids 列表顺序重置 sort_order |

**请求体 `FavoriteSortDTO`**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| ids | List\<Long\> | 是 | @NotEmpty | 按新顺序排列的收藏 ID 列表 |

**响应 `R<Void>`**

---

## 38. 定时推送 — ScheduleTaskController `/rpt/common/schedule-tasks`

> **端点前缀编码：** ST-
> **调度机制：** `ScheduledReportJobHandler` 每分钟扫描 next_run_time <= now 的启用任务并执行

### ST-01 分页查询

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/common/schedule-tasks` |
| **功能** | 分页查询任务列表，支持关键字搜索 |

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 搜索关键字（任务名称/报表编码） |
| pageNum | Integer | 否 | 页码（默认1） |
| pageSize | Integer | 否 | 每页条数（默认10） |

**响应 `R<IPage<ScheduleTaskVO>>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 任务ID |
| taskCode | String | 任务编码 |
| taskName | String | 任务名称 |
| reportCode | String | 报表编码 |
| cronExpression | String | Cron 表达式 |
| recipients | String | 收件人列表（JSON） |
| ccRecipients | String | 抄送人列表（JSON） |
| exportFormat | String | 导出格式 |
| lastRunTime | LocalDateTime | 上次执行时间 |
| nextRunTime | LocalDateTime | 下次执行时间 |
| runCount | Integer | 累计执行次数 |
| failCount | Integer | 连续失败次数 |
| status | Integer | 0=禁用，1=启用 |

---

### ST-02 查询详情

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/common/schedule-tasks/{id}` |
| **功能** | 查询单条任务详情 |

**路径参数**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 任务ID |

**响应 `R<ScheduleTaskVO>`** — 同 ST-01 字段

---

### ST-03 创建任务

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/common/schedule-tasks` |
| **功能** | 创建定时推送任务 |

**请求体 `ScheduleTaskDTO`**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| taskName | String | 是 | @NotBlank | 任务名称 |
| reportCode | String | 是 | @NotBlank | 报表编码 |
| cronExpression | String | 是 | @NotBlank | Cron 表达式（6位Spring格式：秒 分 时 日 月 周） |
| recipients | List\<String\> | 是 | @NotEmpty + @Email | 收件人邮箱列表 |
| ccRecipients | List\<String\> | 否 | — | 抄送人邮箱列表 |
| exportFormat | String | 否 | — | 导出格式（默认 EXCEL） |
| filterParams | Map\<String, Object\> | 否 | — | 固定筛选参数 |
| enabled | Boolean | 否 | — | 是否立即启用（默认 true） |

**响应 `R<Long>`** — 任务 ID

---

### ST-04 更新任务

| 项目 | 说明 |
|------|------|
| **Method / Path** | `PUT /rpt/common/schedule-tasks/{id}` |
| **功能** | 更新定时推送任务 |

**路径参数** — id: Long

**请求体** — 同 ST-03 `ScheduleTaskDTO`

**响应 `R<Void>`**

---

### ST-05 删除任务

| 项目 | 说明 |
|------|------|
| **Method / Path** | `DELETE /rpt/common/schedule-tasks/{id}` |
| **功能** | 逻辑删除任务 |

**响应 `R<Void>`**

---

### ST-06 启用/禁用

| 项目 | 说明 |
|------|------|
| **Method / Path** | `PUT /rpt/common/schedule-tasks/{id}/toggle` |
| **功能** | 切换任务状态；启用时自动重算 nextRunTime；重置连续失败计数 |

**路径参数** — id: Long

**响应 `R<Integer>`** — 新状态值（0=禁用, 1=启用）

**业务规则**

- 连续失败 ≥ 3 次自动禁用
- 启用时重新计算 nextRunTime
- 切换时重置 failCount = 0

---

## 39. ETL 手动触发 — EtlTriggerController `/rpt/etl`

> **端点前缀编码：** ET-
> **用途：** XXL-Job admin 未运行时手动触发 ETL，或补跑历史数据
> **权限：** 生产环境应限制为 admin 角色

### ET-01 一键触发全部 ETL

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/etl/all` |
| **功能** | 顺序执行 5 个 ETL：资产日 + 招商日 + 财务月 + 营运月 + 账龄 |

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| statDate | String | 否 | 日级统计日期（yyyy-MM-dd，空=昨日） |
| statMonth | String | 否 | 月级统计月份（yyyy-MM，空=上月） |

**响应 `R<String>`** — 执行结果摘要，如 `asset=OK; investment=OK; finance=OK; operation=OK; aging=OK`

**业务规则**

- 5 个 ETL 顺序执行，不并行
- 全部成功返回 `R.ok(result)`，任一失败返回 `R.fail(result)`

---

### ET-02 资产日汇总 ETL

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/etl/asset` |
| **功能** | 触发 `AssetDailyEtlJob.execute(statDate)` |

**请求参数** — statDate: String（yyyy-MM-dd，空=昨日）

**响应 `R<String>`** — ETL 执行消息

---

### ET-03 招商日汇总 ETL

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/etl/investment` |
| **功能** | 触发 `InvestmentDailyEtlJob.execute(statDate)` |

**请求参数** — statDate: String（yyyy-MM-dd，空=昨日）

**响应 `R<String>`**

---

### ET-04 财务月汇总 ETL

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/etl/finance` |
| **功能** | 触发 `FinanceMonthlyEtlJob.execute(statMonth)` |

**请求参数** — statMonth: String（yyyy-MM，空=上月）

**响应 `R<String>`**

---

### ET-05 营运月汇总 ETL

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/etl/operation` |
| **功能** | 触发 `OperationMonthlyEtlJob.execute(statMonth)` |

**请求参数** — statMonth: String（yyyy-MM，空=上月）

**响应 `R<String>`**

---

### ET-06 账龄分析 ETL

| 项目 | 说明 |
|------|------|
| **Method / Path** | `POST /rpt/etl/aging` |
| **功能** | 触发 `AgingAnalysisEtlJob.execute(statDate)` |

**请求参数** — statDate: String（yyyy-MM-dd，空=昨日）

**响应 `R<String>`**

---

## 40. 报表权限 — ReportPermissionController `/rpt/common`

> **端点前缀编码：** PM-

### PM-01 查询当前用户报表权限

| 项目 | 说明 |
|------|------|
| **Method / Path** | `GET /rpt/common/user-permissions` |
| **功能** | 返回用户可访问的报表模块、财务数据权限及可见项目范围 |
| **权限注解** | 无（本接口本身用于权限查询） |

**响应 `R<UserPermissionVO>`**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户ID |
| username | String | 用户名 |
| admin | boolean | 是否管理员 |
| hasFinViewPerm | boolean | 是否有财务绝对金额查看权限 |
| accessibleModules | List\<String\> | 可访问模块列表：ASSET/INV/OPR/FIN |
| permittedProjectIds | List\<Long\> | 可见项目ID列表（null=管理员可见全部） |

**业务规则**

- 资产/招商/营运模块始终可访问
- 财务模块需专属权限或管理员身份
- 管理员：permittedProjectIds = null（无限制）

---

## 报表管理 — ETL 聚合表字段定义

### rpt_asset_daily（资产日汇总表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| stat_date | LocalDate | 统计日期（ETL日期=T-1） |
| project_id | Long | 项目ID |
| building_id | Long | 楼栋ID（0=项目级汇总） |
| floor_id | Long | 楼层ID（0=楼栋级汇总） |
| format_type | String | 业态类型（空串=全业态汇总） |
| total_shops | Integer | 商铺总数 |
| rented_shops | Integer | 已租商铺数 |
| vacant_shops | Integer | 空置商铺数 |
| decorating_shops | Integer | 装修中商铺数 |
| opened_shops | Integer | 已开业商铺数 |
| total_area | BigDecimal | 总面积（㎡） |
| rented_area | BigDecimal | 已租面积（㎡） |
| vacant_area | BigDecimal | 空置面积（㎡） |
| decoration_area | BigDecimal | 装修中面积（㎡） |
| vacancy_rate | BigDecimal | 空置率（%）= vacant_area/total_area*100 |
| rental_rate | BigDecimal | 出租率（%）= rented_area/total_area*100 |
| opening_rate | BigDecimal | 开业率（%）= opened_shops/total_shops*100 |

### rpt_investment_daily（招商日汇总表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| stat_date | LocalDate | 统计日期 |
| project_id | Long | 项目ID |
| format_type | String | 业态类型（空串=全业态汇总） |
| investment_manager_id | Long | 招商负责人ID（0=全员汇总） |
| intention_count | Integer | 意向协议数（累计有效） |
| intention_signed | Integer | 已签意向数 |
| new_intention | Integer | 当日新增意向 |
| contract_count | Integer | 租赁合同数（累计有效） |
| contract_amount | BigDecimal | 合同总金额（元） |
| contract_area | BigDecimal | 签约面积（㎡） |
| new_contract | Integer | 当日新增合同 |
| conversion_rate | BigDecimal | 意向转化率（%） |
| avg_rent_price | BigDecimal | 平均租金单价（元/㎡/月） |

### rpt_operation_monthly（营运月汇总表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| stat_month | String | 统计月份（YYYY-MM） |
| project_id | Long | 项目ID |
| building_id | Long | 楼栋ID（0=项目级汇总） |
| format_type | String | 业态类型（空串=全业态汇总） |
| revenue_amount | BigDecimal | 月营收总额（元） |
| floating_rent_amount | BigDecimal | 浮动租金总额（元） |
| avg_revenue_per_sqm | BigDecimal | 坪效（元/㎡） |
| passenger_flow | Long | 月客流总量（人次） |
| avg_daily_passenger | Integer | 日均客流（人次） |
| change_count | Integer | 合同变更次数 |
| change_rent_impact | BigDecimal | 变更租金影响额（元） |
| expiring_contracts | Integer | 即将到期合同数（90天内） |
| terminated_contracts | Integer | 本月解约合同数 |

### rpt_finance_monthly（财务月汇总表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| stat_month | String | 统计月份（YYYY-MM） |
| project_id | Long | 项目ID |
| fee_item_id | Long | 费项ID（0=所有费项汇总） |
| fee_item_type | String | 费项类型 |
| receivable_amount | BigDecimal | 应收总额（元） |
| received_amount | BigDecimal | 已收总额（元） |
| outstanding_amount | BigDecimal | 欠款总额（元） |
| deduction_amount | BigDecimal | 减免总额（元） |
| adjustment_amount | BigDecimal | 调整总额（元） |
| overdue_amount | BigDecimal | 逾期总额（元） |
| overdue_rate | BigDecimal | 逾期率（%） |
| deposit_balance | BigDecimal | 保证金余额（月末快照） |
| prepay_balance | BigDecimal | 预收款余额（月末余额） |
| collection_rate | BigDecimal | 收缴率（%） |

### rpt_aging_analysis（账龄分析表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| stat_date | LocalDate | 统计日期 |
| project_id | Long | 项目ID |
| merchant_id | Long | 商家ID |
| contract_id | Long | 合同ID |
| fee_item_id | Long | 费项ID（0=所有费项汇总） |
| within_30 | BigDecimal | 30天内欠款（元） |
| days_31_60 | BigDecimal | 31-60天欠款（元） |
| days_61_90 | BigDecimal | 61-90天欠款（元） |
| days_91_180 | BigDecimal | 91-180天欠款（元） |
| days_181_365 | BigDecimal | 181-365天欠款（元） |
| over_365 | BigDecimal | 365天以上欠款（元） |
| total_outstanding | BigDecimal | 欠款合计（元） |

### rpt_generation_log（报表生成日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| log_code | String | 日志流水号（全局唯一） |
| report_id | Long | 报表ID（ad-hoc 时为 0） |
| task_id | Long | 定时任务ID（手动时为 null） |
| generation_type | String | MANUAL / SCHEDULE |
| triggered_by | Long | 触发人ID（自动为 0） |
| file_format | String | EXCEL / PDF |
| file_name | String | 文件名称 |
| file_path | String | 文件存储路径 |
| file_size | Long | 文件大小（字节） |
| file_md5 | String | MD5 校验值 |
| filter_params | String | 查询参数快照（JSON） |
| data_count | Integer | 导出数据条数 |
| status | Integer | 0=失败，1=成功，2=进行中 |
| error_msg | String | 错误信息 |
| duration_ms | Integer | 耗时（毫秒） |

### rpt_schedule_task（定时推送任务表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| task_code | String | 任务编码 |
| version | Integer | 版本号 |
| task_name | String | 任务名称 |
| report_id | Long | 报表配置ID |
| report_code | String | 报表编码 |
| cron_expression | String | Cron 表达式（6位Spring格式） |
| recipients | String | 收件人邮箱列表（JSON数组） |
| cc_recipients | String | 抄送人邮箱列表（JSON数组） |
| export_format | String | EXCEL / PDF |
| filter_params | String | 固定筛选参数（JSON） |
| last_run_time | LocalDateTime | 上次执行时间 |
| next_run_time | LocalDateTime | 下次执行时间 |
| run_count | Integer | 累计执行次数 |
| fail_count | Integer | 连续失败次数（≥3 自动禁用） |
| status | Integer | 0=禁用，1=启用 |

### rpt_user_favorite（用户报表收藏表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| user_id | Long | 用户ID |
| report_id | Long | 报表配置ID |
| report_code | String | 报表编码 |
| report_name | String | 报表名称 |
| route_path | String | 前端路由路径 |
| category | Integer | 分类：1=资产,2=招商,3=营运,4=财务 |
| sort_order | Integer | 收藏排序 |
| quick_access | Boolean | 是否快捷入口 |

> **通用审计字段（所有 rpt_* 表）：** created_by, created_at, updated_by, updated_at, is_deleted

---

## 报表管理 — 汇总统计

| 维度 | 数量 |
|------|------|
| 控制器 | 10 |
| 端点（Endpoint） | 62 |
| ETL 聚合表 | 5（rpt_asset_daily / rpt_investment_daily / rpt_operation_monthly / rpt_finance_monthly / rpt_aging_analysis） |
| 功能表 | 3（rpt_generation_log / rpt_schedule_task / rpt_user_favorite） |
| DTO | 6（ReportQueryParam / ExportTaskDTO / DrillDownRequestDTO / FavoriteAddDTO / FavoriteSortDTO / ScheduleTaskDTO） |
| VO | 41+（4个 Dashboard + 7个资产 + 7个招商 + 9个营运 + 10个财务 + 4个通用） |
| HTTP 方法分布 | GET: 43, POST: 12, PUT: 3, DELETE: 4 |
| 只读接口比例 | 69%（43 / 62）— 报表模块以查询为主 |

### 按控制器端点统计

| 控制器 | 端点数 |
|--------|--------|
| ReportAssetController | 10 |
| ReportInvestmentController | 8 |
| ReportOperationController | 10 |
| ReportFinanceController | 11 |
| ReportExportController | 4 |
| DrillDownController | 2 |
| FavoriteController | 4 |
| ScheduleTaskController | 6 |
| EtlTriggerController | 6 |
| ReportPermissionController | 1 |

> **注：** 全部 39 个报表查询接口（RA/RI/RO/FR）均使用 `@RptDataScope` 注解实现数据权限自动注入。财务类 2 个接口（FR-01/FR-02）额外实现 `FinanceDataMaskUtil` 绝对金额脱敏。ETL 聚合表采用 T+1 预计算策略，凭证统计（FR-08）为唯一实时查询业务表的报表接口。异步导出采用「提交→轮询→下载」三步模式，30分钟缓存防重复生成。定时推送连续失败 ≥3 次自动禁用。

---

## 报表管理 — 代码 vs 技术分析报告差异

| 差异点 | 技术报告设计 | 代码实际实现 |
|--------|------------|------------|
| **路径前缀** | `/api/v1/report/*` | `/rpt/*`（由 Vite 代理添加 `/api` 前缀） |
| **数据权限注解** | 报告设计 `@DataScope` | 代码使用独立的 `@RptDataScope` + `ReportDataPermissionAspect`（与系统管理 `@DataScope` 解耦） |
| **权限上下文** | 报告设计共用 SecurityContext | 代码使用独立的 `ReportPermissionContext`（ThreadLocal），permittedProjectIds 注入 ReportQueryParam |
| **财务脱敏** | 报告设计角色控制 | 代码实现 `FinanceDataMaskUtil.shouldMask()` 工具类，精确控制 dashboard/receivable-summary 两个接口 |
| **导出模式** | 报告设计同步下载 | 代码实现异步三步模式（提交→轮询→下载），支持 30 分钟缓存和断点复用 |
| **钻取接口** | 报告设计各报表独立钻取 | 代码统一为 DrillDownController，通过 reportCode 前缀判断域（ASSET_*/FIN_*） |
| **定时推送** | 报告设计 XXL-Job 任务 | 代码实现 `ScheduledReportJobHandler` 每分钟扫描 + Spring Mail 发送（XXL-Job 仅用于 ETL） |
| **定时任务自动禁用** | 报告未提及 | 代码实现连续失败 ≥3 次自动禁用（failCount 字段） |
| **ETL 触发** | 报告设计仅 XXL-Job 调度 | 代码增加 EtlTriggerController 手动 REST 触发（6 个 POST 端点），支持补跑历史数据 |
| **收藏表冗余** | 报告设计 JOIN rpt_config | 代码在 rpt_user_favorite 冗余 reportCode/reportName/routePath/category（避免 JOIN） |
| **GET 钻取** | 报告未提及 | 代码额外提供 GET 方式钻取接口（DD-02），适配 ECharts click 事件直接 URL 跳转 |
| **凭证统计** | 报告设计基于 ETL | 代码直接查询 fin_voucher 业务表（实时数据），未走 ETL 预计算 |

---

## 六、系统管理（asset-system，端口 8006）

> **认证接口路径前缀：** `/auth`
> **管理接口路径前缀：** `/sys/*`
> **Vite 代理规则：** `/api/auth/*` 和 `/api/sys/*` → `localhost:8001`（由 asset-base 转发至 asset-system，或直连 8006）
> **数据库：** `asset_db`（共用主库）
> **Knife4j 分组：** 00-认证管理 / 01-用户管理 / 02-机构管理 / 03-岗位管理 / 04-角色管理 / 05-菜单管理 / 06-业务字典 / 07-操作日志 / 07b-登录日志 / 08-编码规则管理 / 09-租费算法管理 / 09-分类管理 / 10-系统参数配置 / 11-在线用户管理

---

## 系统管理 — 状态枚举字典

### 用户状态 UserStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | DISABLED | 停用 |
| 1 | NORMAL | 正常 |

### 角色状态 RoleStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | DISABLED | 停用 |
| 1 | NORMAL | 正常 |

### 菜单类型 MenuType

| 值 | 枚举 | 含义 |
|----|------|------|
| M | DIRECTORY | 目录 |
| C | MENU | 菜单（页面） |
| F | BUTTON | 按钮/权限标识 |

### 部门状态 DeptStatus

| 值 | 枚举 | 含义 |
|----|------|------|
| 0 | DISABLED | 停用 |
| 1 | NORMAL | 正常 |

### 数据权限范围 DataScope

| 值 | 枚举 | 含义 |
|----|------|------|
| 1 | ALL | 全部数据（不限制） |
| 2 | CUSTOM | 自定义（按 sys_role_data 配置的部门列表） |
| 3 | DEPT | 本部门 |
| 4 | DEPT_AND_CHILD | 本部门及以下 |
| 5 | SELF | 仅本人创建的数据 |

### 登录日志状态

| 值 | 含义 |
|----|------|
| 0 | 成功 |
| 1 | 失败 |

### 操作日志状态

| 值 | 含义 |
|----|------|
| 0 | 成功 |
| 1 | 失败 |

### 编码重置周期 resetType

| 值 | 含义 |
|----|------|
| 0 | 不重置 |
| 1 | 按年重置 |
| 2 | 按月重置 |
| 3 | 按日重置 |

### 算法类型 algoType

| 值 | 含义 |
|----|------|
| 1 | 租金算法 |
| 2 | 保证金算法 |
| 3 | 服务费算法 |
| 4 | 其他 |

### 算法计算方式 calcMode

| 值 | 含义 |
|----|------|
| 1 | 固定金额 |
| 2 | 比率计算 |
| 3 | 阶梯计算 |
| 4 | 自定义公式 |

### 系统配置分组 configGroup

| 值 | 含义 |
|----|------|
| basic | 基础配置 |
| security | 安全配置 |
| upload | 上传配置 |
| other | 其他 |

---

## 系统管理 — 认证与安全机制

### JWT 双令牌模型

| 维度 | 说明 |
|------|------|
| **Access Token** | JWT（jjwt 0.12.6），有效期 30 分钟，Header `Authorization: Bearer <token>` |
| **Refresh Token** | UUID 字符串，存 Redis，有效期 7 天 |
| **刷新流程** | Access Token 过期后用 Refresh Token 换取新 Access Token，Refresh Token 不变 |
| **登出** | Access Token 加入 Redis 黑名单（TTL = 剩余有效时间），Refresh Token 从 Redis 删除 |
| **强制下线** | 删除目标用户全部 Refresh Token + 在线会话记录 |

### 登录失败锁定

| 维度 | 说明 |
|------|------|
| **最大次数** | 5 次 |
| **锁定时长** | 15 分钟（Redis TTL） |
| **解锁** | 登录成功后清除失败计数 |

### 密码安全

| 维度 | 说明 |
|------|------|
| **传输加密** | SM2 非对称加密（前端用公钥加密，后端用私钥解密） |
| **存储加密** | SM3 哈希（国密替代 BCrypt） |
| **敏感字段** | 手机号使用 SM4 对称加密存储 |

### 数据权限引擎（@DataScope）

| 维度 | 说明 |
|------|------|
| **注解** | `@DataScope`，标注在 Controller 方法上 |
| **切面** | `DataScopeAspect`（AOP），拦截带注解方法 |
| **上下文** | `DataScopeContext`（ThreadLocal），传递给 Service/Mapper 层 |
| **权限合并** | 多角色取并集（最大权限范围），SUPER_ADMIN 或 dataScope=1 直接跳过过滤 |
| **SQL 注入** | 按数据权限范围自动追加 WHERE 条件（部门 IN / created_by = 当前用户） |

---

## 41. 认证管理 — SysAuthController `/auth`

> **端点前缀编码：** SA-
> **认证要求：** SA-01、SA-02 为公开接口（白名单），其余需 JWT

### SA-01 获取 SM2 公钥

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/auth/publicKey` |
| **认证** | 否（白名单） |
| **响应** | `R<String>` — SM2 公钥十六进制字符串 |

---

### SA-02 用户登录

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/auth/login` |
| **认证** | 否（白名单） |

**请求体 LoginRequest：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | **是** | 用户名 |
| password | String | **是** | SM2 加密后的密码（前端用公钥加密） |

**响应 `R<LoginResult>`：**

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | String | JWT Access Token（30分钟有效） |
| refreshToken | String | Refresh Token（UUID，7天有效） |
| tokenType | String | 固定 `Bearer` |
| expiresIn | Long | Access Token 有效期（秒），值为 1800 |

> **业务规则：**
> 1. 连续登录失败 ≥5 次，账号锁定 15 分钟
> 2. SM2 解密失败时降级为明文匹配（开发调试兼容）
> 3. 登录成功后记录 sys_login_log、更新 last_login_ip/last_login_time、存储在线会话至 Redis

---

### SA-03 刷新 Access Token

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/auth/refresh` |
| **认证** | 否（白名单） |

**请求体 RefreshRequest：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | String | **是** | 刷新令牌 |

**响应 `R<LoginResult>`** — 结构同 SA-02，refreshToken 保持不变

> **业务规则：** Refresh Token 过期或无效返回错误，前端需跳转登录页。

---

### SA-04 用户登出

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/auth/logout` |
| **认证** | 是 |

**请求体 RefreshRequest（可选）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | String | 否 | 不传则删除该用户全部 Refresh Token |

> **业务规则：** 将当前 Access Token 的 jti 加入 Redis 黑名单（TTL = 剩余有效时间），删除 Refresh Token 和在线会话记录，记录登出日志。

---

### SA-05 获取当前用户信息

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/auth/info` 或 `/auth/userInfo` |
| **认证** | 是 |
| **请求参数** | 无（从 Token 解析 userId） |

**响应 `R<UserInfoVO>`：**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| username | String | 用户名 |
| realName | String | 真实姓名 |
| avatar | String | 头像 URL |
| deptId | Long | 部门 ID |
| deptName | String | 部门名称 |
| roles | List\<String\> | 角色编码列表（如 `["SUPER_ADMIN", "MANAGER"]`） |
| permissions | List\<String\> | 权限标识列表（超管返回 `["*:*:*"]`） |

---

### SA-06 获取当前用户动态路由

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/auth/user-routes` |
| **认证** | 是 |
| **请求参数** | 无 |

**响应 `R<List<RouteVO>>`：**

| 字段 | 类型 | 说明 |
|------|------|------|
| path | String | 路由路径 |
| name | String | 路由名称 |
| component | String | 前端组件路径 |
| meta | Map | 路由元信息（title、icon） |
| children | List\<RouteVO\> | 子路由（null 表示无子路由） |

> **业务规则：** 超管返回全部可见路由（menuType=M/C），非超管返回用户角色对应的菜单路由。

---

## 42. 用户管理 — SysUserController `/sys/users`

> **端点前缀编码：** SU-
> **数据权限：** SU-01 启用 `@DataScope` 注解

### SU-01 用户列表（分页）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/users` |
| **数据权限** | `@DataScope`（按部门过滤） |
| **响应** | `R<IPage<UserDetailVO>>` |

**查询参数 UserQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20 |
| username | String | 否 | 用户名（模糊查询） |
| realName | String | 否 | 真实姓名（模糊查询） |
| phone | String | 否 | 手机号 |
| status | Integer | 否 | 状态（0停用/1正常） |
| deptId | Long | 否 | 部门 ID |
| roleId | Long | 否 | 角色 ID |

---

### SU-02 用户详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/users/{id}` |
| **响应** | `R<UserDetailVO>` |

**响应字段 UserDetailVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户 ID |
| username | String | 用户名 |
| realName | String | 真实姓名 |
| deptId | Long | 部门 ID |
| deptName | String | 部门名称 |
| phone | String | 手机号 |
| email | String | 邮箱 |
| avatar | String | 头像 URL |
| status | Integer | 状态 |
| statusName | String | 状态名称 |
| loginIp | String | 最后登录 IP |
| loginTime | LocalDateTime | 最后登录时间 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |
| roleIds | List\<Long\> | 角色 ID 列表 |
| roleNames | List\<String\> | 角色名称列表 |
| postIds | List\<Long\> | 岗位 ID 列表 |
| postNames | List\<String\> | 岗位名称列表 |

---

### SU-03 新增用户

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/users` |
| **响应** | `R<Long>` — 新建用户 ID |

**请求体 UserCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| username | String | **是** | @NotBlank @Size(min=2, max=50) | 用户名 |
| password | String | 新增必填 | - | 密码（新增时必填，更新时不填则不修改） |
| realName | String | 否 | - | 真实姓名 |
| deptId | Long | 否 | - | 所属部门 ID |
| phone | String | 否 | - | 手机号 |
| email | String | 否 | - | 邮箱 |
| avatar | String | 否 | - | 头像 URL |
| status | Integer | 否 | - | 状态（0停用/1正常） |
| roleIds | List\<Long\> | 否 | - | 角色 ID 列表 |
| postIds | List\<Long\> | 否 | - | 岗位 ID 列表 |

> **业务规则：** 密码经 SM3 哈希后存储，手机号经 SM4 加密存储。用户名唯一校验。

---

### SU-04 更新用户

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/users/{id}` |
| **响应** | `R<Void>` |

**请求体** — 同 UserCreateDTO（password 不填则不修改密码）

---

### SU-05 删除用户

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/users/{id}` |
| **响应** | `R<Void>` |

> **业务规则：** 逻辑删除。不允许删除超级管理员。

---

### SU-06 重置密码（管理员操作）

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/users/reset-password` |
| **响应** | `R<Void>` |

**请求体 ResetPwdDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| userId | Long | **是** | @NotNull | 目标用户 ID |
| newPassword | String | **是** | @NotBlank | 新密码（SM3 哈希存储） |

---

### SU-07 修改用户状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/users/{id}/status` |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | **是** | 0=停用 / 1=正常 |

---

### SU-08 修改个人资料

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/users/profile` |
| **响应** | `R<Void>` |

**请求体 UserProfileDTO：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| realName | String | 否 | 真实姓名 |
| phone | String | 否 | 手机号 |
| email | String | 否 | 邮箱 |
| avatar | String | 否 | 头像 URL |

> **备注：** 从 JWT Token 解析当前用户 ID，仅能修改自己的资料。

---

### SU-09 修改自身密码

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/users/profile/password` |
| **响应** | `R<Void>` |

**请求体 ChangePasswordDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| oldPassword | String | **是** | @NotBlank | 原密码 |
| newPassword | String | **是** | @NotBlank | 新密码 |

> **业务规则：** 需验证原密码正确后才允许修改。

---

### SU-10 分配角色

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/users/{id}/roles` |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleIds | List\<Long\> | **是** | 角色 ID 列表（全量覆盖） |

---

### SU-11 分配岗位

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/users/{id}/posts` |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postIds | List\<Long\> | **是** | 岗位 ID 列表（全量覆盖） |

---

### SU-12 强制用户下线

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/users/{id}/token` |
| **响应** | `R<Void>` |

> **业务规则：** 删除目标用户的全部 Refresh Token 和在线会话记录。

---

## 43. 机构管理 — SysDeptController `/sys/depts`

> **端点前缀编码：** SD-
> **技术要点：** ancestors 路径枚举法，Redis 缓存机构树

### SD-01 部门树列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/depts` |
| **响应** | `R<List<DeptTreeVO>>` |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | 否 | 按状态过滤（不传返回全部） |

**响应字段 DeptTreeVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 部门 ID |
| parentId | Long | 父部门 ID（0=顶级） |
| deptName | String | 部门名称 |
| deptCode | String | 部门编码 |
| sortOrder | Integer | 排序 |
| leader | String | 负责人 |
| status | Integer | 状态 |
| children | List\<DeptTreeVO\> | 子部门列表 |

> **缓存：** 优先读 Redis 缓存，增删改后自动失效。

---

### SD-02 部门详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/depts/{id}` |
| **响应** | `R<SysDept>` |

---

### SD-03 新增部门

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/depts` |
| **响应** | `R<Long>` — 新建部门 ID |

**请求体 DeptCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| parentId | Long | 否 | - | 父部门 ID（0 或不填=顶级） |
| deptName | String | **是** | @NotBlank | 部门名称 |
| deptCode | String | 否 | - | 部门编码 |
| sortOrder | Integer | 否 | - | 排序 |
| leader | String | 否 | - | 负责人 |
| phone | String | 否 | - | 联系电话 |
| email | String | 否 | - | 邮箱 |
| status | Integer | 否 | - | 状态（0停用/1正常） |

> **业务规则：** 自动计算 ancestors 字段（如 `0,1,5`）。

---

### SD-04 更新部门

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/depts/{id}` |
| **响应** | `R<Void>` |

**请求体** — 同 DeptCreateDTO

> **业务规则：** 上级变更时自动更新所有后代节点的 ancestors 字段。

---

### SD-05 删除部门

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/depts/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 有子部门或关联用户时不允许删除。

---

### SD-06 修改部门状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/depts/{id}/status` |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | **是** | 0=停用 / 1=正常 |

---

### SD-07 移动子树

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/depts/{id}/move` |
| **响应** | `R<Void>` |

**请求体 MoveDeptDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| targetParentId | Long | **是** | @NotNull | 新的父部门 ID（0=提升为顶级） |

> **业务规则：** 批量更新当前节点及所有后代节点的 ancestors，同时失效 Redis 缓存。

---

### SD-08 查询机构下用户列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/depts/{id}/users` |
| **响应** | `R<List<SysUser>>` |

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| includeChildren | boolean | 否 | false | 是否包含子机构用户 |

---

### SD-09 清除机构树缓存

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/depts/cache` |
| **响应** | `R<Void>` |

---

## 44. 岗位管理 — SysPostController `/sys/posts`

> **端点前缀编码：** SP-

### SP-01 岗位列表（分页）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/posts` |
| **响应** | `R<IPage<SysPost>>` |

**查询参数 PostQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| postCode | String | 否 | 岗位编码 |
| postName | String | 否 | 岗位名称 |
| status | Integer | 否 | 状态 |

---

### SP-02 启用岗位列表（下拉/选择器）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/posts/list` 或 `/sys/posts/dropdown` |
| **响应** | `R<List<SysPost>>` — 仅返回 status=1 的岗位 |

---

### SP-03 新增岗位

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/posts` |
| **响应** | `R<Long>` — 新建岗位 ID |

**请求体 PostCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| postCode | String | **是** | @NotBlank | 岗位编码（唯一） |
| postName | String | **是** | @NotBlank | 岗位名称 |
| sortOrder | Integer | 否 | - | 排序 |
| status | Integer | 否 | - | 状态 |
| remark | String | 否 | - | 备注 |

---

### SP-04 更新岗位

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/posts/{id}` |
| **响应** | `R<Void>` |

---

### SP-05 删除岗位

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/posts/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 有关联用户时不允许删除。

---

### SP-06 修改岗位状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/posts/{id}/status` |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | **是** | 0=停用 / 1=正常 |

---

## 45. 角色管理 — SysRoleController `/sys/roles`

> **端点前缀编码：** SR-
> **核心能力：** 菜单权限分配 + 数据权限范围配置

### SR-01 角色列表（分页）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/roles` |
| **响应** | `R<IPage<SysRole>>` |

**查询参数 RoleQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| roleName | String | 否 | 角色名称 |
| roleCode | String | 否 | 角色编码 |
| status | Integer | 否 | 状态 |

---

### SR-02 启用角色列表（下拉用）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/roles/list` 或 `/sys/roles/dropdown` |
| **响应** | `R<List<SysRole>>` |

---

### SR-03 角色详情（含已分配菜单 ID）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/roles/{id}` |
| **响应** | `R<RoleDetailVO>` |

**响应字段 RoleDetailVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 角色 ID |
| roleName | String | 角色名称 |
| roleCode | String | 角色编码 |
| dataScope | Integer | 数据权限范围 |
| dataScopeName | String | 数据权限名称 |
| sortOrder | Integer | 排序 |
| status | Integer | 状态 |
| statusName | String | 状态名称 |
| remark | String | 备注 |
| createdAt | LocalDateTime | 创建时间 |
| menuIds | List\<Long\> | 已分配的菜单 ID 列表 |
| deptIds | List\<Long\> | 自定义数据权限部门 ID（dataScope=2 时有值） |

---

### SR-04 新增角色

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/roles` |
| **响应** | `R<Long>` — 新建角色 ID |

**请求体 RoleCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| roleName | String | **是** | @NotBlank | 角色名称 |
| roleCode | String | **是** | @NotBlank | 角色编码（唯一） |
| dataScope | Integer | 否 | - | 数据权限范围（1-5） |
| sortOrder | Integer | 否 | - | 排序 |
| status | Integer | 否 | - | 状态 |
| remark | String | 否 | - | 备注 |
| menuIds | List\<Long\> | 否 | - | 分配的菜单 ID 列表 |

---

### SR-05 更新角色

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/roles/{id}` |
| **响应** | `R<Void>` |

---

### SR-06 删除角色

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/roles/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 有关联用户时不允许删除。

---

### SR-07 修改角色状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/roles/{id}/status` |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | **是** | 0=停用 / 1=正常 |

---

### SR-08 分配菜单权限

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/roles/{id}/menus` |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| menuIds | List\<Long\> | **是** | 菜单 ID 列表（全量覆盖，含按钮权限） |

> **业务规则：** 先删除原有 sys_role_menu 记录，再批量插入新记录。

---

### SR-09 获取角色已分配菜单 ID

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/roles/{id}/menu-ids` |
| **响应** | `R<List<Long>>` |

---

### SR-10 设置数据权限范围

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/roles/{id}/data-scope` |
| **响应** | `R<Void>` |

**请求体 DataScopeDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| dataScope | Integer | **是** | @NotNull | 数据权限范围（1-5） |
| deptIds | List\<Long\> | 条件必填 | - | dataScope=2（自定义）时必填的部门 ID 列表 |

> **业务规则：** 更新 sys_role.data_scope 字段；dataScope=2 时同步更新 sys_role_data 表。

---

### SR-11 获取角色自定义数据权限部门 ID

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/roles/{id}/dept-ids` |
| **响应** | `R<List<Long>>` |

---

## 46. 菜单管理 — SysMenuController `/sys/menus`

> **端点前缀编码：** SM-
> **菜单类型：** M（目录）/ C（页面菜单）/ F（按钮/权限标识）

### SM-01 菜单树（全量）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/menus` |
| **响应** | `R<List<MenuTreeVO>>` |

**响应字段 MenuTreeVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 菜单 ID |
| parentId | Long | 父菜单 ID（0=顶级） |
| menuName | String | 菜单名称 |
| menuType | String | 类型：M 目录 / C 菜单 / F 按钮 |
| path | String | 路由地址 |
| component | String | 组件路径 |
| perms | String | 权限标识（如 `sys:user:list`） |
| icon | String | 图标 |
| sortOrder | Integer | 排序 |
| visible | Integer | 是否显示（0隐藏/1显示） |
| status | Integer | 状态（0停用/1正常） |
| children | List\<MenuTreeVO\> | 子菜单列表 |

---

### SM-02 菜单详情

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/menus/{id}` |
| **响应** | `R<SysMenu>` |

---

### SM-03 用户路由树

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/menus/routes` |
| **响应** | `R<List<MenuTreeVO>>` |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | **是** | 用户 ID |

---

### SM-04 用户权限标识列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/menus/perms` |
| **响应** | `R<List<String>>` — 权限标识列表（如 `["sys:user:list", "sys:user:add"]`） |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | Long | **是** | 用户 ID |

---

### SM-05 新增菜单

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/menus` |
| **响应** | `R<Long>` — 新建菜单 ID |

**请求体 MenuCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| parentId | Long | 否 | - | 父菜单 ID（0 或不填=顶级） |
| menuName | String | **是** | @NotBlank | 菜单名称 |
| menuType | String | **是** | @NotBlank | M 目录 / C 菜单 / F 按钮 |
| path | String | 否 | - | 路由地址（M/C 必填） |
| component | String | 否 | - | 组件路径（C 必填） |
| perms | String | 否 | - | 权限标识（F 必填） |
| icon | String | 否 | - | 图标 |
| sortOrder | Integer | 否 | - | 排序 |
| visible | Integer | 否 | - | 是否显示 |
| status | Integer | 否 | - | 状态 |
| remark | String | 否 | - | 备注 |

---

### SM-06 更新菜单

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/menus/{id}` |
| **响应** | `R<Void>` |

---

### SM-07 删除菜单

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/menus/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 有子菜单或角色绑定时不允许删除。

---

### SM-08 修改菜单状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/menus/{id}/status` |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | Integer | **是** | 0=停用 / 1=正常 |

---

### SM-09 修改菜单显示状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/menus/{id}/visible` |
| **响应** | `R<Void>` |

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| visible | Integer | **是** | 0=隐藏 / 1=显示 |

---

## 47. 业务字典管理 — SysDictController `/sys/dict`

> **端点前缀编码：** DC-
> **缓存策略：** Redis key=`sys:dict:{type}`，TTL 60 分钟；增删改自动失效

### DC-01 字典类型列表（分页）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/dict/types` |
| **响应** | `R<IPage<SysDictType>>` |

**查询参数 DictQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dictName | String | 否 | 字典名称（模糊） |
| dictType | String | 否 | 字典类型标识 |
| status | Integer | 否 | 状态 |

---

### DC-02 新增字典类型

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/dict/types` |
| **响应** | `R<Long>` |

**请求体 DictTypeCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| dictName | String | **是** | @NotBlank | 字典名称 |
| dictType | String | **是** | @NotBlank | 字典类型标识（唯一） |
| status | Integer | 否 | - | 状态 |
| remark | String | 否 | - | 备注 |

---

### DC-03 更新字典类型

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/dict/types/{id}` |
| **响应** | `R<Void>` |

---

### DC-04 删除字典类型

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/dict/types/{id}` |
| **响应** | `R<Void>` |

> **业务规则：** 级联删除该类型下所有字典数据，同时清除 Redis 缓存。

---

### DC-05 修改字典类型状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/dict/types/{id}/status` |
| **响应** | `R<Void>` |

---

### DC-06 获取启用字典数据（业务下拉，走缓存）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/dict/data/{dictType}` |
| **响应** | `R<List<SysDictData>>` — 仅返回 status=1 的数据 |

> **缓存：** 优先读 Redis `sys:dict:{dictType}`，缓存未命中时查库并回填。

---

### DC-07 获取全部字典数据（含停用，管理界面用）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/dict/data/{dictType}/all` |
| **响应** | `R<List<SysDictData>>` |

---

### DC-08 新增字典数据

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/dict/data` |
| **响应** | `R<Long>` |

**请求体 DictDataCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| dictType | String | **是** | @NotBlank | 所属字典类型标识 |
| dictLabel | String | **是** | @NotBlank | 字典标签（显示文本） |
| dictValue | String | **是** | @NotBlank | 字典值 |
| cssClass | String | 否 | - | 样式属性（el-tag type） |
| sortOrder | Integer | 否 | - | 排序 |
| status | Integer | 否 | - | 状态 |
| remark | String | 否 | - | 备注 |

---

### DC-09 更新字典数据

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/dict/data/{id}` |
| **响应** | `R<Void>` |

---

### DC-10 删除字典数据

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/dict/data/{id}` |
| **响应** | `R<Void>` |

---

### DC-11 修改字典数据状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/dict/data/{id}/status` |
| **响应** | `R<Void>` |

---

### DC-12 刷新字典缓存

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/dict/cache/{dictType}` |
| **响应** | `R<Void>` |

> **业务规则：** 删除 Redis `sys:dict:{dictType}` 缓存，下次查询时自动重新加载。

---

## 48. 操作日志管理 — SysOperLogController `/sys/logs`

> **端点前缀编码：** OL-
> **采集机制：** `@OperLog` 自定义注解 + AOP 切面异步写入

### OL-01 操作日志列表（分页）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/logs` |
| **响应** | `R<IPage<SysOperLog>>` |

**查询参数 OperLogQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| module | String | 否 | 模块名称 |
| operUser | String | 否 | 操作人用户名 |
| status | Integer | 否 | 状态（0成功/1失败） |
| timeFrom | LocalDateTime | 否 | 查询开始时间 |
| timeTo | LocalDateTime | 否 | 查询结束时间 |

**SysOperLog 实体字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| module | String | 模块名称 |
| bizType | String | 业务类型 |
| method | String | 请求方法（类.方法名） |
| requestMethod | String | HTTP 方法（GET/POST/PUT/DELETE） |
| requestUrl | String | 请求 URL |
| requestParam | String | 请求参数（脱敏后） |
| responseResult | String | 响应结果摘要 |
| operUser | String | 操作人用户名 |
| operIp | String | 操作人 IP |
| status | Integer | 状态（0成功/1失败） |
| errorMsg | String | 错误消息（失败时） |
| costTime | Long | 耗时（毫秒） |
| operTime | LocalDateTime | 操作时间 |

---

### OL-02 清空操作日志

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/logs/clear` |
| **响应** | `R<Void>` |

---

## 49. 登录日志管理 — SysLoginLogController `/sys/login-logs`

> **端点前缀编码：** LL-

### LL-01 登录日志列表（分页）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/login-logs` |
| **响应** | `R<IPage<SysLoginLog>>` |

**查询参数 LoginLogQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 否 | 用户名（模糊查询） |
| ipAddr | String | 否 | IP 地址（模糊查询） |
| status | Integer | 否 | 状态（0成功/1失败） |
| timeFrom | LocalDateTime | 否 | 查询开始时间 |
| timeTo | LocalDateTime | 否 | 查询结束时间 |

**SysLoginLog 实体字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键 |
| username | String | 用户名 |
| ipAddr | String | IP 地址 |
| browser | String | 浏览器 |
| os | String | 操作系统 |
| status | Integer | 状态（0成功/1失败） |
| msg | String | 消息/失败原因 |
| loginTime | LocalDateTime | 登录时间 |

---

### LL-02 清空登录日志

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/login-logs/clear` |
| **响应** | `R<Void>` |

---

## 50. 在线用户管理 — SysOnlineUserController `/sys/online-users`

> **端点前缀编码：** OU-
> **实现方式：** 扫描 Redis 中的在线会话记录

### OU-01 查询在线用户列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/online-users` |
| **响应** | `R<List<OnlineUserVO>>` |

**响应字段 OnlineUserVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| username | String | 用户名 |
| loginIp | String | 登录 IP |
| loginTime | String | 登录时间（ISO 字符串） |

---

### OU-02 强制下线指定用户

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/online-users/{userId}` |
| **响应** | `R<Void>` |

> **业务规则：** 删除目标用户的 Redis 在线会话和全部 Refresh Token。有 `@OperLog` 操作日志记录。

---

## 51. 编码规则管理 — SysCodeRuleController `/sys/code-rules`

> **端点前缀编码：** CR-
> **并发安全：** Redis INCR 原子递增序列号

### CR-01 分页查询编码规则

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/code-rules` |
| **响应** | `R<IPage<SysCodeRule>>` |

**查询参数 CodeRuleQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ruleName | String | 否 | 规则名称（模糊） |
| ruleKey | String | 否 | 规则标识（模糊） |
| status | Integer | 否 | 状态 |

---

### CR-02 新增编码规则

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/code-rules` |
| **响应** | `R<Long>` |

**请求体 CodeRuleCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| ruleKey | String | **是** | @NotBlank | 规则标识（唯一，如 `project` / `contract`） |
| ruleName | String | **是** | @NotBlank | 规则名称 |
| prefix | String | 否 | - | 编码前缀（如 `PRJ`） |
| dateFormat | String | 否 | - | 日期格式（如 `yyyyMM`，留空不拼日期） |
| sep | String | 否 | - | 分隔符（如 `-`） |
| seqLength | Integer | **是** | @NotNull @Min(1) | 序列号位数（前补零） |
| resetType | Integer | **是** | @NotNull | 重置周期（0不重置/1按年/2按月/3按日） |
| status | Integer | 否 | - | 状态 |
| remark | String | 否 | - | 备注 |

> **编码示例：** prefix=`PRJ`、dateFormat=`yyyyMM`、sep=`-`、seqLength=4 → 生成 `PRJ-202603-0001`

---

### CR-03 更新编码规则

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/code-rules/{id}` |
| **响应** | `R<Void>` |

---

### CR-04 删除编码规则

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/code-rules/{id}` |
| **响应** | `R<Void>` |

---

### CR-05 修改编码规则状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/code-rules/{id}/status` |
| **响应** | `R<Void>` |

---

### CR-06 重置序列号

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/code-rules/{id}/reset-seq` |
| **响应** | `R<Void>` |

> **业务规则：** 将当前序列号归零，同时清除 Redis 中对应的计数器。

---

### CR-07 生成下一个业务编码（预览）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/code-rules/generate/{ruleKey}` |
| **响应** | `R<String>` — 生成的编码字符串（如 `PRJ-202603-0001`） |

> **业务规则：** Redis INCR 原子递增序列号，按 resetType 判断是否需要重置周期。

---

## 52. 租费算法管理 — SysFeeAlgorithmController `/sys/fee-algorithms`

> **端点前缀编码：** FA-
> **技术要点：** 公式引擎解析，变量替换，服务端试算

### FA-01 分页查询算法列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/fee-algorithms` |
| **响应** | `R<IPage<SysFeeAlgorithm>>` |

**查询参数 FeeAlgorithmQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| algoName | String | 否 | 算法名称（模糊） |
| algoType | Integer | 否 | 算法类型（1/2/3/4） |
| calcMode | Integer | 否 | 计算方式（1/2/3/4） |
| status | Integer | 否 | 状态 |

---

### FA-02 启用算法列表（下拉用）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/fee-algorithms/enabled` |
| **响应** | `R<List<SysFeeAlgorithm>>` |

---

### FA-03 新增算法

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/fee-algorithms` |
| **响应** | `R<Long>` |

**请求体 FeeAlgorithmCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| algoCode | String | **是** | @NotBlank | 算法编码（唯一） |
| algoName | String | **是** | @NotBlank | 算法名称 |
| algoType | Integer | **是** | @NotNull | 算法类型（1租金/2保证金/3服务费/4其他） |
| calcMode | Integer | **是** | @NotNull | 计算方式（1固定金额/2比率/3阶梯/4自定义公式） |
| formula | String | **是** | @NotBlank | 计算公式（变量用英文名，如 `area * unitPrice * months`） |
| variables | JsonNode | 否 | - | 变量定义列表（JSON Array，如 `[{"key":"area","name":"面积","unit":"㎡"}]`） |
| params | JsonNode | 否 | - | 固定参数（JSON Object） |
| description | String | 否 | - | 算法说明 |
| status | Integer | 否 | - | 状态 |

---

### FA-04 更新算法

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/fee-algorithms/{id}` |
| **响应** | `R<Void>` |

---

### FA-05 删除算法

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/fee-algorithms/{id}` |
| **响应** | `R<Void>` |

---

### FA-06 启用/停用算法

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/fee-algorithms/{id}/status` |
| **响应** | `R<Void>` |

---

### FA-07 服务端试算

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/fee-algorithms/test-calc` |
| **响应** | `R<CalcTestResultVO>` |

**请求体 CalcTestDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| algoId | Long | **是** | @NotNull | 目标算法 ID |
| inputs | Map\<String, String\> | **是** | @NotNull | 变量输入值映射（如 `{"area":"100","unitPrice":"50","months":"12"}`） |

**响应 CalcTestResultVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| result | String | 最终计算结果（保留 2 位小数） |
| expandedFormula | String | 展开后的公式（变量替换为实际值） |
| detail | String | 计算过程说明 |

---

## 53. 分类管理 — SysCategoryController `/sys/categories`

> **端点前缀编码：** CT-
> **用途：** 业态分类、品牌分类等多维分类树

### CT-01 查询分类树（按维度）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/categories/tree` |
| **响应** | `R<List<CategoryTreeVO>>` |

**查询参数 CategoryQueryDTO：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryType | String | **是** | 分类维度标识（如 `format_type` 业态 / `brand_type` 品牌） |
| categoryName | String | 否 | 分类名称（模糊） |
| status | Integer | 否 | 状态 |

**响应字段 CategoryTreeVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 分类 ID |
| parentId | Long | 父节点 ID |
| categoryCode | String | 分类编码 |
| categoryName | String | 分类名称 |
| sortOrder | Integer | 排序 |
| status | Integer | 状态 |
| children | List\<CategoryTreeVO\> | 子节点列表 |

---

### CT-02 获取所有分类维度

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/categories/types` |
| **响应** | `R<List<String>>` — 所有分类维度标识 |

---

### CT-03 新增分类节点

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/categories` |
| **响应** | `R<Long>` |

**请求体 CategoryCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| categoryType | String | **是** | 分类维度标识 |
| parentId | Long | 否 | 父节点 ID（0=顶级） |
| categoryCode | String | 否 | 分类编码 |
| categoryName | String | **是** | 分类名称 |
| sortOrder | Integer | 否 | 排序 |
| status | Integer | 否 | 状态 |
| remark | String | 否 | 备注 |

---

### CT-04 更新分类节点

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/categories/{id}` |
| **响应** | `R<Void>` |

---

### CT-05 删除分类节点

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/categories/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 仅允许删除叶子节点（无子节点）。

---

### CT-06 修改分类状态

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/categories/{id}/status` |
| **响应** | `R<Void>` |

---

## 54. 系统参数配置 — SysConfigController `/sys/configs`

> **端点前缀编码：** SC-
> **缓存策略：** Redis key=`sys:config:{key}`，增删改自动失效
> **操作日志：** 新增/更新/删除/刷新缓存操作均有 `@OperLog` 记录

### SC-01 分页查询参数列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/configs` |
| **响应** | `R<IPage<SysConfig>>` |

**查询参数 SysConfigQueryDTO（继承 PageQuery）：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| configKey | String | 否 | 配置键（模糊） |
| configName | String | 否 | 配置名称（模糊） |
| configGroup | String | 否 | 分组（basic/security/upload/other） |

---

### SC-02 按分组查询参数列表

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/configs/group/{group}` |
| **响应** | `R<List<SysConfig>>` |

---

### SC-03 按键查询参数值（走缓存）

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/configs/key/{key}` |
| **响应** | `R<String>` — 参数值；key 不存在时返回 404 |

---

### SC-04 新增参数

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/configs` |
| **响应** | `R<Long>` |

**请求体 SysConfigCreateDTO（@Valid）：**

| 字段 | 类型 | 必填 | 校验 | 说明 |
|------|------|------|------|------|
| configKey | String | **是** | @NotBlank | 配置键（唯一） |
| configName | String | **是** | @NotBlank | 配置名称 |
| configValue | String | **是** | @NotBlank | 配置值 |
| configGroup | String | 否 | - | 分组 |
| description | String | 否 | - | 说明 |
| isBuiltIn | Integer | 否 | - | 是否内置（1=是，不可删除） |

---

### SC-05 更新参数

| 项 | 值 |
|---|---|
| **方法** | `PUT` |
| **路径** | `/sys/configs/{id}` |
| **响应** | `R<Void>` |

---

### SC-06 删除参数

| 项 | 值 |
|---|---|
| **方法** | `DELETE` |
| **路径** | `/sys/configs/{id}` |
| **响应** | `R<Void>` |

> **业务约束：** 内置参数（isBuiltIn=1）禁止删除。

---

### SC-07 刷新所有参数缓存

| 项 | 值 |
|---|---|
| **方法** | `POST` |
| **路径** | `/sys/configs/refresh` |
| **响应** | `R<Void>` |

---

## 55. 数据权限调试 — DataScopeController `/sys/data-scope`

> **端点前缀编码：** DS-
> **用途：** 调试/运维接口，查看当前用户的数据权限解析结果

### DS-01 查询当前用户数据权限信息

| 项 | 值 |
|---|---|
| **方法** | `GET` |
| **路径** | `/sys/data-scope/current` |
| **响应** | `R<DataScopeVO>` |

**响应字段 DataScopeVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| deptId | Long | 用户所属部门 ID |
| admin | boolean | 是否管理员（无数据限制） |
| selfOnly | boolean | 是否仅本人数据 |
| effectiveDeptIds | List\<Long\> | 有效部门 ID 列表（多角色取并集） |
| effectiveDeptNames | List\<String\> | 有效部门名称列表 |
| roles | List\<RoleScopeVO\> | 各角色的权限范围明细 |

**RoleScopeVO：**

| 字段 | 类型 | 说明 |
|------|------|------|
| roleId | Long | 角色 ID |
| roleCode | String | 角色编码 |
| roleName | String | 角色名称 |
| dataScope | Integer | 数据权限范围值 |
| dataScopeName | String | 数据权限范围名称 |
| customDeptIds | List\<Long\> | 自定义部门 ID（dataScope=2 时） |

---

## 系统管理 — 数据表清单

### 用户与组织

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| sys_user | 系统用户 | username, password(SM3), real_name, dept_id, phone(SM4), email, avatar, status |
| sys_dept | 部门/机构 | parent_id, ancestors, dept_name, dept_code, sort_order, leader, phone, email, status |
| sys_post | 岗位 | post_code, post_name, sort_order, status |
| sys_user_role | 用户-角色关联 | user_id, role_id |
| sys_user_post | 用户-岗位关联 | user_id, post_id |

### 角色与权限

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| sys_role | 角色 | role_name, role_code, data_scope(1-5), sort_order, status |
| sys_menu | 菜单/按钮 | parent_id, menu_name, menu_type(M/C/F), path, component, perms, icon, sort_order, visible, status |
| sys_role_menu | 角色-菜单关联 | role_id, menu_id |
| sys_role_data | 自定义数据权限 | role_id, dept_id（dataScope=2 时的自定义部门列表） |

### 业务配置

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| sys_dict_type | 字典类型 | dict_name, dict_type, status |
| sys_dict_data | 字典数据 | dict_type, dict_label, dict_value, css_class, sort_order, status |
| sys_category | 分类管理 | category_type, parent_id, category_code, category_name, sort_order, status |
| sys_code_rule | 编码规则 | rule_key, rule_name, prefix, date_format, sep, seq_length, reset_type, current_seq, current_period, status |
| sys_fee_algorithm | 租费算法 | algo_code, algo_name, algo_type, calc_mode, formula, variables(JSON), params(JSON), status |
| sys_config | 系统参数 | config_key, config_name, config_value, config_group, is_built_in |

### 日志

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| sys_oper_log | 操作日志 | module, biz_type, method, request_url, oper_user, status, cost_time, oper_time |
| sys_login_log | 登录日志 | username, ip_addr, status, msg, login_time |

> **通用审计字段（大部分 sys_* 表）：** created_by, created_at, updated_by, updated_at, is_deleted

---

## 系统管理 — 汇总统计

| 维度 | 数量 |
|------|------|
| 控制器 | 15（含 DataScopeController 调试接口） |
| 端点（Endpoint） | 91 |
| 数据表 | 17 |
| DTO（含 Query/Create/VO） | 35 |
| 枚举 | 5（UserStatus / RoleStatus / MenuType / DeptStatus / DataScope） |
| HTTP 方法分布 | GET: 36, POST: 14, PUT: 26, DELETE: 15 |
| 需认证接口 | 88（白名单仅 SA-01/SA-02/SA-03 共 3 个） |

### 按控制器端点统计

| 控制器 | 路径前缀 | 端点数 |
|--------|----------|--------|
| SysAuthController | /auth | 6 |
| SysUserController | /sys/users | 12 |
| SysDeptController | /sys/depts | 9 |
| SysPostController | /sys/posts | 6 |
| SysRoleController | /sys/roles | 11 |
| SysMenuController | /sys/menus | 9 |
| SysDictController | /sys/dict | 12 |
| SysOperLogController | /sys/logs | 2 |
| SysLoginLogController | /sys/login-logs | 2 |
| SysOnlineUserController | /sys/online-users | 2 |
| SysCodeRuleController | /sys/code-rules | 7 |
| SysFeeAlgorithmController | /sys/fee-algorithms | 7 |
| SysCategoryController | /sys/categories | 6 |
| SysConfigController | /sys/configs | 7 |
| DataScopeController | /sys/data-scope | 1 |

> **注：** 认证模块（SA-01 ~ SA-06）中 SA-01/SA-02/SA-03 为公开白名单接口，其余需 JWT 认证。用户管理 SU-01 启用 `@DataScope` 数据权限过滤。字典数据 DC-06 走 Redis 缓存（key=`sys:dict:{type}`，TTL 60 分钟）。系统参数 SC-03 走 Redis 缓存（key=`sys:config:{key}`）。编码生成 CR-07 使用 Redis INCR 保证并发安全。租费算法 FA-07 支持服务端试算（公式解析 + 变量替换）。

---

## 系统管理 — 代码 vs 技术分析报告差异

| 差异点 | 技术报告设计 | 代码实际实现 |
|--------|------------|------------|
| **路径前缀** | `/api/v1/system/*` 和 `/api/v1/auth/*` | `/sys/*` 和 `/auth/*`（由 Vite 代理添加 `/api` 前缀） |
| **机构表名** | 报告设计 `sys_org` | 代码使用 `sys_dept`（Entity 为 SysDept，控制器路径 `/sys/depts`） |
| **密码加密** | 报告设计 BCrypt | 代码使用 SM3 哈希（国密替代方案） |
| **JWT 模型** | 报告设计双 Token（Access + Refresh） | 代码完整实现：Access Token 30min + Refresh Token 7天 Redis 存储 |
| **数据权限注解** | 报告设计 `@DataScope` 在 Service 层 | 代码 `@DataScope` 标注在 Controller 方法上，通过 `DataScopeContext`（ThreadLocal）传递 |
| **数据权限实现** | 报告设计 MyBatis 拦截器注入 SQL | 代码使用 AOP 切面 + ThreadLocal 上下文 + Service 层手动过滤（非 SQL 自动注入） |
| **在线用户** | 报告设计扫描 Redis Token | 代码使用独立 Redis Hash 存储在线会话（login 时写入，logout 时清除） |
| **字典缓存同步** | 报告设计 Redis Pub/Sub 多节点同步 | 代码使用简单 Redis 缓存 + 手动刷新接口（DC-12），未实现 Pub/Sub |
| **租费算法** | 报告设计 8 种策略模式 + Aviator/SpEL 引擎 | 代码实现通用公式引擎（变量替换 + 表达式解析），未区分 8 种策略类 |
| **分类管理** | 报告设计 `sys_category` 含 ancestors | 代码实现为简单父子树（parent_id），无 ancestors 字段 |
| **编码生成** | 报告设计 Redis INCR + SELECT FOR UPDATE 兜底 | 代码使用 Redis INCR 原子递增，无数据库兜底 |
| **操作日志** | 报告设计 `@OperLog` 全接口覆盖 | 代码仅在部分重要接口标注 `@OperLog`（系统配置、在线用户管理等） |
| **登录失败锁定** | 报告设计 sys_user.login_fail_count + lock_time 字段 | 代码使用 Redis 计数器（TTL 15 分钟自动过期），不持久化到数据库 |
| **DataScope 调试** | 报告未提及 | 代码新增 DataScopeController（DS-01），方便运维查看当前用户权限解析结果 |
