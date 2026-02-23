<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" text @click="router.back()">返回</el-button>
          <span>{{ isEdit ? '开业审批详情' : '新增开业审批' }}</span>
          <div />
        </div>
      </template>

      <el-form :model="form" label-width="120px" class="form-body">
        <el-form-item label="关联合同" required>
          <el-select v-model="form.contractId" placeholder="请选择合同" style="width: 300px" filterable>
            <el-option label="暂无数据" :value="0" disabled />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开业日" required>
          <el-date-picker v-model="form.plannedOpenDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="审批备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" style="width: 400px" />
        </el-form-item>

        <el-divider content-position="left">附件上传</el-divider>
        <el-form-item label="附件">
          <el-upload drag action="#" :auto-upload="false" multiple>
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、图片格式，单文件不超过 20MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <div class="form-actions">
        <el-button type="primary" @click="handleSubmit">提交审批</el-button>
        <el-button @click="handleSaveDraft">暂存草稿</el-button>
        <el-button @click="router.back()">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, UploadFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.query.id)

const form = ref({ contractId: undefined as number | undefined, plannedOpenDate: '', remark: '' })

function handleSubmit() { ElMessage.success('提交成功'); router.push('/inv/opening-approvals') }
function handleSaveDraft() { ElMessage.success('已暂存') }
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.form-body { max-width: 700px; }
.form-actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }
</style>
