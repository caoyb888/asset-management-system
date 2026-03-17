# 前端 TypeScript 错误修复计划

> 文档创建：2026-03-17
> 错误总数：30 处
> 涉及文件：11 个
> 优先级：P1（阻断 `vue-tsc` 严格模式编译，当前用 `vite build` 绕过）

---

## 一、根本原因分析

所有 30 处错误均源于**同一个根因**：

`src/api/request.ts` 的响应拦截器在运行时将 `ApiResponse<T>` 解包为 `T`，但 TypeScript 编译器不感知这个运行时行为——它仍然认为 `request.get<T>()` 的返回类型是 `Promise<AxiosResponse<ApiResponse<T>, any>>`。

```
运行时：  request.get<UserPermissionVO>('/rpt/...')  →  Promise<UserPermissionVO>       ✅
TS 视角：request.get<UserPermissionVO>('/rpt/...')  →  Promise<AxiosResponse<...>>     ❌
```

导致两类症状：

| 症状 | 典型报错 |
|------|---------|
| 访问解包后的业务字段（`.records`、`.total`、`.hasFinViewPerm` 等）| `Property 'records' does not exist on type 'AxiosResponse<any>'` |
| 将 API 返回值直接赋给具体 VO 类型变量 | `Type 'AxiosResponse<ChangeDetailVO>' is not assignable to type 'ChangeDetailVO'` |

---

## 二、错误分类清单

### 类型 A：`request.ts` 内部类型错误（2 处）

| 文件 | 行号 | 错误描述 |
|------|------|---------|
| `src/api/request.ts` | 62 | `result.accessToken` 为 `string \| undefined`，`setToken()` 要求 `string` |
| `src/api/request.ts` | 64 | `pendingQueue.forEach(cb => cb(newToken))` 中 `newToken` 可能为 `undefined` |

**根因：** `refreshToken(rt)` 的返回值在运行时已是解包后的数据对象，但 TS 认为它是 `AxiosResponse`，`result.accessToken` 因而推断为 `undefined`。

---

### 类型 B：`BasicLayout.vue` 权限字段访问错误（1 处）

| 文件 | 行号 | 错误描述 |
|------|------|---------|
| `src/layouts/BasicLayout.vue` | 395 | `data.hasFinViewPerm` 不存在于 `AxiosResponse<UserPermissionVO>` |

**根因：** `getUserPermissions()` 未声明正确的解包后返回类型，`data` 被 TS 识别为 `AxiosResponse` 而非 `UserPermissionVO`。

---

### 类型 C：opr 模块分页数据访问错误（14 处）

直接访问 `res.records` / `res.total` 等解包后字段，TS 不认识。

| 文件 | 行号 | 字段 |
|------|------|------|
| `src/views/opr/alerts/contract-expiry.vue` | 188, 189 | `.records`, `.total` |
| `src/views/opr/change/index.vue` | 181, 182 | `.records`, `.total` |
| `src/views/opr/change/form.vue` | 369 | `.records` |
| `src/views/opr/flow/index.vue` | 299, 307, 315, 323, 331 | `.records` (×5) |
| `src/views/opr/flow/index.vue` | 348, 349 | `.records`, `.total` |
| `src/views/opr/flow/index.vue` | 359 | `.trendPoints` |
| `src/views/opr/flow/index.vue` | 521, 522, 523 | `.successCount`, `.failCount`, `.errorList` |
| `src/views/opr/ledger/index.vue` | 245, 246 | `.records`, `.total` |
| `src/views/opr/termination/index.vue` | 157, 158 | `.records`, `.total` |
| `src/views/opr/termination/form.vue` | 215 | `.records` |

---

### 类型 D：opr 模块 VO 赋值类型不匹配（12 处）

将 API 返回值赋给具体 VO 类型的响应式变量时报错。

