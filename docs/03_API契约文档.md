# 基础数据管理模块 — REST API 契约文档

> **生成日期：** 2026-03-10
> **数据来源：** `asset-base` 模块源码 + `docs/基础数据管理模块_技术分析报告.docx`
> **服务端口：** 8001
> **认证方式：** JWT Token（Header: `Authorization: Bearer <token>`）

---

## 目录

- [统一响应格式](#统一响应格式)
- [通用分页参数](#通用分页参数)
- [错误码](#错误码)
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

## 汇总统计

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

## 代码 vs 技术分析报告差异

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
