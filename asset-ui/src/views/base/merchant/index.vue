<template>
  <div class="merchant-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @keyup.enter="handleSearch">
        <el-form-item label="商家名称">
          <el-input v-model="query.merchantName" placeholder="请输入商家名称" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="所属项目">
          <el-select v-model="query.projectId" placeholder="全部项目" clearable filterable style="width: 180px">
            <el-option v-for="p in projectOptions" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商家属性">
          <el-select v-model="query.merchantAttr" placeholder="全部" clearable style="width: 110px">
            <el-option label="个体户" :value="1" /><el-option label="企业" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="商家评级">
          <el-select v-model="query.merchantLevel" placeholder="全部" clearable style="width: 110px">
            <el-option label="优秀" :value="1" /><el-option label="良好" :value="2" />
            <el-option label="一般" :value="3" /><el-option label="差" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 110px">
            <el-option label="待审核" :value="0" /><el-option label="通过" :value="1" /><el-option label="驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 + 表格 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增商家</el-button>
        <el-button :icon="Upload" @click="importDialogVisible = true">批量导入</el-button>
        <el-button :icon="Download" @click="handleDownloadTemplate">下载模板</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" border stripe row-key="id" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="merchantCode" label="商家编号" width="120" show-overflow-tooltip />
        <el-table-column prop="merchantName" label="商家名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="projectName" label="所属项目" width="150" show-overflow-tooltip />
        <el-table-column prop="merchantAttrName" label="商家属性" width="90" align="center" />
        <el-table-column prop="merchantNatureName" label="商家性质" width="90" align="center" />
        <el-table-column prop="formatType" label="经营业态" width="110" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机" width="120" />
        <el-table-column label="商家评级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.merchantLevel)" size="small">{{ row.merchantLevelName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="auditTagType(row.auditStatus)" size="small">{{ row.auditStatusName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.auditStatus === 0">
              <el-button link type="warning" size="small" @click="handleAudit(row)">审核</el-button>
              <el-divider direction="vertical" />
            </template>
            <el-button link type="primary" size="small" @click="handleDetail(row)">详情</el-button>
            <el-divider direction="vertical" />
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-divider direction="vertical" />
            <el-popconfirm title="确认删除该商家？" confirm-button-text="确认" cancel-button-text="取消" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑 Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="820px" :close-on-click-modal="false" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" label-position="right">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属项目" prop="projectId">
              <el-select v-model="form.projectId" placeholder="请选择项目" style="width: 100%" filterable>
                <el-option v-for="p in projectOptions" :key="p.id" :label="p.projectName" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家编号">
              <el-input v-model="form.merchantCode" placeholder="商家编号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家名称" prop="merchantName">
              <el-input v-model="form.merchantName" placeholder="请输入商家名称" maxlength="200" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家属性">
              <el-select v-model="form.merchantAttr" placeholder="请选择" clearable style="width: 100%">
                <el-option label="个体户" :value="1" /><el-option label="企业" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家性质">
              <el-select v-model="form.merchantNature" placeholder="请选择" clearable style="width: 100%">
                <el-option label="民营" :value="1" /><el-option label="国营" :value="2" />
                <el-option label="外资" :value="3" /><el-option label="合资" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="经营业态">
              <el-input v-model="form.formatType" placeholder="经营业态" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商家评级">
              <el-select v-model="form.merchantLevel" placeholder="请选择" clearable style="width: 100%">
                <el-option label="优秀" :value="1" /><el-option label="良好" :value="2" />
                <el-option label="一般" :value="3" /><el-option label="差" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="审核状态">
              <el-select v-model="form.auditStatus" placeholder="请选择" clearable style="width: 100%">
                <el-option label="待审核" :value="0" /><el-option label="通过" :value="1" /><el-option label="驳回" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="自然人">
              <el-input v-model="form.naturalPerson" placeholder="自然人姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" placeholder="身份证号" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机">
              <el-input v-model="form.phone" placeholder="手机号码" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" placeholder="地址" maxlength="500" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 商家详情 Drawer -->
    <el-drawer
      v-model="detailDrawerVisible"
      :title="`商家详情 - ${currentMerchant?.merchantName || ''}`"
      size="760px"
      destroy-on-close
    >
      <el-tabs v-model="detailTab" @tab-change="onDetailTabChange">

        <!-- Tab1: 联系人 -->
        <el-tab-pane label="联系人" name="contacts">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="openContactDialog()">新增联系人</el-button>
          </div>
          <el-table v-loading="mContactLoading" :data="merchantContacts" border size="small" style="width:100%">
            <el-table-column prop="contactName" label="姓名" width="100" />
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip />
            <el-table-column prop="position" label="职位" width="110" />
            <el-table-column label="主要" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isPrimary === 1 ? 'success' : 'info'" size="small">{{ row.isPrimary === 1 ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openContactDialog(row)">编辑</el-button>
                <el-divider direction="vertical" />
                <el-popconfirm title="确认删除？" @confirm="deleteContact(row.id)">
                  <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab2: 诚信记录 -->
        <el-tab-pane label="诚信记录" name="credits">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="openCreditDialog()">新增记录</el-button>
          </div>
          <el-table v-loading="creditLoading" :data="merchantCredits" border size="small" style="width:100%">
            <el-table-column prop="recordTypeName" label="记录类型" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="creditTagType(row.recordType)" size="small">{{ row.recordTypeName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="内容" min-width="180" show-overflow-tooltip />
            <el-table-column prop="recordDate" label="记录日期" width="110" align="center" />
            <el-table-column prop="createdAt" label="创建时间" width="160" align="center" />
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ row }">
                <el-popconfirm title="确认删除？" @confirm="deleteCredit(row.id)">
                  <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab3: 开票信息 -->
        <el-tab-pane label="开票信息" name="invoices">
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="openInvoiceDialog()">新增开票信息</el-button>
          </div>
          <el-table v-loading="invoiceLoading" :data="merchantInvoices" border size="small" style="width:100%">
            <el-table-column prop="invoiceTitle" label="发票抬头" min-width="160" show-overflow-tooltip />
            <el-table-column prop="taxNumber" label="税号" width="160" show-overflow-tooltip />
            <el-table-column prop="bankName" label="开户银行" width="150" show-overflow-tooltip />
            <el-table-column prop="bankAccount" label="银行账号" width="160" show-overflow-tooltip />
            <el-table-column prop="phone" label="注册电话" width="120" />
            <el-table-column label="默认" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isDefault === 1 ? 'success' : 'info'" size="small">{{ row.isDefault === 1 ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openInvoiceDialog(row)">编辑</el-button>
                <el-divider direction="vertical" />
                <el-popconfirm title="确认删除？" @confirm="deleteInvoice(row.id)">
                  <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab4: 附件 -->
        <el-tab-pane label="附件" name="attachments">
          <el-table v-loading="attachmentLoading" :data="merchantAttachments" border size="small" style="width:100%">
            <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="fileType" label="类型" width="80" align="center" />
            <el-table-column prop="createdAt" label="上传时间" width="160" align="center" />
            <el-table-column label="操作" width="100" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="previewAttachment(row.fileUrl)">查看</el-button>
                <el-divider direction="vertical" />
                <el-popconfirm title="确认删除？" @confirm="deleteAttachment(row.id)">
                  <template #reference><el-button link type="danger" size="small">删除</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <!-- 联系人编辑 Dialog -->
    <el-dialog v-model="contactDialogVisible" :title="contactEditId ? '编辑联系人' : '新增联系人'" width="440px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactFormRules" label-width="90px">
        <el-form-item label="姓名" prop="contactName">
          <el-input v-model="contactForm.contactName" placeholder="必填" maxlength="50" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="contactForm.phone" maxlength="30" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="contactForm.email" maxlength="100" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="contactForm.position" maxlength="50" />
        </el-form-item>
        <el-form-item label="主要联系人">
          <el-switch v-model="contactForm.isPrimary" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="contactSaving" @click="submitContact">保存</el-button>
      </template>
    </el-dialog>

    <!-- 诚信记录 Dialog -->
    <el-dialog v-model="creditDialogVisible" title="新增诚信记录" width="480px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="creditFormRef" :model="creditForm" :rules="creditFormRules" label-width="90px">
        <el-form-item label="记录类型" prop="recordType">
          <el-select v-model="creditForm.recordType" placeholder="请选择" style="width:100%">
            <el-option label="好评" :value="1" /><el-option label="差评" :value="2" />
            <el-option label="违约" :value="3" /><el-option label="其他" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="记录内容" prop="content">
          <el-input v-model="creditForm.content" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker v-model="creditForm.recordDate" type="date" format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="creditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creditSaving" @click="submitCredit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 审核 Dialog -->
    <el-dialog v-model="auditDialogVisible" title="商家审核" width="420px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditFormRules" label-width="90px">
        <el-form-item label="商家名称">
          <span>{{ currentAuditMerchant?.merchantName }}</span>
        </el-form-item>
        <el-form-item label="审核结果" prop="auditStatus">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="auditForm.auditStatus === 2" label="驳回原因" prop="rejectReason">
          <el-input v-model="auditForm.rejectReason" type="textarea" :rows="3" placeholder="请填写驳回原因" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditSubmitting" @click="submitAudit">确认</el-button>
      </template>
    </el-dialog>

    <!-- 开票信息 Dialog -->
    <el-dialog v-model="invoiceDialogVisible" :title="invoiceEditId ? '编辑开票信息' : '新增开票信息'" width="520px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="invoiceFormRef" :model="invoiceForm" :rules="invoiceFormRules" label-width="90px">
        <el-form-item label="发票抬头" prop="invoiceTitle">
          <el-input v-model="invoiceForm.invoiceTitle" placeholder="必填" maxlength="200" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="invoiceForm.taxNumber" maxlength="50" />
        </el-form-item>
        <el-form-item label="开户银行">
          <el-input v-model="invoiceForm.bankName" maxlength="100" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="invoiceForm.bankAccount" maxlength="50" />
        </el-form-item>
        <el-form-item label="注册地址">
          <el-input v-model="invoiceForm.address" maxlength="300" />
        </el-form-item>
        <el-form-item label="注册电话">
          <el-input v-model="invoiceForm.phone" maxlength="30" />
        </el-form-item>
        <el-form-item label="默认">
          <el-switch v-model="invoiceForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invoiceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="invoiceSaving" @click="submitInvoice">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 Dialog -->
    <el-dialog v-model="importDialogVisible" title="批量导入商家" width="520px" :close-on-click-modal="false" destroy-on-close @close="resetImport">
      <el-form label-width="90px">
        <el-form-item label="所属项目">
          <el-select v-model="importProjectId" placeholder="请选择项目（必填）" filterable style="width: 100%">
            <el-option v-for="p in projectOptions" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            ref="importUploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="onImportFileChange"
            :on-remove="() => { importFile = null }"
            :file-list="[]"
            drag
            style="width: 100%"
          >
            <el-icon class="el-icon--upload"><Upload /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .xlsx / .xls 文件</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <div v-if="importResult" class="import-result">
        <el-alert
          :title="`导入完成：成功 ${importResult.successCount} 条，失败 ${importResult.failCount} 条`"
          :type="importResult.failCount > 0 ? 'warning' : 'success'"
          show-icon
          :closable="false"
        />
        <el-table v-if="importResult.errors?.length" :data="importResult.errors.map((e, i) => ({ index: i+1, msg: e }))" border size="small" style="margin-top:8px;max-height:160px;overflow-y:auto">
          <el-table-column prop="index" label="序号" width="60" align="center" />
          <el-table-column prop="msg" label="错误信息" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile || !importProjectId" @click="handleImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus, Upload, Download } from '@element-plus/icons-vue'
import {
  getMerchantPage, getMerchantDetail, createMerchant, updateMerchant, deleteMerchant,
  getMerchantContacts, addMerchantContact, updateMerchantContact, deleteMerchantContact,
  getMerchantCredits, addMerchantCredit, deleteMerchantCredit,
  getMerchantInvoices, addMerchantInvoice, updateMerchantInvoice, deleteMerchantInvoice,
  getMerchantAttachments, deleteMerchantAttachment,
  auditMerchant, importMerchants, downloadMerchantTemplate,
  type MerchantVO, type MerchantContactVO, type MerchantCreditVO, type MerchantInvoiceVO, type AttachmentVO,
} from '@/api/base/merchant'
import { getProjectList } from '@/api/base/project'
import { useAppStore } from '@/store/modules/app'

useAppStore().setPageTitle('商家管理')

// ─────────── 项目选项 ───────────
const projectOptions = ref<Array<{ id: number; projectName: string }>>([])
async function loadProjectOptions() {
  try { projectOptions.value = await getProjectList() ?? [] } catch { projectOptions.value = [] }
}

// ─────────── 列表 ───────────
const loading = ref(false)
const tableData = ref<MerchantVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1, pageSize: 10, merchantName: '',
  projectId: undefined as number | undefined,
  merchantAttr: undefined as number | undefined,
  merchantNature: undefined as number | undefined,
  merchantLevel: undefined as number | undefined,
  auditStatus: undefined as number | undefined,
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getMerchantPage(query)
    tableData.value = res.records ?? (res as any).data?.records ?? []
    total.value = res.total ?? (res as any).data?.total ?? 0
  } finally { loading.value = false }
}

function handleSearch() { query.pageNum = 1; fetchList() }
function handleReset() {
  Object.assign(query, { pageNum: 1, merchantName: '', projectId: undefined, merchantAttr: undefined, merchantNature: undefined, merchantLevel: undefined, auditStatus: undefined })
  fetchList()
}

function levelTagType(level: number) {
  return (level === 1 ? 'success' : level === 2 ? 'primary' : level === 3 ? 'warning' : 'danger') as any
}
function auditTagType(status: number) {
  return (status === 1 ? 'success' : status === 2 ? 'danger' : 'info') as any
}
function creditTagType(type: number) {
  return (type === 1 ? 'success' : type === 2 ? 'warning' : type === 3 ? 'danger' : 'info') as any
}

// ─────────── 新增/编辑 ───────────
const dialogVisible = ref(false)
const dialogTitle = ref('新增商家')
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

function defaultForm() {
  return {
    id: undefined as number | undefined,
    projectId: undefined as number | undefined,
    merchantCode: '', merchantName: '',
    merchantAttr: undefined as number | undefined,
    merchantNature: undefined as number | undefined,
    formatType: '', naturalPerson: '', idCard: '', address: '', phone: '',
    merchantLevel: undefined as number | undefined,
    auditStatus: 0 as number | undefined,
  }
}

const form = reactive(defaultForm())
const formRules: FormRules = {
  projectId:    [{ required: true, message: '请选择所属项目', trigger: 'change' }],
  merchantName: [{ required: true, message: '请输入商家名称', trigger: 'blur' }],
}

function handleAdd() {
  isEdit.value = false; dialogTitle.value = '新增商家'
  Object.assign(form, defaultForm()); dialogVisible.value = true
}

async function handleEdit(row: MerchantVO) {
  isEdit.value = true; dialogTitle.value = '编辑商家'
  const data = await getMerchantDetail(row.id)
  Object.assign(form, defaultForm(), { ...data, idCard: '' })
  dialogVisible.value = true
}

function resetForm() { formRef.value?.clearValidate() }

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value && form.id) { await updateMerchant(form.id, form as any); ElMessage.success('编辑成功') }
    else { await createMerchant(form as any); ElMessage.success('新增成功') }
    dialogVisible.value = false; fetchList()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try { await deleteMerchant(id); ElMessage.success('删除成功'); fetchList() } catch {}
}