| 文件 | 行号 | 错误描述 |
|------|------|---------|
| `src/views/opr/change/detail.vue` | 179 | `AxiosResponse<ChangeDetailVO>` → `ChangeDetailVO` |
| `src/views/opr/change/form.vue` | 413 | `AxiosResponse<number>` → `number` |
| `src/views/opr/change/form.vue` | 426 | `AxiosResponse<ChangeImpactVO>` → `ChangeImpactVO` |
| `src/views/opr/ledger/detail.vue` | 504 | `AxiosResponse<ChangeDetailVO[]>` → `ChangeDetailVO[]` |
| `src/views/opr/termination/detail.vue` | 168 | `AxiosResponse<any>` → `TerminationDetailVO` |
| `src/views/opr/termination/form.vue` | 259 | `AxiosResponse<any>` → `number` |
| `src/views/opr/termination/form.vue` | 285 | `AxiosResponse<any>` → `TerminationDetailVO` |
| `src/views/opr/termination/form.vue` | 286 | `.settlements` 不存在于 `AxiosResponse` |
| `src/views/opr/termination/form.vue` | 315 | `AxiosResponse` → `TerminationDetailVO` 类型断言失败 |
| `src/views/opr/flow/index.vue` | 538 | `AxiosResponse` → `Blob` 类型断言失败 |

---

## 三、修复方案

### 方案选择

| 方案 | 描述 | 优点 | 缺点 |
|------|------|------|------|
| **方案 A（推荐）** | 在 `request.ts` 中导出类型安全的包装函数 `http`，让 TS 感知解包行为 | 一次修复，所有 API 文件受益；类型自动推断 | 需同步更新所有 API 文件的导入 |
| 方案 B | 在每个 API 函数上手动加 `: Promise<T>` 返回类型注解 | 改动范围小，可逐文件处理 | 重复劳动多，约 30+ 个函数需改 |
| 方案 C | 在 Vue 文件中用 `as any` / `as T` 强制断言 | 改动最少 | 掩盖问题，丧失类型保护，不推荐 |

---

### 方案 A 详细步骤（推荐）

#### Step 1：在 `request.ts` 中导出 `http` 包装对象

在文件末尾 `export default request` 之前添加：

```ts
/**
 * 类型安全的请求包装器
 * 响应拦截器已将 ApiResponse<T> 解包为 T，此处通过类型声明让 TS 感知该行为。
 * 所有 API 文件应使用 http 而非直接使用 request。
 */
export const http = {
  get<T = unknown>(url: string, config?: object): Promise<T> {
    return request.get(url, config) as unknown as Promise<T>
  },
  post<T = unknown>(url: string, data?: unknown, config?: object): Promise<T> {
    return request.post(url, data, config) as unknown as Promise<T>
  },
  put<T = unknown>(url: string, data?: unknown, config?: object): Promise<T> {
    return request.put(url, data, config) as unknown as Promise<T>
  },
  delete<T = unknown>(url: string, config?: object): Promise<T> {
    return request.delete(url, config) as unknown as Promise<T>
  },
}
```

> **注意**：`request` 本身（`export default`）保留不动，供 `request.ts` 内部自引用（Token 刷新重试逻辑）继续使用。

---

#### Step 2：修复 `request.ts` 内部报错（类型 A）

将刷新 Token 逻辑中的 `result` 类型明确：

```ts
// 修改前
const result = await refreshToken(rt)
const newToken = result.accessToken           // TS 报错

// 修改后：refreshToken 改用 http.post 或手动断言
const result = await refreshToken(rt) as { accessToken?: string; token?: string }
const newToken = result.token || result.accessToken || ''
setToken(newToken)
pendingQueue.forEach(cb => cb(newToken))      // newToken 确保为 string
```

---

#### Step 3：更新 opr 模块 API 文件（类型 B/C/D 的源头）

将以下 5 个文件中 `import request from '@/api/request'` 改为 `import { http } from '@/api/request'`，并将所有 `request.get/post/put/delete` 替换为 `http.get/post/put/delete`：

- `src/api/opr/change.ts`
- `src/api/opr/termination.ts`
- `src/api/opr/ledger.ts`
- `src/api/opr/flow.ts`
- `src/api/opr/revenue.ts`

