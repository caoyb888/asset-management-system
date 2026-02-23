<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>{{ isEdit ? '编辑租决政策' : '新增租决政策' }}</span>
          <div />
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="form" label-width="120px" class="form-body">
            <el-form-item label="政策名称" required>
              <el-input v-model="form.policyName" placeholder="请输入政策名称" style="width: 300px" />
            </el-form-item>
            <el-form-item label="适用项目" required>
              <el-select v-model="form.projectId" placeholder="请选择项目" style="width: 300px" filterable>
                <el-option label="暂无数据" :value="0" disabled />
              </el-select>
            </el-form-item>
            <el-form-item label="适用年度" required>
              <el-date-picker v-model="form.effectiveYear" type="year" placeholder="选择年度" value-format="YYYY" />
            </el-form-item>
            <el-form-item label="政策说明">
              <el-input v-model="form.description" type="textarea" :rows="3" style="width: 400px" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="年度指标" name="annual">
          <el-empty description="年度指标配置 - 开发中" />
        </el-tab-pane>
        <el-tab-pane label="分类指标" name="category">
          <el-empty description="分类指标配置（主力/次主力/一般）- 开发中" />
        </el-tab-pane>
      </el-tabs>

      <div class="form-actions">
        <el-button type="primary" @click="handleSave">保存</el-button>
        <el-button type="success" @click="handleSubmitApproval">提交审批</el-button>
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
const activeTab = ref('basic')
const form = ref({ policyName: '', projectId: undefined as number | undefined, effectiveYear: '', description: '' })

function handleSave() { ElMessage.success('保存成功'); router.push('/inv/rent-policies') }
function handleSubmitApproval() { ElMessage.success('已提交审批') }
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.form-body { max-width: 700px; }
.form-actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }
</style>