// ═══════════════════════════════════════════════════════
// 商家详情 Drawer
// ═══════════════════════════════════════════════════════
const detailDrawerVisible = ref(false)
const detailTab = ref('contacts')
const currentMerchant = ref<MerchantVO | null>(null)

async function handleDetail(row: MerchantVO) {
  currentMerchant.value = row
  detailTab.value = 'contacts'
  detailDrawerVisible.value = true
  await loadContacts(row.id)
}

async function onDetailTabChange(tab: string | number) {
  const id = currentMerchant.value?.id
  if (!id) return
  if (tab === 'contacts') await loadContacts(id)
  else if (tab === 'credits') await loadCredits(id)
  else if (tab === 'invoices') await loadInvoices(id)
  else if (tab === 'attachments') await loadAttachments(id)
}

// ─────────── 联系人 ───────────
const mContactLoading = ref(false)
const merchantContacts = ref<MerchantContactVO[]>([])
const contactDialogVisible = ref(false)
const contactSaving = ref(false)
const contactEditId = ref<number | null>(null)
const contactFormRef = ref<FormInstance>()
const contactForm = reactive({ contactName: '', phone: '', email: '', position: '', isPrimary: 0 })
const contactFormRules: FormRules = {
  contactName: [{ required: true, message: '姓名不能为空', trigger: 'blur' }],
}