同时补全无泛型参数的函数，例如：

```ts
// 修改前（无泛型，返回 any）
export function getTerminationById(id: number) {
  return request.get(`/opr/terminations/${id}`)
}

// 修改后（带泛型，返回 Promise<TerminationDetailVO>）
export function getTerminationById(id: number) {
  return http.get<TerminationDetailVO>(`/opr/terminations/${id}`)
}
```

完整的泛型补全清单（`opr/termination.ts`）：

| 函数 | 补全类型 |
|------|---------|
| `getTerminationPage` | `http.get<PageResult<TerminationDetailVO>>` |
| `getTerminationById` | `http.get<TerminationDetailVO>` |
| `createTermination` | `http.post<number>` |
| `calculateSettlement` | `http.post<number>` |

---

#### Step 4：修复 `BasicLayout.vue` 权限报错（类型 B）

更新 `src/api/rpt/permission.ts`，使用 `http` 并显式声明返回类型：

```ts
// 修改前
export function getUserPermissions() {
  return request.get<UserPermissionVO>('/rpt/common/user-permissions')
}

// 修改后
export function getUserPermissions() {
  return http.get<UserPermissionVO>('/rpt/common/user-permissions')
}
```

修改后 `BasicLayout.vue` 中的 `data.hasFinViewPerm` 将自动获得正确类型，无需改动 Vue 文件。

---

#### Step 5：修复 `flow/index.vue` Blob 断言错误（类型 D，第 538 行）

`flow/index.vue` 中导出文件的请求使用了 `responseType: 'blob'`，拦截器会直接返回原始 `response.data`，需要在 API 层特殊处理：

```ts
// src/api/opr/flow.ts 中的导出函数（示例）
export function exportFlowData(params: object): Promise<Blob> {
  return request.get('/opr/flow/export', {
    params,
    responseType: 'blob',
  }) as unknown as Promise<Blob>
}
```

在 Vue 文件中直接使用该函数，不再需要强制转型。

---

## 四、修复任务分解

| 任务 | 文件 | 预估改动行数 | 优先级 |
|------|------|------------|--------|
| T1 | `src/api/request.ts` — 添加 `http` 导出 + 修复内部报错 | ~20 行 | P0 |
| T2 | `src/api/rpt/permission.ts` — 切换为 `http` | ~3 行 | P0 |
| T3 | `src/api/opr/change.ts` — 切换 + 补全泛型 | ~15 行 | P1 |
| T4 | `src/api/opr/termination.ts` — 切换 + 补全泛型 | ~15 行 | P1 |
| T5 | `src/api/opr/ledger.ts` — 切换 + 补全泛型 | ~10 行 | P1 |
| T6 | `src/api/opr/flow.ts` — 切换 + 补全泛型 + Blob 函数 | ~20 行 | P1 |
| T7 | `src/api/opr/revenue.ts` — 切换 + 补全泛型 | ~10 行 | P2 |
| T8 | 验证：`npm run build`（含 `vue-tsc`）无报错 | — | P0 |

---

## 五、执行顺序与验证

```
T1（request.ts）→ T2（permission.ts）→ T3~T7（opr API 文件）→ T8（全量编译验证）
```

每完成一批可用以下命令增量验证：

```bash
cd asset-ui

# 仅做类型检查，不编译产物（速度快）
npx vue-tsc --noEmit

# 完整编译
npm run build
```

预期：完成全部任务后 `vue-tsc` 报错数归零，`npm run build` 一步通过。

---

## 六、不建议的做法

- ❌ 在 Vue 文件中批量加 `as any` —— 掩盖问题，日后难以追查
- ❌ 把 `vue-tsc --noEmit` 从 `build` 脚本中删除 —— 失去编译期类型保护
- ❌ 逐个 Vue 文件加 `// @ts-ignore` —— 噪音多，影响代码可读性

---

*文档更新时间：2026-03-17*
