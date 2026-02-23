<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>{{ isEdit ? '编辑租金分解' : '新增租金分解' }}</span>
          <div />
        </div>
      </template>

      <el-form :model="form" label-width="130px" class="form-body">
        <el-form-item label="关联租决政策" required>
          <el-select v-model="form.policyId" placeholder="请选择已审批的租决政策" style="width: 320px" filterable>
            <el-option label="暂无数据" :value="0" disabled />
          </el-select>
        </el-form-item>
        <el-form-item label="业务年度" required>
          <el-date-picker v-model="form.businessYear" type="year" placeholder="选择年度" value-format="YYYY" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" style="width: 400px" />
        </el-form-item>
      </el-form>

      <el-divider content-position="left">指标录入</el-divider>

      <el-tabs v-model="activeCategory">
        <el-tab-pane label="主力店" name="major">
          <el-empty description="主力店指标录入 - 开发中" />
        </el-tab-pane>
        <el-tab-pane label="次主力店" name="sub">
          <el-empty description="次主力店指标录入 - 开发中" />
        </el-tab-pane>
        <el-tab-pane label="一般商铺" name="normal">
          <el-empty description="一般商铺指标录入 - 开发中" />
        </el-tab-pane>
      </el-tabs>

      <el-alert v-if="totalAnnualRent" type="success" :closable="false" class="mt-4">
        年租金合计：<strong>{{ totalAnnualRent.toLocaleString() }} 元</strong>
      </el-alert>

      <div class="form-actions">
        <el-button type="warning" @click="handleCalculate">自动计算汇总</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
        <el-button @click="router.back()">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.query.id)
const activeCategory = ref('major')
const totalAnnualRent = ref<number | null>(null)
const form = ref({ policyId: undefined as number | undefined, businessYear: '', remark: '' })

function handleCalculate() { totalAnnualRent.value = 0; ElMessage.info('计算完成，当前合计 0 元（待录入数据）') }
function handleSave() { ElMessage.success('保存成功'); router.push('/inv/rent-decomps') }
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.form-body { max-width: 700px; }
.mt-4 { margin-top: 16px; }
.form-actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }
</style>