async function loadContacts(id: number) {
  mContactLoading.value = true
  try { merchantContacts.value = await getMerchantContacts(id) }
  finally { mContactLoading.value = false }
}

function openContactDialog(row?: MerchantContactVO) {
  contactEditId.value = row?.id ?? null
  Object.assign(contactForm, { contactName: '', phone: '', email: '', position: '', isPrimary: 0 })
  if (row) Object.assign(contactForm, row)
  contactDialogVisible.value = true
}

async function submitContact() {
  const valid = await contactFormRef.value?.validate().catch(() => false)
  if (!valid) return
  contactSaving.value = true
  const id = currentMerchant.value!.id
  try {
    if (contactEditId.value) { await updateMerchantContact(id, contactEditId.value, contactForm); ElMessage.success('修改成功') }
    else { await addMerchantContact(id, contactForm); ElMessage.success('新增成功') }
    contactDialogVisible.value = false
    await loadContacts(id)
  } finally { contactSaving.value = false }
}

async function deleteContact(cid: number) {
  try {
    await deleteMerchantContact(currentMerchant.value!.id, cid)
    ElMessage.success('删除成功')
    await loadContacts(currentMerchant.value!.id)
  } catch {}
}

// ─────────── 诚信记录 ───────────
const creditLoading = ref(false)
const merchantCredits = ref<MerchantCreditVO[]>([])
const creditDialogVisible = ref(false)
const creditSaving = ref(false)
const creditFormRef = ref<FormInstance>()
const creditForm = reactive({ recordType: undefined as number | undefined, content: '', recordDate: '' })
const creditFormRules: FormRules = {
  recordType: [{ required: true, message: '请选择记录类型', trigger: 'change' }],
  content:    [{ required: true, message: '内容不能为空', trigger: 'blur' }],
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }],
}

