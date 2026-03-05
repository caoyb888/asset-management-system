<template>
  <div class="form-page">
    <el-card shadow="never" class="form-card">
      <div class="card-header">
        <el-button :icon="ArrowLeft" text @click="router.push("/inv/intentions")">返回</el-button>
        <span class="header-title">{{ isEdit ? '编辑意向协议' : '新增意向协议' }}</span>
        <div />
      </div>

      <!-- 向导步骤条 -->
      <el-steps :active="currentStep" align-center class="mb-6" finish-status="success">
        <el-step title="计租方案" />
        <el-step title="基础信息" />
        <el-step title="商务信息" />
        <el-step title="费项配置" />
        <el-step title="分铺计租" />
        <el-step title="费用生成" />
        <el-step title="账期设置" />
      </el-steps>

      <!-- ===== Step 0: 计租方案 ===== -->
      <div v-show="currentStep === 0" class="step-panel">
        <div class="step-hint">请选择本次意向的计租方案，计租方案决定了收费逻辑和账期生成规则</div>
        <RentSchemeSelector v-model="selectedSchemeId" @select="onSchemeSelect" />
      </div>

      <!-- ===== Step 1: 基础信息 ===== -->
      <div v-show="currentStep === 1" class="step-panel">
        <el-form ref="basicFormRef" :model="basicForm" label-width="120px" class="form-section">
          <el-form-item
            label="意向名称"
            prop="intentionName"
            :rules="[{ required: true, message: '请输入意向名称', trigger: 'blur' }]"
          >
            <el-input v-model="basicForm.intentionName" placeholder="请输入意向协议名称" maxlength="100" show-word-limit />
          </el-form-item>
          <el-form-item
            label="所属项目"
            prop="projectId"
            :rules="[{ required: true, message: '请选择项目', trigger: 'change' }]"
          >
            <el-select
              v-model="basicForm.projectId"
              filterable
              remote
              :remote-method="searchProjects"
              placeholder="请搜索项目名称"
              style="width: 100%"
              @change="onProjectChange"
            >
              <el-option v-for="p in projectOptions" :key="p.id" :label="p.projectName" :value="p.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="签约方">
            <el-input v-model="basicForm.signingEntity" placeholder="签约方名称（乙方）" />
          </el-form-item>
          <el-form-item label="商家">
            <el-select
              v-model="basicForm.merchantId"
              filterable
              remote
              clearable
              :remote-method="searchMerchants"
              placeholder="请搜索商家名称"
              style="width: 100%"
            >
              <el-option v-for="m in merchantOptions" :key="m.id" :label="m.merchantName" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="品牌">
            <el-select
              v-model="basicForm.brandId"
              filterable
              remote
              clearable
              :remote-method="searchBrands"
              placeholder="请搜索品牌名称"
              style="width: 100%"
            >
              <el-option v-for="b in brandOptions" :key="b.id" :label="b.brandNameCn" :value="b.id" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <!-- ===== Step 2: 商务信息 ===== -->
      <div v-show="currentStep === 2" class="step-panel">
        <el-form ref="businessFormRef" :model="businessForm" label-width="120px" class="form-section">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item
                label="合同开始日"
                prop="contractStart"
                :rules="[{ required: true, message: '请选择合同开始日期', trigger: 'change' }]"
              >
                <el-date-picker
                  v-model="businessForm.contractStart"
                  type="date"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item
                label="合同结束日"
                prop="contractEnd"
                :rules="[{ required: true, message: '请选择合同结束日期', trigger: 'change' }]"
              >
                <el-date-picker
                  v-model="businessForm.contractEnd"
                  type="date"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="付款周期">
                <el-select v-model="businessForm.paymentCycle" style="width: 100%" clearable>
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
                <el-select v-model="businessForm.billingMode" style="width: 100%" clearable>
                  <el-option :value="1" label="预付" />
                  <el-option :value="2" label="当期" />
                  <el-option :value="3" label="后付" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="交付日期">
                <el-date-picker v-model="businessForm.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="开业日期">
                <el-date-picker v-model="businessForm.openingDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="装修开始">
                <el-date-picker v-model="businessForm.decorationStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="装修结束">
                <el-date-picker v-model="businessForm.decorationEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <!-- 商铺选择 -->
          <el-divider content-position="left">关联商铺</el-divider>
          <div class="shop-filter">
            <el-select
              v-model="shopFilter.buildingId"
              clearable
              placeholder="按楼栋筛选"
              style="width: 200px"
              :disabled="!basicForm.projectId"
              @change="loadShops"
            >
              <el-option v-for="b in buildingOptions" :key="b.id" :label="b.buildingName" :value="b.id" />
            </el-select>
            <el-button @click="loadShops">刷新</el-button>
            <span class="selected-count">已选 {{ selectedShops.length }} 个商铺</span>
          </div>
          <el-table
            ref="shopTableRef"
            v-loading="shopsLoading"
            :data="availableShops"
            border
            max-height="280"
            row-key="id"
            @selection-change="onShopSelectionChange"
          >
            <el-table-column type="selection" width="55" reserve-selection />
            <el-table-column prop="shopCode" label="商铺编号" width="120" />
            <el-table-column prop="buildingName" label="楼栋" width="110" />
            <el-table-column prop="floorName" label="楼层" width="80" />
            <el-table-column prop="rentArea" label="租赁面积(㎡)" width="120" align="right" />
            <el-table-column prop="shopStatusName" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.shopStatus === 0 ? 'success' : 'info'" size="small">
                  {{ row.shopStatusName || '-' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-form>
      </div>

      <!-- ===== Step 3: 费项配置 ===== -->
      <div v-show="currentStep === 3" class="step-panel">
        <div class="step-hint">
          <el-alert
            type="info"
            :closable="false"
            :title="`当前计租方案：${selectedScheme?.schemeName ?? '-'}，收费方式：${chargeTypeLabel(selectedScheme?.chargeType)}`"
          />
        </div>
        <div class="toolbar">
          <el-button type="primary" plain size="small" :icon="Plus" @click="addFeeRow">添加费项</el-button>
        </div>
        <el-table :data="feeRows" border style="width: 100%">
          <el-table-column label="收款项目" min-width="150">
            <template #default="{ row }">
              <FeeItemSelector
                v-model="row.feeItemId"
                :grouped="false"
                @select="(item) => onFeeItemSelect(row, item)"
              />
            </template>
          </el-table-column>
          <el-table-column label="收费方式" width="130">
            <template #default="{ row }">
              <el-select v-model="row.chargeType" style="width: 100%">
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
                v-model="row.unitPrice"
                :precision="2"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
              <span v-else class="text-placeholder">-</span>
            </template>
          </el-table-column>
          <el-table-column label="面积(㎡)" width="110">
            <template #default="{ row }">
              <el-input-number
                v-if="[1,2,3,4].includes(row.chargeType)"
                v-model="row.area"
                :precision="2"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
              <span v-else class="text-placeholder">-</span>
            </template>
          </el-table-column>
          <el-table-column label="一次性金额(元)" width="145">
            <template #default="{ row }">
              <el-input-number
                v-if="row.chargeType === 5"
                v-model="row.oneTimeAmount"
                :precision="2"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
              <span v-else class="text-placeholder">-</span>
            </template>
          </el-table-column>
          <el-table-column label="最低提成(元/月)" width="145">
            <template #default="{ row }">
              <el-input-number
                v-if="[2,3,4].includes(row.chargeType)"
                v-model="row.minCommissionAmount"
                :precision="2"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
              <span v-else class="text-placeholder">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="65" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link :icon="Delete" @click="removeFeeRow($index)" />
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="feeRows.length === 0" description="请点击「添加费项」按钮添加收款项" :image-size="60" />
      </div>

      <!-- ===== Step 4: 分铺计租 ===== -->
      <div v-show="currentStep === 4" class="step-panel">
        <div v-if="needStageFees.length === 0">
          <el-alert
            type="info"
            :closable="false"
            title="当前费项均无需配置分铺计租阶段（仅阶梯提成、两者取高需要配置），可直接下一步"
          />
        </div>
        <el-tabs v-else type="border-card">
          <el-tab-pane
            v-for="fee in needStageFees"
            :key="fee._rowIndex"
            :label="fee.feeName || `费项 ${fee._rowIndex + 1}`"
          >
            <div class="toolbar">
              <el-button type="primary" plain size="small" :icon="Plus" @click="addStageRow(fee._rowIndex)">
                添加阶段
              </el-button>
            </div>
            <el-table :data="getStagesForFee(fee._rowIndex)" border size="small">
              <el-table-column label="商铺" width="140">
                <template #default="{ row }">
                  <el-select v-model="row.shopId" clearable placeholder="全部商铺" style="width: 100%">
                    <el-option
                      v-for="s in selectedShops"
                      :key="s.id"
                      :label="s.shopCode"
                      :value="s.id"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="阶段开始" width="150">
                <template #default="{ row }">
                  <el-date-picker v-model="row.stageStart" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="阶段结束" width="150">
                <template #default="{ row }">
                  <el-date-picker v-model="row.stageEnd" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="单价(元/㎡/月)" width="140">
                <template #default="{ row }">
                  <el-input-number v-model="row.unitPrice" :precision="2" :min="0" controls-position="right" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="提成率(%)" width="110">
                <template #default="{ row }">
                  <el-input-number v-model="row.commissionRate" :precision="4" :min="0" :max="100" controls-position="right" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="最低提成(元)" width="130">
                <template #default="{ row }">
                  <el-input-number v-model="row.minCommissionAmount" :precision="2" :min="0" controls-position="right" style="width: 100%" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60" align="center">
                <template #default="{ $index }">
                  <el-button type="danger" link :icon="Delete" @click="removeStageRow(fee._rowIndex, $index)" />
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="getStagesForFee(fee._rowIndex).length === 0" description="请添加阶梯阶段" :image-size="50" />
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- ===== Step 5: 费用生成 ===== -->
      <div v-show="currentStep === 5" class="step-panel">
        <div v-if="costLoading" class="loading-center">
          <el-icon class="spinning" size="40"><Loading /></el-icon>
          <p class="mt-2">正在计算费用，请稍候...</p>
        </div>
        <div v-else-if="costResult">
          <el-result icon="success" title="费用计算成功">
            <template #sub-title>
              <div class="cost-total">合同期总金额：<strong class="amount-value">¥ {{ fmtAmount(costResult.totalAmount) }}</strong></div>
            </template>
            <template #extra>
              <el-table
                :data="costResult.items || []"
                border
                stripe
                style="max-width: 640px; margin: 16px auto 0"
              >
                <el-table-column prop="feeName" label="费项名称" />
                <el-table-column prop="chargeTypeName" label="收费方式" width="110" />
                <el-table-column label="金额(元)" width="160" align="right">
                  <template #default="{ row }">{{ fmtAmount(row.amount) }}</template>
                </el-table-column>
              </el-table>
            </template>
          </el-result>
        </div>
        <div v-else class="loading-center">
          <el-icon size="32" color="#e6a23c"><Warning /></el-icon>
          <p class="mt-2">费用生成失败，请检查费项配置后重试</p>
          <el-button type="primary" class="mt-4" @click="doGenerateCost">重新生成</el-button>
        </div>
      </div>

      <!-- ===== Step 6: 账期设置 ===== -->
      <div v-show="currentStep === 6" class="step-panel">
        <div v-if="billingLoading" class="loading-center">
          <el-icon class="spinning" size="40"><Loading /></el-icon>
          <p class="mt-2">正在生成账期...</p>
        </div>
        <template v-else>
          <div class="billing-summary">
            共 <strong>{{ billingList.length }}</strong> 个账期&nbsp;&nbsp;
            合计 <strong>¥ {{ fmtAmount(costResult?.totalAmount) }}</strong>
          </div>
          <el-table :data="billingList" border stripe max-height="400">
            <el-table-column prop="feeName" label="费项" min-width="120" />
            <el-table-column prop="billingStart" label="账期开始" width="120" />
            <el-table-column prop="billingEnd" label="账期结束" width="120" />
            <el-table-column label="账期类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.billingType === 1 ? 'warning' : undefined">
                  {{ row.billingType === 1 ? '首账期' : '普通' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="金额(元)" width="140" align="right">
              <template #default="{ row }">{{ fmtAmount(row.amount) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="billingList.length === 0" description="暂无账期数据" :image-size="60" />
        </template>
      </div>

      <!-- ===== 步骤按钮 ===== -->
      <div class="step-actions">
        <el-button v-if="currentStep > 0" :disabled="saving" @click="prevStep">上一步</el-button>
        <el-button
          v-if="currentStep < 6"
          type="primary"
          :loading="saving"
          @click="nextStep"
        >
          {{ currentStep === 5 ? '确认费用，下一步' : '下一步' }}
        </el-button>
        <el-button
          v-if="currentStep === 6"
          type="success"
          @click="approvalDialogVisible = true"
        >
          提交审批
        </el-button>
        <el-button
          v-if="currentStep >= 1 && currentStep <= 3 && intentionId"
          :loading="draftSaving"
          @click="handleSaveDraft"
        >
          暂存
        </el-button>
      </div>

      <!-- 审批流程时间线（已创建记录后展示） -->
      <template v-if="intentionId">
        <el-divider content-position="left">审批流程</el-divider>
        <div class="timeline-wrap">
          <ApprovalTimeline :current-status="intentionCurrentStatus" />
        </div>
      </template>
    </el-card>
  </div>

  <!-- 审批发起弹窗 -->
  <ApprovalDialog
    v-model:visible="approvalDialogVisible"
    title="提交意向协议审批"
    :loading="submitting"
    @confirm="onApprovalConfirm"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useIntentionStore } from '@/store/modules/inv/intention'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus, Delete, Loading, Warning } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'

import RentSchemeSelector from '@/components/inv/RentSchemeSelector.vue'
import FeeItemSelector from '@/components/inv/FeeItemSelector.vue'
import ApprovalDialog from '@/components/inv/ApprovalDialog.vue'
import ApprovalTimeline from '@/components/inv/ApprovalTimeline.vue'

import type { RentSchemeVO } from '@/api/inv/config'
import type { FeeItemVO } from '@/api/inv/config'
import type { ProjectVO } from '@/api/base/project'
import type { MerchantVO } from '@/api/base/merchant'
import type { BrandVO } from '@/api/base/brand'
import type { BuildingVO } from '@/api/base/building'
import type { ShopVO } from '@/api/base/shop'

import { getProjectPage } from '@/api/base/project'
import { getMerchantPage } from '@/api/base/merchant'
import { getBrandPage } from '@/api/base/brand'
import { getBuildingPage } from '@/api/base/building'
import { getShopPage } from '@/api/base/shop'

import {
  createIntention, updateIntention, getIntentionDetail, saveDraft as apiSaveDraft, submitApproval,
  saveShops, getShops, saveFees, getFees, saveFeeStages, getFeeStages,
  generateCost as apiGenerateCost, generateBilling as apiGenerateBilling,
  type IntentionSaveDTO, type IntentionFeeVO, type CostResultVO, type IntentionBillingVO,
} from '@/api/inv/intention'
import { getRentSchemeDetail } from '@/api/inv/config'

// ─── Pinia 向导状态缓存 ──────────────────────────────────────────────────────
const intentionStore = useIntentionStore()

// ─── Route & mode ───────────────────────────────────────────────────────────
const route = useRoute()
const router = useRouter()
const routeId = computed(() => route.query.id ? Number(route.query.id) : null)
const isEdit = computed(() => !!routeId.value)
const intentionId = ref<number | null>(null)
const intentionCurrentStatus = ref(0)  // 审批状态 0草稿 1审批中 2通过 3驳回 4已转合同
const approvalDialogVisible = ref(false)

// ─── 步骤状态 ────────────────────────────────────────────────────────────────
const currentStep = ref(0)
const saving = ref(false)
const draftSaving = ref(false)
const submitting = ref(false)

// ─── Step 0: 计租方案 ────────────────────────────────────────────────────────
const selectedSchemeId = ref<number | null>(null)
const selectedScheme = ref<RentSchemeVO | null>(null)

function onSchemeSelect(scheme: RentSchemeVO) {
  selectedScheme.value = scheme
  // 从方案中预填付款周期和结算方式
  businessForm.value.paymentCycle = scheme.paymentCycle
  businessForm.value.billingMode = scheme.billingMode
  // 上游变更时清空下游
  feeRows.value = []
  stageRows.value = []
  costResult.value = null
  billingList.value = []
  // 同步到 Store（上游变更清空下游数据）
  intentionStore.setRentScheme({
    schemeId: scheme.id,
    schemeName: scheme.schemeName,
    chargeType: scheme.chargeType ?? null,
  })
}

/** 将当前表单状态同步到 Pinia store（步骤完成/切换时调用） */
function syncToStore() {
  intentionStore.$patch((s) => {
    s.intentionId = intentionId.value
    s.currentStep = currentStep.value
    s.basicInfo = {
      projectId: basicForm.value.projectId,
      projectName: '',
      intentionName: basicForm.value.intentionName,
      partyBName: basicForm.value.signingEntity,
      merchantId: basicForm.value.merchantId,
      brandId: basicForm.value.brandId,
    }
    s.businessInfo = {
      shopIds: selectedShops.value.map((sh) => sh.id),
      contractStart: businessForm.value.contractStart,
      contractEnd: businessForm.value.contractEnd,
      deliveryDate: businessForm.value.deliveryDate,
      decorationStart: businessForm.value.decorationStart,
      decorationEnd: businessForm.value.decorationEnd,
      openingDate: businessForm.value.openingDate,
      paymentCycle: businessForm.value.paymentCycle,
      billingMode: businessForm.value.billingMode,
    }
  })
}

/** 从 Pinia store 恢复表单状态（新增模式导航离开后重新进入时） */
function restoreFromStore() {
  const s = intentionStore
  selectedSchemeId.value = s.rentScheme.schemeId
  basicForm.value.intentionName = s.basicInfo.intentionName
  basicForm.value.projectId = s.basicInfo.projectId
  basicForm.value.signingEntity = s.basicInfo.partyBName
  basicForm.value.merchantId = s.basicInfo.merchantId
  basicForm.value.brandId = s.basicInfo.brandId
  businessForm.value.contractStart = s.businessInfo.contractStart
  businessForm.value.contractEnd = s.businessInfo.contractEnd
  businessForm.value.deliveryDate = s.businessInfo.deliveryDate
  businessForm.value.decorationStart = s.businessInfo.decorationStart
  businessForm.value.decorationEnd = s.businessInfo.decorationEnd
  businessForm.value.openingDate = s.businessInfo.openingDate
  businessForm.value.paymentCycle = s.businessInfo.paymentCycle
  businessForm.value.billingMode = s.businessInfo.billingMode
  // 费项及以后步骤需从后端重新拉取，最多恢复到步骤2
  currentStep.value = Math.min(s.currentStep, 2)
}

function chargeTypeLabel(t?: number | null) {
  const map: Record<number, string> = { 1: '固定租金', 2: '固定提成', 3: '阶梯提成', 4: '两者取高', 5: '一次性' }
  return t != null ? (map[t] ?? '-') : '-'
}

// ─── Step 1: 基础信息 ────────────────────────────────────────────────────────
const basicFormRef = ref<FormInstance>()
const basicForm = ref({
  intentionName: '',
  projectId: null as number | null,
  signingEntity: '',
  merchantId: null as number | null,
  brandId: null as number | null,
})

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

function onProjectChange(projectId: number | null) {
  buildingOptions.value = []
  shopFilter.value.buildingId = null
  availableShops.value = []
  selectedShops.value = []
  if (projectId) loadBuildings(projectId)
}

// ─── Step 2: 商务信息 ────────────────────────────────────────────────────────
const businessFormRef = ref<FormInstance>()
const businessForm = ref({
  contractStart: '',
  contractEnd: '',
  deliveryDate: '',
  decorationStart: '',
  decorationEnd: '',
  openingDate: '',
  paymentCycle: null as number | null,
  billingMode: null as number | null,
})

// 商铺选择
const shopTableRef = ref()
const buildingOptions = ref<BuildingVO[]>([])
const shopFilter = ref({ buildingId: null as number | null })
const availableShops = ref<ShopVO[]>([])
const selectedShops = ref<ShopVO[]>([])
const shopsLoading = ref(false)

async function loadBuildings(projectId: number) {
  const res = await getBuildingPage({ pageNum: 1, pageSize: 100, projectId })
  buildingOptions.value = res.records ?? []
}

async function loadShops() {
  if (!basicForm.value.projectId) return
  shopsLoading.value = true
  try {
    const res = await getShopPage({
      pageNum: 1,
      pageSize: 200,
      projectId: basicForm.value.projectId,
      buildingId: shopFilter.value.buildingId ?? undefined,
    })
    availableShops.value = res.records ?? []
    // 恢复已选行
    await nextTick()
    const selectedIds = new Set(selectedShops.value.map((s) => s.id))
    availableShops.value.forEach((shop) => {
      if (selectedIds.has(shop.id)) {
        shopTableRef.value?.toggleRowSelection(shop, true)
      }
    })
  } finally {
    shopsLoading.value = false
  }
}

function onShopSelectionChange(selection: ShopVO[]) {
  selectedShops.value = selection
}

// ─── Step 3: 费项配置 ────────────────────────────────────────────────────────
interface FeeRow {
  _rowIndex: number
  savedFeeId?: number     // 保存后由服务端返回的ID
  feeItemId: number | null
  feeName: string
  chargeType: number
  unitPrice: number | undefined
  area: number | undefined
  oneTimeAmount: number | undefined
  minCommissionAmount: number | undefined
}

let _feeCounter = 0
const feeRows = ref<FeeRow[]>([])

function addFeeRow() {
  feeRows.value.push({
    _rowIndex: _feeCounter++,
    feeItemId: null,
    feeName: '',
    chargeType: selectedScheme.value?.chargeType ?? 1,
    unitPrice: undefined,
    area: undefined,
    oneTimeAmount: undefined,
    minCommissionAmount: undefined,
  })
}

function removeFeeRow(index: number) {
  feeRows.value.splice(index, 1)
}

function onFeeItemSelect(row: FeeRow, item: FeeItemVO | undefined) {
  if (item) row.feeName = item.itemName
}

// 需要配置分铺计租阶段的费项（阶梯提成3、两者取高4）
const needStageFees = computed(() => feeRows.value.filter((f) => f.chargeType === 3 || f.chargeType === 4))

// ─── Step 4: 分铺计租 ────────────────────────────────────────────────────────
interface StageRow {
  feeRowIndex: number
  shopId: number | null
  stageStart: string
  stageEnd: string
  unitPrice: number | undefined
  commissionRate: number | undefined
  minCommissionAmount: number | undefined
}

const stageRows = ref<StageRow[]>([])

function getStagesForFee(feeRowIndex: number) {
  return stageRows.value.filter((s) => s.feeRowIndex === feeRowIndex)
}

function addStageRow(feeRowIndex: number) {
  stageRows.value.push({
    feeRowIndex,
    shopId: null,
    stageStart: businessForm.value.contractStart || '',
    stageEnd: businessForm.value.contractEnd || '',
    unitPrice: undefined,
    commissionRate: undefined,
    minCommissionAmount: undefined,
  })
}

function removeStageRow(feeRowIndex: number, indexInFee: number) {
  const feeStages = stageRows.value.filter((s) => s.feeRowIndex === feeRowIndex)
  const target = feeStages[indexInFee]
  const globalIdx = stageRows.value.indexOf(target)
  if (globalIdx >= 0) stageRows.value.splice(globalIdx, 1)
}

// ─── Step 5: 费用生成 ────────────────────────────────────────────────────────
const costLoading = ref(false)
const costResult = ref<CostResultVO | null>(null)

async function doGenerateCost() {
  if (!intentionId.value) return
  costLoading.value = true
  costResult.value = null
  try {
    costResult.value = await apiGenerateCost(intentionId.value)
  } catch {
    ElMessage.error('费用生成失败，请检查费项配置')
  } finally {
    costLoading.value = false
  }
}

// ─── Step 6: 账期 ────────────────────────────────────────────────────────────
const billingLoading = ref(false)
const billingList = ref<IntentionBillingVO[]>([])

async function doGenerateBilling() {
  if (!intentionId.value) return
  billingLoading.value = true
  billingList.value = []
  try {
    billingList.value = await apiGenerateBilling(intentionId.value)
  } catch {
    ElMessage.error('账期生成失败')
  } finally {
    billingLoading.value = false
  }
}

// ─── 步骤导航 ────────────────────────────────────────────────────────────────
async function nextStep() {
  const ok = await validateStep(currentStep.value)
  if (!ok) return

  saving.value = true
  try {
    await saveStep(currentStep.value)
    currentStep.value++
    syncToStore()  // 步骤完成后同步数据到 Pinia store
    await onEnterStep(currentStep.value)
  } catch (err: unknown) {
    const msg = (err as { message?: string })?.message
    ElMessage.error(msg || '操作失败，请重试')
  } finally {
    saving.value = false
  }
}

function prevStep() {
  if (currentStep.value > 0) {
    currentStep.value--
    intentionStore.$patch({ currentStep: currentStep.value })
  }
}

async function validateStep(step: number): Promise<boolean> {
  if (step === 0) {
    if (!selectedSchemeId.value) { ElMessage.warning('请选择计租方案'); return false }
  }
  if (step === 1) {
    const valid = await basicFormRef.value?.validate().then(() => true).catch(() => false)
    return valid ?? false
  }
  if (step === 2) {
    const valid = await businessFormRef.value?.validate().then(() => true).catch(() => false)
    return valid ?? false
  }
  if (step === 3) {
    if (feeRows.value.length === 0) { ElMessage.warning('请至少添加一个费项'); return false }
    const noItem = feeRows.value.some((f) => !f.feeItemId)
    if (noItem) { ElMessage.warning('存在未选择收款项目的费项，请补充'); return false }
  }
  return true
}

async function saveStep(step: number) {
  switch (step) {
    case 0: break  // 无需调用 API
    case 1: await saveBasicInfo(); break
    case 2: await saveBusinessInfo(); break
    case 3: await saveFeeItems(); break
    case 4: await saveFeeStageItems(); break
    case 5: break  // 费用已生成
  }
}

async function onEnterStep(step: number) {
  if (step === 5) await doGenerateCost()
  if (step === 6) await doGenerateBilling()
}

// ─── 各步骤保存方法 ───────────────────────────────────────────────────────────
function buildDTO(): IntentionSaveDTO {
  return {
    intentionName: basicForm.value.intentionName,
    projectId: basicForm.value.projectId ?? undefined,
    merchantId: basicForm.value.merchantId ?? undefined,
    brandId: basicForm.value.brandId ?? undefined,
    signingEntity: basicForm.value.signingEntity || undefined,
    rentSchemeId: selectedSchemeId.value ?? undefined,
    contractStart: businessForm.value.contractStart || undefined,
    contractEnd: businessForm.value.contractEnd || undefined,
    deliveryDate: businessForm.value.deliveryDate || undefined,
    decorationStart: businessForm.value.decorationStart || undefined,
    decorationEnd: businessForm.value.decorationEnd || undefined,
    openingDate: businessForm.value.openingDate || undefined,
    paymentCycle: businessForm.value.paymentCycle ?? undefined,
    billingMode: businessForm.value.billingMode ?? undefined,
  }
}

async function saveBasicInfo() {
  const dto = buildDTO()
  if (!intentionId.value) {
    intentionId.value = await createIntention(dto)
  } else {
    await updateIntention(intentionId.value, dto)
  }
}

async function saveBusinessInfo() {
  await updateIntention(intentionId.value!, buildDTO())
  const shopDTOs = selectedShops.value.map((shop) => ({
    shopId: shop.id,
    buildingId: shop.buildingId,
    floorId: shop.floorId,
    area: shop.rentArea,
  }))
  await saveShops(intentionId.value!, shopDTOs)
}

async function saveFeeItems() {
  const feeDTOs = feeRows.value.map((row) => ({
    feeItemId: row.feeItemId ?? undefined,
    feeName: row.feeName,
    chargeType: row.chargeType,
    unitPrice: row.unitPrice,
    area: row.area,
    startDate: businessForm.value.contractStart || undefined,
    endDate: businessForm.value.contractEnd || undefined,
    formulaParams: row.chargeType === 5 && row.oneTimeAmount != null
      ? { amount: row.oneTimeAmount }
      : undefined,
  }))
  await saveFees(intentionId.value!, feeDTOs)

  // 拉取保存后的费项（含 ID），用于第4步阶段关联
  const saved: IntentionFeeVO[] = await getFees(intentionId.value!)
  saved.forEach((sf, i) => {
    if (feeRows.value[i]) feeRows.value[i].savedFeeId = sf.id
  })
}

async function saveFeeStageItems() {
  if (needStageFees.value.length === 0 || stageRows.value.length === 0) return

  const validStages: import('@/api/inv/intention').IntentionFeeStageItemDTO[] = stageRows.value
    .filter((s) => s.stageStart && s.stageEnd)
    .flatMap((s) => {
      const feeRow = feeRows.value.find((f) => f._rowIndex === s.feeRowIndex)
      if (!feeRow?.savedFeeId) return []
      return [{
        intentionFeeId: feeRow.savedFeeId,
        shopId: s.shopId ?? undefined,
        stageStart: s.stageStart,
        stageEnd: s.stageEnd,
        unitPrice: s.unitPrice,
        commissionRate: s.commissionRate,
        minCommissionAmount: s.minCommissionAmount,
      }]
    })

  if (validStages.length > 0) {
    await saveFeeStages(intentionId.value!, validStages)
  }
}

// ─── 暂存 & 提交 ─────────────────────────────────────────────────────────────
async function handleSaveDraft() {
  if (!intentionId.value) { ElMessage.warning('请先完成基础信息并保存'); return }
  draftSaving.value = true
  try {
    await apiSaveDraft(intentionId.value, buildDTO())
    ElMessage.success('已暂存')
  } finally {
    draftSaving.value = false
  }
}

async function onApprovalConfirm(_payload: { approverIds: number[]; comment: string }) {
  if (!intentionId.value) return
  submitting.value = true
  try {
    await submitApproval(intentionId.value)
    intentionCurrentStatus.value = 1
    approvalDialogVisible.value = false
    ElMessage.success('已成功提交审批')
    router.push('/inv/intentions')
  } catch (err: unknown) {
    ElMessage.error((err as { message?: string })?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// ─── 格式化 ──────────────────────────────────────────────────────────────────
function fmtAmount(n: number | null | undefined) {
  if (n == null) return '-'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ─── 编辑模式：从后端加载数据回填各步骤 ──────────────────────────────────────
async function loadEditData(id: number) {
  saving.value = true
  try {
    // 并行拉取：主体详情 + 关联商铺 + 费项 + 分铺计租阶段
    const [detail, linkedShops, fees, stages] = await Promise.all([
      getIntentionDetail(id),
      getShops(id),
      getFees(id),
      getFeeStages(id),
    ])

    // ── Step 0: 计租方案 ──
    if (detail.rentSchemeId) {
      selectedSchemeId.value = detail.rentSchemeId
      try {
        const scheme = await getRentSchemeDetail(detail.rentSchemeId)
        selectedScheme.value = scheme
        // 从方案预填付款周期和结算方式（若意向自身未设定）
        if (!detail.paymentCycle) businessForm.value.paymentCycle = scheme.paymentCycle
        if (!detail.billingMode) businessForm.value.billingMode = scheme.billingMode
      } catch { /* 方案已停用时忽略 */ }
    }

    // ── 审批状态 ──
    intentionCurrentStatus.value = detail.status ?? 0

    // ── Step 1: 基础信息 ──
    basicForm.value.intentionName = detail.intentionName || ''
    basicForm.value.projectId = detail.projectId ?? null
    basicForm.value.signingEntity = detail.signingEntity || ''
    basicForm.value.merchantId = detail.merchantId ?? null
    basicForm.value.brandId = detail.brandId ?? null

    // ── Step 2: 商务信息（日期 / 周期） ──
    businessForm.value.contractStart  = detail.contractStart  ? String(detail.contractStart)  : ''
    businessForm.value.contractEnd    = detail.contractEnd    ? String(detail.contractEnd)    : ''
    businessForm.value.deliveryDate   = detail.deliveryDate   ? String(detail.deliveryDate)   : ''
    businessForm.value.decorationStart = detail.decorationStart ? String(detail.decorationStart) : ''
    businessForm.value.decorationEnd  = detail.decorationEnd  ? String(detail.decorationEnd)  : ''
    businessForm.value.openingDate    = detail.openingDate    ? String(detail.openingDate)    : ''
    businessForm.value.paymentCycle   = detail.paymentCycle   ?? businessForm.value.paymentCycle
    businessForm.value.billingMode    = detail.billingMode    ?? businessForm.value.billingMode

    // 加载楼栋 + 商铺，再恢复选中状态
    if (detail.projectId) {
      await loadBuildings(detail.projectId)
      await loadShops()
      const linkedIds = new Set(linkedShops.map((s) => s.shopId))
      const toSelect = availableShops.value.filter((s) => linkedIds.has(s.id))
      selectedShops.value = toSelect
      await nextTick()
      toSelect.forEach((shop) => shopTableRef.value?.toggleRowSelection(shop, true))
    }

    // ── Step 3: 费项配置 ──
    _feeCounter = 0
    feeRows.value = fees.map((fee: IntentionFeeVO) => {
      const fp: Record<string, unknown> = fee.formulaParams ?? {}
      return {
        _rowIndex: _feeCounter++,
        savedFeeId: fee.id,
        feeItemId: fee.feeItemId,
        feeName: fee.feeName,
        chargeType: fee.chargeType,
        unitPrice: fee.unitPrice as number | undefined,
        area: fee.area as number | undefined,
        oneTimeAmount: fp['amount'] as number | undefined,
        minCommissionAmount: fp['minCommissionAmount'] as number | undefined,
      }
    })

    // ── Step 4: 分铺计租阶段 ──
    stageRows.value = stages.map((s) => {
      const feeRow = feeRows.value.find((f) => f.savedFeeId === s.intentionFeeId)
      return {
        feeRowIndex: feeRow?._rowIndex ?? 0,
        shopId: s.shopId ?? null,
        stageStart: s.stageStart ? String(s.stageStart) : '',
        stageEnd: s.stageEnd ? String(s.stageEnd) : '',
        unitPrice: s.unitPrice,
        commissionRate: s.commissionRate,
        minCommissionAmount: s.minCommissionAmount,
      }
    })

    // 编辑模式默认停在步骤 1（基础信息），让用户从此开始检查
    currentStep.value = 1
    ElMessage.success('已加载意向协议数据')
  } catch {
    ElMessage.error('加载意向协议数据失败，请刷新重试')
  } finally {
    saving.value = false
  }
}

// ─── 初始化 ──────────────────────────────────────────────────────────────────
onMounted(async () => {
  // 并行预加载下拉选项（编辑模式下也需要这些列表以正确显示已选项）
  await Promise.all([searchProjects(''), searchMerchants(''), searchBrands('')])

  if (isEdit.value && routeId.value) {
    // 编辑模式：初始化 store 并从后端加载数据
    intentionStore.initEdit(routeId.value)
    intentionId.value = routeId.value
    await loadEditData(routeId.value)
  } else {
    // 新增模式：若 store 中有上次未完成的数据则恢复，否则重置
    if (intentionStore.rentScheme.schemeId !== null) {
      restoreFromStore()
    } else {
      intentionStore.initCreate()
    }
  }
})
</script>

<style scoped>
.step-panel {
  min-height: 340px;
  padding: 8px 0 16px;
}

.step-hint {
  margin-bottom: 16px;
}

.form-section {
  max-width: 720px;
}

.toolbar {
  margin-bottom: 10px;
}

.shop-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.selected-count {
  color: #409eff;
  font-size: 13px;
  font-weight: 500;
}

.step-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.loading-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 220px;
  color: #606266;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.cost-total {
  font-size: 15px;
  margin-top: 4px;
}

.amount-value {
  font-size: 20px;
  color: #e6a23c;
}

.billing-summary {
  margin-bottom: 12px;
  font-size: 14px;
  color: #606266;
}

.text-placeholder {
  color: #c0c4cc;
  font-size: 13px;
}

.mt-2 {
  margin-top: 8px;
}

.mt-4 {
  margin-top: 16px;
}

.mb-6 {
  margin-bottom: 24px;
}

.form-page {
  padding-bottom: 24px;
}

.form-card {
  border-radius: 12px !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;

  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: #1e293b;
  }
}
.timeline-wrap { max-width: 600px; padding: 0 4px 16px; }
</style>
