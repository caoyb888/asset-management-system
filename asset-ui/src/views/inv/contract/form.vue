<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>{{ isEdit ? '编辑合同' : '新增合同' }}</span>
          <div />
        </div>
      </template>

      <el-tabs v-model="activeTab">

        <!-- ===== Tab 1: 基本信息 ===== -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="basicFormRef" :model="form" label-width="120px" class="form-section">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item
                  label="合同名称"
                  prop="contractName"
                  :rules="[{ required: true, message: '请输入合同名称', trigger: 'blur' }]"
                >
                  <el-input v-model="form.contractName" placeholder="请输入合同名称" maxlength="100" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  label="合同类型"
                  prop="contractType"
                  :rules="[{ required: true, message: '请选择合同类型', trigger: 'change' }]"
                >
                  <el-select v-model="form.contractType" style="width: 100%">
                    <el-option :value="1" label="标准租赁合同" />
                    <el-option :value="2" label="临时租赁合同" />
                    <el-option :value="3" label="补充协议" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item
                  label="所属项目"
                  prop="projectId"
                  :rules="[{ required: true, message: '请选择项目', trigger: 'change' }]"
                >
                  <el-select
                    v-model="form.projectId"
                    filterable remote :remote-method="searchProjects"
                    placeholder="请搜索项目名称" style="width: 100%"
                    :disabled="isReadonly"
                  >
                    <el-option v-for="p in projectOptions" :key="p.id" :label="p.projectName" :value="p.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="商家">
                  <el-select
                    v-model="form.merchantId"
                    filterable remote clearable :remote-method="searchMerchants"
                    placeholder="请搜索商家名称" style="width: 100%"
                    :disabled="isReadonly"
                  >
                    <el-option v-for="m in merchantOptions" :key="m.id" :label="m.merchantName" :value="m.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="品牌">
                  <el-select
                    v-model="form.brandId"
                    filterable remote clearable :remote-method="searchBrands"
                    placeholder="请搜索品牌名称" style="width: 100%"
                    :disabled="isReadonly"
                  >
                    <el-option v-for="b in brandOptions" :key="b.id" :label="b.brandNameCn" :value="b.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="签约主体">
                  <el-input v-model="form.signingEntity" placeholder="乙方名称" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="合同开始日">
                  <el-date-picker v-model="form.contractStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="合同结束日">
                  <el-date-picker v-model="form.contractEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="交付日期">
                  <el-date-picker v-model="form.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="开业日期">
                  <el-date-picker v-model="form.openingDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="装修开始">
                  <el-date-picker v-model="form.decorationStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="装修结束">
                  <el-date-picker v-model="form.decorationEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" :disabled="isReadonly" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="付款周期">
                  <el-select v-model="form.paymentCycle" style="width: 100%" clearable :disabled="isReadonly">
                    <el-option :value="1" label="月付" />
                    <el-option :value="2" label="两月付" />
                    <el-option :value="3" label="季付" />
                    <el-option :value="4" label="四月付" />
                    <el-option :value="5" label="半年付" />
                    <el-option :value="6" label="年付" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="结算方式">
                  <el-select v-model="form.billingMode" style="width: 100%" clearable :disabled="isReadonly">
                    <el-option :value="1" label="预付" />
                    <el-option :value="2" label="当期" />
                    <el-option :value="3" label="后付" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 关联商铺只读展示（来自转合同或已保存） -->
            <template v-if="shops.length > 0">
              <el-divider content-position="left">关联商铺</el-divider>
              <el-table :data="shops" border size="small" max-height="240">
                <el-table-column prop="shopId" label="商铺ID" width="90" />
                <el-table-column prop="formatType" label="业态" />
                <el-table-column prop="area" label="面积(㎡)" width="110" align="right" />
              </el-table>
            </template>
          </el-form>

          <!-- 操作栏 -->
          <div class="tab-actions" v-if="!isReadonly">
            <el-button type="primary" :loading="saving" @click="handleSave">保存基本信息</el-button>
          </div>
        </el-tab-pane>

        <!-- ===== Tab 2: 费项配置 ===== -->
        <el-tab-pane label="费项配置" name="fees">
          <div class="toolbar" v-if="!isReadonly && isDraft">
            <el-button type="primary" plain size="small" :icon="Plus" @click="addFeeRow">添加费项</el-button>
          </div>

          <el-table :data="feeRows" border style="width: 100%">
            <el-table-column label="收款项目" min-width="150">
              <template #default="{ row }">
                <FeeItemSelector
                  v-model="row.feeItemId"
                  :grouped="false"
                  :disabled="isReadonly || !isDraft"
                  @select="(item) => onFeeItemSelect(row, item)"
                />
              </template>
            </el-table-column>
            <el-table-column label="收费方式" width="130">
              <template #default="{ row }">
                <el-select v-model="row.chargeType" style="width: 100%" :disabled="isReadonly || !isDraft">
                  <el-option :value="1" label="固定租金" />
                  <el-option :value="2" label="固定提成" />
                  <el-option :value="3" label="阶梯提成" />
                  <el-option :value="4" label="两者取高" />
                  <el-option :value="5" label="一次性" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="单价(元/㎡/月)" width="145">
              <template #default="{ row }">
                <el-input-number
                  v-if="[1,2,3,4].includes(row.chargeType)"
                  v-model="row.unitPrice" :precision="2" :min="0"
                  controls-position="right" style="width: 100%"
                  :disabled="isReadonly || !isDraft"
                />
                <span v-else class="text-placeholder">-</span>
              </template>
            </el-table-column>
            <el-table-column label="面积(㎡)" width="110">
              <template #default="{ row }">
                <el-input-number
                  v-if="[1,2,3,4].includes(row.chargeType)"
                  v-model="row.area" :precision="2" :min="0"
                  controls-position="right" style="width: 100%"
                  :disabled="isReadonly || !isDraft"
                />
                <span v-else class="text-placeholder">-</span>
              </template>
            </el-table-column>
            <el-table-column label="一次性金额(元)" width="145">
              <template #default="{ row }">
                <el-input-number
                  v-if="row.chargeType === 5"
                  v-model="row.oneTimeAmount" :precision="2" :min="0"
                  controls-position="right" style="width: 100%"
                  :disabled="isReadonly || !isDraft"
                />
                <span v-else class="text-placeholder">-</span>
              </template>
            </el-table-column>
            <el-table-column label="最低提成(元/月)" width="145">
              <template #default="{ row }">
                <el-input-number
                  v-if="[2,3,4].includes(row.chargeType)"
                  v-model="row.minCommissionAmount" :precision="2" :min="0"
                  controls-position="right" style="width: 100%"
                  :disabled="isReadonly || !isDraft"
                />
                <span v-else class="text-placeholder">-</span>
              </template>
            </el-table-column>
            <el-table-column label="金额(元)" width="120" align="right">
              <template #default="{ row }">
                {{ row.amount != null ? fmtAmount(row.amount) : '-' }}
              </template>
            </el-table-column>
            <el-table-column v-if="!isReadonly && isDraft" label="操作" width="65" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link :icon="Delete" @click="removeFeeRow($index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="feeRows.length === 0" description="暂无费项数据" :image-size="60" />

          <div class="tab-actions" v-if="!isReadonly && contractId">
            <el-button type="primary" plain :loading="feeSaving" @click="handleSaveFees">保存费项</el-button>
            <el-button type="success" :loading="costLoading" @click="handleGenerateCost">
              生成费用
            </el-button>
            <span v-if="totalAmount != null" class="total-amount">
              合同总金额：<strong>¥ {{ fmtAmount(totalAmount) }}</strong>
            </span>
          </div>
        </el-tab-pane>

        <!-- ===== Tab 3: 分铺计租阶段 ===== -->
        <el-tab-pane label="分铺计租" name="feeStages">
          <template v-if="qualifyingFees.length === 0">
            <el-empty description="当前无阶梯提成/两者取高类型费项，无需配置分铺计租阶段" :image-size="80" />
          </template>
          <template v-else>
            <el-alert
              type="info" :closable="false"
              title="分铺计租阶段：针对「阶梯提成」和「两者取高」类型费项，按商铺和时间段分别配置计租参数"
              style="margin-bottom: 16px"
            />
            <div v-for="fee in qualifyingFees" :key="fee.savedFeeId" style="margin-bottom: 24px">
              <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px">
                <el-tag :type="fee.chargeType === 3 ? 'warning' : 'primary'" size="small">
                  {{ fee.chargeType === 3 ? '阶梯提成' : '两者取高' }}
                </el-tag>
                <span style="font-weight: 500">{{ fee.feeName || `费项#${fee.savedFeeId}` }}</span>
                <el-button
                  v-if="!isReadonly && isDraft"
                  type="primary" plain size="small" :icon="Plus"
                  @click="addStageRow(fee.savedFeeId!)"
                >添加阶段</el-button>
              </div>
              <el-table :data="stagesByFeeId[fee.savedFeeId!] || []" border size="small">
                <el-table-column label="商铺（可选）" min-width="130">
                  <template #default="{ row }">
                    <el-select
                      v-model="row.shopId" clearable placeholder="整体计租"
                      style="width: 100%" :disabled="isReadonly || !isDraft"
                    >
                      <el-option
                        v-for="s in shops" :key="s.shopId"
                        :label="`商铺${s.shopId}`" :value="s.shopId"
                      />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="阶段开始" width="140">
                  <template #default="{ row }">
                    <el-date-picker
                      v-model="row.stageStart" type="date" value-format="YYYY-MM-DD"
                      style="width: 100%" :disabled="isReadonly || !isDraft"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="阶段结束" width="140">
                  <template #default="{ row }">
                    <el-date-picker
                      v-model="row.stageEnd" type="date" value-format="YYYY-MM-DD"
                      style="width: 100%" :disabled="isReadonly || !isDraft"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="单价(元/㎡/月)" width="135">
                  <template #default="{ row }">
                    <el-input-number
                      v-model="row.unitPrice" :precision="2" :min="0"
                      controls-position="right" style="width: 100%"
                      :disabled="isReadonly || !isDraft"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="提成比例(%)" width="120">
                  <template #default="{ row }">
                    <el-input-number
                      v-model="row.commissionRate" :precision="2" :min="0" :max="100"
                      controls-position="right" style="width: 100%"
                      :disabled="isReadonly || !isDraft"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="最低提成(元/月)" width="140">
                  <template #default="{ row }">
                    <el-input-number
                      v-model="row.minCommissionAmount" :precision="2" :min="0"
                      controls-position="right" style="width: 100%"
                      :disabled="isReadonly || !isDraft"
                    />
                  </template>
                </el-table-column>
                <el-table-column v-if="!isReadonly && isDraft" label="操作" width="60" align="center">
                  <template #default="{ $index }">
                    <el-button
                      type="danger" link :icon="Delete"
                      @click="removeStageRow(fee.savedFeeId!, $index)"
                    />
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="tab-actions" v-if="!isReadonly && contractId && isDraft">
              <el-button type="primary" :loading="stageSaving" @click="handleSaveFeeStages">
                保存阶段配置
              </el-button>
            </div>
          </template>
        </el-tab-pane>

        <!-- ===== Tab 4: 账期列表 ===== -->
        <el-tab-pane label="账期列表" name="billing">
          <div class="tab-actions" v-if="!isReadonly && contractId && isDraft" style="margin-bottom: 12px">
            <el-button type="primary" plain :loading="billingLoading" @click="handleGenerateBilling">
              生成账期
            </el-button>
          </div>

          <el-table v-loading="billingLoading" :data="billingList" border stripe max-height="420">
            <el-table-column prop="billingStart" label="账期开始" width="120" />
            <el-table-column prop="billingEnd" label="账期结束" width="120" />
            <el-table-column prop="dueDate" label="应收日" width="110" />
            <el-table-column label="账期类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.billingType === 1 ? 'warning' : 'info'">
                  {{ row.billingType === 1 ? '首账期' : '普通' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额(元)" width="140" align="right">
              <template #default="{ row }">{{ fmtAmount(row.amount) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="billingList.length === 0" description="暂无账期数据，请先生成账期" :image-size="60" />

          <!-- 发起审批（仅草稿状态且有账期时展示） -->
          <div class="tab-actions" v-if="!isReadonly && isDraft && billingList.length > 0" style="margin-top: 20px">
            <el-button type="danger" size="large" :loading="submitting" @click="handleSubmitApproval">
              发起审批
            </el-button>
          </div>

          <!-- Mock 审批回调（仅审批中状态展示，方便测试） -->
          <div v-if="contractStatus === 1" class="mock-approval">
            <el-divider>Mock 审批操作（仅测试用）</el-divider>
            <el-button type="success" @click="mockApprove(true)">模拟审批通过</el-button>
            <el-button type="danger" @click="mockApprove(false)">模拟审批驳回</el-button>
          </div>
        </el-tab-pane>

        <!-- ===== Tab 5: 版本历史 ===== -->
        <el-tab-pane label="版本历史" name="versions" lazy>
          <el-table v-loading="versionsLoading" :data="versionList" border stripe>
            <el-table-column prop="version" label="版本号" width="80" align="center" />
            <el-table-column prop="changeReason" label="变更原因" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="快照预览" width="90" align="center">
              <template #default="{ row }">
                <el-popover placement="left" :width="400" trigger="click">
                  <template #reference>
                    <el-button link type="primary">查看</el-button>
                  </template>
                  <pre style="max-height: 300px; overflow: auto; font-size: 12px">{{ JSON.stringify(row.snapshotData, null, 2) }}</pre>
                </el-popover>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="versionList.length === 0 && !versionsLoading" description="暂无版本记录" :image-size="60" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Delete } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

import FeeItemSelector from '@/components/inv/FeeItemSelector.vue'
import type { FeeItemVO } from '@/api/inv/config'
import type { ProjectVO } from '@/api/base/project'
import type { MerchantVO } from '@/api/base/merchant'
import type { BrandVO } from '@/api/base/brand'

import { getProjectPage } from '@/api/base/project'
import { getMerchantPage } from '@/api/base/merchant'
import { getBrandPage } from '@/api/base/brand'

import {
  createContract, updateContract, getContractDetail,
  submitContractApproval, approvalCallback,
  saveContractFees, getContractFees,
  saveContractFeeStages, getContractFeeStages,
  generateContractCost, generateContractBilling, getContractBillingList,
  getContractShops, getContractVersions,
  type ContractFeeVO, type ContractShopVO, type ContractBillingVO,
  type ContractFeeStageVO, type ContractVersionVO,
} from '@/api/inv/contract'

// ─── Route & mode ───────────────────────────────────────────────────────────
const route = useRoute()
const router = useRouter()
const routeId = computed(() => route.query.id ? Number(route.query.id) : null)
const isEdit = computed(() => !!routeId.value)
const isReadonly = computed(() => route.query.readonly === '1')
const contractId = ref<number | null>(null)
const contractStatus = ref<number>(0)
const activeTab = ref('basic')

const isDraft = computed(() => contractStatus.value === 0)

// ─── Loading 状态 ────────────────────────────────────────────────────────────
const saving = ref(false)
const feeSaving = ref(false)
const stageSaving = ref(false)
const costLoading = ref(false)
const billingLoading = ref(false)
const submitting = ref(false)
const versionsLoading = ref(false)

// ─── 基本信息表单 ─────────────────────────────────────────────────────────────
const basicFormRef = ref<FormInstance>()
const form = ref({
  contractName: '',
  contractType: undefined as number | undefined,
  projectId: null as number | null,
  merchantId: null as number | null,
  brandId: null as number | null,
  signingEntity: '',
  rentSchemeId: null as number | null,
  contractStart: '',
  contractEnd: '',
  deliveryDate: '',
  openingDate: '',
  decorationStart: '',
  decorationEnd: '',
  paymentCycle: null as number | null,
  billingMode: null as number | null,
})

// ─── 下拉选项 ─────────────────────────────────────────────────────────────────
const projectOptions = ref<ProjectVO[]>([])
const merchantOptions = ref<MerchantVO[]>([])
const brandOptions = ref<BrandVO[]>([])

async function searchProjects(q: string) {
  const res = await getProjectPage({ pageNum: 1, pageSize: 20, projectName: q })
  projectOptions.value = res.records ?? []
}

async function searchMerchants(q: string) {
  const res = await getMerchantPage({ pageNum: 1, pageSize: 20, merchantName: q })
  merchantOptions.value = res.records ?? []
}

async function searchBrands(q: string) {
  const res = await getBrandPage({ pageNum: 1, pageSize: 20, brandNameCn: q })
  brandOptions.value = res.records ?? []
}

// ─── 关联商铺（只读展示） ───────────────────────────────────────────────────────
const shops = ref<ContractShopVO[]>([])

// ─── 费项配置 ─────────────────────────────────────────────────────────────────
interface FeeRow {
  savedFeeId?: number
  feeItemId: number | null
  feeName: string
  chargeType: number
  unitPrice: number | undefined
  area: number | undefined
  oneTimeAmount: number | undefined
  minCommissionAmount: number | undefined
  amount: number | null
}

const feeRows = ref<FeeRow[]>([])
const totalAmount = ref<number | null>(null)

function addFeeRow() {
  feeRows.value.push({
    feeItemId: null,
    feeName: '',
    chargeType: 1,
    unitPrice: undefined,
    area: undefined,
    oneTimeAmount: undefined,
    minCommissionAmount: undefined,
    amount: null,
  })
}

function removeFeeRow(index: number) {
  feeRows.value.splice(index, 1)
}

function onFeeItemSelect(row: FeeRow, item: FeeItemVO | undefined) {
  if (item) row.feeName = item.itemName
}

// ─── 分铺计租阶段 ──────────────────────────────────────────────────────────────
interface StageRow {
  contractFeeId: number
  shopId?: number
  stageStart: string
  stageEnd: string
  unitPrice?: number
  commissionRate?: number
  minCommissionAmount?: number
}
// 按 contractFeeId 分组存储
const stagesByFeeId = ref<Record<number, StageRow[]>>({})

/** 需要配置分铺阶段的费项（chargeType 3=阶梯提成 4=两者取高） */
const qualifyingFees = computed(() =>
  feeRows.value.filter(f => f.savedFeeId && [3, 4].includes(f.chargeType))
)

function addStageRow(feeId: number) {
  if (!stagesByFeeId.value[feeId]) stagesByFeeId.value[feeId] = []
  stagesByFeeId.value[feeId].push({
    contractFeeId: feeId,
    stageStart: '',
    stageEnd: '',
  })
}

function removeStageRow(feeId: number, index: number) {
  stagesByFeeId.value[feeId]?.splice(index, 1)
}

function syncStageRows(stages: ContractFeeStageVO[]) {
  const map: Record<number, StageRow[]> = {}
  for (const s of stages) {
    if (!map[s.contractFeeId]) map[s.contractFeeId] = []
    map[s.contractFeeId].push({
      contractFeeId: s.contractFeeId,
      shopId: s.shopId || undefined,
      stageStart: s.stageStart,
      stageEnd: s.stageEnd,
      unitPrice: s.unitPrice || undefined,
      commissionRate: s.commissionRate || undefined,
      minCommissionAmount: s.minCommissionAmount || undefined,
    })
  }
  stagesByFeeId.value = map
}

// ─── 版本历史 ─────────────────────────────────────────────────────────────────
const versionList = ref<ContractVersionVO[]>([])

// ─── 账期列表 ─────────────────────────────────────────────────────────────────
const billingList = ref<ContractBillingVO[]>([])

// ─── 保存基本信息 ─────────────────────────────────────────────────────────────
async function handleSave() {
  const valid = await basicFormRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const dto = {
      contractName: form.value.contractName,
      contractType: form.value.contractType,
      projectId: form.value.projectId ?? undefined,
      merchantId: form.value.merchantId ?? undefined,
      brandId: form.value.brandId ?? undefined,
      signingEntity: form.value.signingEntity || undefined,
      rentSchemeId: form.value.rentSchemeId ?? undefined,
      contractStart: form.value.contractStart || undefined,
      contractEnd: form.value.contractEnd || undefined,
      deliveryDate: form.value.deliveryDate || undefined,
      openingDate: form.value.openingDate || undefined,
      decorationStart: form.value.decorationStart || undefined,
      decorationEnd: form.value.decorationEnd || undefined,
      paymentCycle: form.value.paymentCycle ?? undefined,
      billingMode: form.value.billingMode ?? undefined,
    }
    if (!contractId.value) {
      contractId.value = await createContract(dto)
      ElMessage.success('合同已创建（草稿）')
    } else {
      await updateContract(contractId.value, dto)
      ElMessage.success('保存成功')
    }
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// ─── 保存费项 ─────────────────────────────────────────────────────────────────
async function handleSaveFees() {
  if (!contractId.value) { ElMessage.warning('请先保存基本信息'); return }
  feeSaving.value = true
  try {
    const feeDTOs = feeRows.value.map(row => ({
      feeItemId: row.feeItemId ?? undefined,
      feeName: row.feeName,
      chargeType: row.chargeType,
      unitPrice: row.unitPrice,
      area: row.area,
      formulaParams: row.chargeType === 5 && row.oneTimeAmount != null
        ? { amount: row.oneTimeAmount }
        : [2, 3, 4].includes(row.chargeType) && row.minCommissionAmount != null
          ? { min_commission_amount: row.minCommissionAmount }
          : undefined,
    }))
    await saveContractFees(contractId.value, feeDTOs)
    // 刷新费项（获取服务端 ID）
    const saved: ContractFeeVO[] = await getContractFees(contractId.value)
    syncFeeRows(saved)
    ElMessage.success('费项保存成功')
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '保存失败')
  } finally {
    feeSaving.value = false
  }
}

function syncFeeRows(saved: ContractFeeVO[]) {
  feeRows.value = saved.map(fee => {
    const fp: Record<string, unknown> = (fee.formulaParams as Record<string, unknown>) ?? {}
    return {
      savedFeeId: fee.id,
      feeItemId: fee.feeItemId,
      feeName: fee.feeName,
      chargeType: fee.chargeType,
      unitPrice: fee.unitPrice as number | undefined,
      area: fee.area as number | undefined,
      oneTimeAmount: fp['amount'] as number | undefined,
      minCommissionAmount: (fp['min_commission_amount'] ?? fp['minCommissionAmount']) as number | undefined,
      amount: fee.amount,
    }
  })
}

// ─── 生成费用 ─────────────────────────────────────────────────────────────────
async function handleGenerateCost() {
  if (!contractId.value) return
  costLoading.value = true
  try {
    const result = await generateContractCost(contractId.value)
    totalAmount.value = result.totalAmount
    syncFeeRows(result.fees)
    ElMessage.success(`费用生成成功，合计 ¥${fmtAmount(result.totalAmount)}`)
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '费用生成失败')
  } finally {
    costLoading.value = false
  }
}