async function loadCredits(id: number) {
  creditLoading.value = true
  try { merchantCredits.value = await getMerchantCredits(id) }
  finally { creditLoading.value = false }
}

function openCreditDialog() {
  Object.assign(creditForm, { recordType: undefined, content: '', recordDate: '' })
  creditDialogVisible.value = true
}

async function submitCredit() {
  const valid = await creditFormRef.value?.validate().catch(() => false)
  if (!valid) return
  creditSaving.value = true
  const id = currentMerchant.value!.id
  try {
    await addMerchantCredit(id, creditForm as any)
    ElMessage.success('新增成功')
    creditDialogVisible.value = false
    await loadCredits(id)
  } finally { creditSaving.value = false }
}

async function deleteCredit(rid: number) {
  try {
    await deleteMerchantCredit(currentMerchant.value!.id, rid)
    ElMessage.success('删除成功')
    await loadCredits(currentMerchant.value!.id)
  } catch {}
}

// ─────────── 开票信息 ───────────
const invoiceLoading = ref(false)
const merchantInvoices = ref<MerchantInvoiceVO[]>([])
const invoiceDialogVisible = ref(false)
const invoiceSaving = ref(false)
const invoiceEditId = ref<number | null>(null)
const invoiceFormRef = ref<FormInstance>()
const invoiceForm = reactive({ invoiceTitle: '', taxNumber: '', bankName: '', bankAccount: '', address: '', phone: '', isDefault: 0 })
const invoiceFormRules: FormRules = {
  invoiceTitle: [{ required: true, message: '发票抬头不能为空', trigger: 'blur' }],
}

async function loadInvoices(id: number) {
  invoiceLoading.value = true
  try { merchantInvoices.value = await getMerchantInvoices(id) }
  finally { invoiceLoading.value = false }
}

function openInvoiceDialog(row?: MerchantInvoiceVO) {
  invoiceEditId.value = row?.id ?? null
  Object.assign(invoiceForm, { invoiceTitle: '', taxNumber: '', bankName: '', bankAccount: '', address: '', phone: '', isDefault: 0 })
  if (row) Object.assign(invoiceForm, row)
  invoiceDialogVisible.value = true
}

async function submitInvoice() {
  const valid = await invoiceFormRef.value?.validate().catch(() => false)
  if (!valid) return
  invoiceSaving.value = true
  const id = currentMerchant.value!.id
  try {
    if (invoiceEditId.value) { await updateMerchantInvoice(id, invoiceEditId.value, invoiceForm); ElMessage.success('修改成功') }
    else { await addMerchantInvoice(id, invoiceForm); ElMessage.success('新增成功') }
    invoiceDialogVisible.value = false
    await loadInvoices(id)
  } finally { invoiceSaving.value = false }
}

async function deleteInvoice(iid: number) {
  try {
    await deleteMerchantInvoice(currentMerchant.value!.id, iid)
    ElMessage.success('删除成功')
    await loadInvoices(currentMerchant.value!.id)
  } catch {}
}