// ─── 生成账期 ─────────────────────────────────────────────────────────────────
async function handleGenerateBilling() {
  if (!contractId.value) return
  billingLoading.value = true
  try {
    billingList.value = await generateContractBilling(contractId.value)
    ElMessage.success(`账期生成成功，共 ${billingList.value.length} 条`)
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '账期生成失败')
  } finally {
    billingLoading.value = false
  }
}

// ─── 保存分铺计租阶段 ──────────────────────────────────────────────────────────
async function handleSaveFeeStages() {
  if (!contractId.value) return
  // 合并所有费项的阶段为一个数组
  const allStages = Object.values(stagesByFeeId.value).flat()
  // 校验必填字段
  const invalid = allStages.some(s => !s.stageStart || !s.stageEnd)
  if (invalid) { ElMessage.warning('请填写所有阶段的开始和结束日期'); return }
  stageSaving.value = true
  try {
    await saveContractFeeStages(contractId.value, allStages)
    ElMessage.success(`阶段配置保存成功，共 ${allStages.length} 条`)
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '保存失败')
  } finally {
    stageSaving.value = false
  }
}

// ─── 发起审批 ─────────────────────────────────────────────────────────────────
async function handleSubmitApproval() {
  if (!contractId.value) return
  await ElMessageBox.confirm('提交审批后将不可再编辑，确认提交？', '发起审批', { type: 'warning' })
  submitting.value = true
  try {
    await submitContractApproval(contractId.value)
    contractStatus.value = 1
    ElMessage.success('已成功提交审批')
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// ─── Mock 审批 ────────────────────────────────────────────────────────────────
async function mockApprove(approved: boolean) {
  if (!contractId.value) return
  try {
    await approvalCallback(contractId.value, { approved, comment: approved ? '审批通过' : '驳回' })
    contractStatus.value = approved ? 2 : 0
    ElMessage.success(approved ? '合同已生效' : '已驳回，合同回到草稿')
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '操作失败')
  }
}

// ─── 格式化 ───────────────────────────────────────────────────────────────────
function fmtAmount(n: number | null | undefined) {
  if (n == null) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ─── 编辑模式：加载数据 ───────────────────────────────────────────────────────
async function loadEditData(id: number) {
  try {
    const [detail, shopList, feeList, billings, stages] = await Promise.all([
      getContractDetail(id),
      getContractShops(id),
      getContractFees(id),
      getContractBillingList(id),
      getContractFeeStages(id),
    ])
    contractStatus.value = detail.status

    form.value.contractName = detail.contractName || ''
    form.value.contractType = detail.contractType
    form.value.projectId = detail.projectId ?? null
    form.value.merchantId = detail.merchantId ?? null
    form.value.brandId = detail.brandId ?? null
    form.value.signingEntity = detail.signingEntity || ''
    form.value.rentSchemeId = detail.rentSchemeId ?? null
    form.value.contractStart = detail.contractStart ? String(detail.contractStart) : ''
    form.value.contractEnd = detail.contractEnd ? String(detail.contractEnd) : ''
    form.value.deliveryDate = detail.deliveryDate ? String(detail.deliveryDate) : ''
    form.value.openingDate = detail.openingDate ? String(detail.openingDate) : ''
    form.value.decorationStart = detail.decorationStart ? String(detail.decorationStart) : ''
    form.value.decorationEnd = detail.decorationEnd ? String(detail.decorationEnd) : ''
    form.value.paymentCycle = detail.paymentCycle ?? null
    form.value.billingMode = detail.billingMode ?? null
    totalAmount.value = detail.totalAmount ?? null

    shops.value = shopList
    syncFeeRows(feeList)
    billingList.value = billings
    syncStageRows(stages)
  } catch {
    ElMessage.error('加载合同数据失败，请刷新重试')
  }
}

// ─── 版本历史懒加载（切换到 versions tab 时） ─────────────────────────────────
watch(activeTab, async (tab) => {
  if (tab === 'versions' && contractId.value && versionList.value.length === 0) {
    versionsLoading.value = true
    try {
      versionList.value = await getContractVersions(contractId.value)
    } finally {
      versionsLoading.value = false
    }
  }
})

// ─── 初始化 ───────────────────────────────────────────────────────────────────
onMounted(async () => {
  await Promise.all([searchProjects(''), searchMerchants(''), searchBrands('')])
  if (isEdit.value && routeId.value) {
    contractId.value = routeId.value
    await loadEditData(routeId.value)
  }
})
</script>

<style scoped>
.form-section {
  max-width: 900px;
}

.toolbar {
  margin-bottom: 10px;
}

.tab-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.total-amount {
  font-size: 14px;
  color: #606266;
}

.total-amount strong {
  font-size: 16px;
  color: #e6a23c;
}

.text-placeholder {
  color: #c0c4cc;
  font-size: 13px;
}

.mock-approval {
  margin-top: 20px;
  text-align: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