// ─────────── 附件 ───────────
const attachmentLoading = ref(false)
const merchantAttachments = ref<AttachmentVO[]>([])

async function loadAttachments(id: number) {
  attachmentLoading.value = true
  try { merchantAttachments.value = await getMerchantAttachments(id) }
  finally { attachmentLoading.value = false }
}

function previewAttachment(url: string) {
  if (url) window.open(url, '_blank')
}

async function deleteAttachment(aid: number) {
  try {
    await deleteMerchantAttachment(currentMerchant.value!.id, aid)
    ElMessage.success('删除成功')
    await loadAttachments(currentMerchant.value!.id)
  } catch {}
}

// ─────────── 审核 ───────────
const auditDialogVisible = ref(false)
const auditSubmitting = ref(false)
const currentAuditMerchant = ref<MerchantVO | null>(null)
const auditFormRef = ref<FormInstance>()
const auditForm = reactive({ auditStatus: 1 as number, rejectReason: '' })
const auditFormRules: FormRules = {
  auditStatus: [{ required: true, message: '请选择审核结果', trigger: 'change' }],
  rejectReason: [{
    validator: (_rule: any, value: string, callback: Function) => {
      if (auditForm.auditStatus === 2 && !value?.trim()) {
        callback(new Error('请填写驳回原因'))
      } else {
        callback()
      }
    },
    trigger: 'blur',
  }],
}

function handleAudit(row: MerchantVO) {
  currentAuditMerchant.value = row
  Object.assign(auditForm, { auditStatus: 1, rejectReason: '' })
  auditDialogVisible.value = true
}

async function submitAudit() {
  const valid = await auditFormRef.value?.validate().catch(() => false)
  if (!valid) return
  auditSubmitting.value = true
  try {
    await auditMerchant(currentAuditMerchant.value!.id, auditForm.auditStatus)
    ElMessage.success('审核成功')
    auditDialogVisible.value = false
    fetchList()
  } finally { auditSubmitting.value = false }
}

// ─────────── 批量导入 ───────────
const importDialogVisible = ref(false)
const importing = ref(false)
const importFile = ref<File | null>(null)
const importProjectId = ref<number | undefined>(undefined)
const importResult = ref<{ successCount: number; failCount: number; errors: string[] } | null>(null)
const importUploadRef = ref()

function onImportFileChange(uploadFile: any) {
  importFile.value = uploadFile.raw ?? null
}

function resetImport() {
  importFile.value = null
  importProjectId.value = undefined
  importResult.value = null
}

async function handleImport() {
  if (!importFile.value || !importProjectId.value) return
  importing.value = true
  try {
    const res = await importMerchants(importFile.value, importProjectId.value)
    importResult.value = (res as any)?.data ?? res
    if ((importResult.value?.failCount ?? 0) === 0) {
      ElMessage.success(`导入成功，共导入 ${importResult.value?.successCount} 条`)
    }
    fetchList()
  } finally { importing.value = false }
}

async function handleDownloadTemplate() {
  try {
    const blob = await downloadMerchantTemplate() as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '商家导入模板.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch { ElMessage.error('模板下载失败') }
}

onMounted(() => { loadProjectOptions(); fetchList() })
</script>

<style scoped lang="scss">
.merchant-page {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .search-card :deep(.el-form-item) { margin-bottom: 0; }
  .toolbar { margin-bottom: 12px; }
  .pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
}

.tab-toolbar { margin-bottom: 12px; }
.import-result { margin-top: 8px; }
</style>
