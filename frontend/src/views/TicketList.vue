<template>
  <div class="ticket-list-container">
    <div class="header-action">
      <h2>工单中心</h2>
      <el-button type="primary" @click="showNewTicketDialog = true">创建工单</el-button>
    </div>

    <el-card shadow="hover">
      <template #header>我的工单</template>
      <el-table :data="tickets" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="工单ID" width="180" />
        <el-table-column prop="type" label="工单类型" width="150">
          <template #default="{ row }">
             <el-tag effect="plain" type="info">{{ getTicketTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="instanceId" label="目标实例ID" width="120" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建工单对话框 -->
    <el-dialog v-model="showNewTicketDialog" title="创建工单" width="500px">
      <el-form :model="ticketForm" label-width="120px">
        <el-form-item label="目标实例ID" required>
          <el-input v-model="ticketForm.instanceId" placeholder="请输入数据库实例ID"></el-input>
        </el-form-item>
        <el-form-item label="工单类型" required>
          <el-select v-model="ticketForm.type" placeholder="请选择工单类型">
            <el-option label="SQL 审核 (DDL/DML)" value="SQL_AUDIT" />
            <el-option label="数据导出" value="DATA_EXPORT" />
            <el-option label="权限申请" value="PERMISSION" />
            <el-option label="账号申请" value="ACCOUNT" />
            <el-option label="库表申请" value="DB_TABLE" />
            <el-option label="数据恢复" value="DATA_RECOVERY" />
          </el-select>
        </el-form-item>
        <el-form-item label="附件 (SQL等)" v-if="ticketForm.type === 'SQL_AUDIT' || ticketForm.type === 'DATA_RECOVERY'">
           <el-upload
             class="upload-demo"
             action="#"
             :auto-upload="false"
             :on-change="handleFileChange"
             :limit="1"
           >
             <el-button type="primary">选择文件</el-button>
           </el-upload>
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input type="textarea" v-model="ticketForm.reason" placeholder="请简述申请原因"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showNewTicketDialog = false">取消</el-button>
          <el-button type="primary" @click="submitTicket" :loading="submitLoading">提交审批</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const tickets = ref([])
const showNewTicketDialog = ref(false)
const submitLoading = ref(false)
const ticketForm = ref({
  instanceId: '',
  type: 'SQL_AUDIT',
  reason: '',
  file: null as File | null
})
const loading = ref(false)
const router = useRouter()

const loadTickets = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/v1/ticket/list')
    tickets.value = res.data
  } catch (error) {
    ElMessage.error('加载工单列表失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (id: number) => {
  router.push(`/ticket/${id}`)
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'AUDITING': return 'warning'
    case 'REJECTED': return 'danger'
    default: return 'info'
  }
}

const getTicketTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    'SQL_AUDIT': 'SQL 审核',
    'DATA_EXPORT': '数据导出',
    'PERMISSION': '权限申请',
    'ACCOUNT': '账号申请',
    'DB_TABLE': '库表申请',
    'DATA_RECOVERY': '数据恢复'
  }
  return map[type] || type || 'SQL 审核'
}

const handleFileChange = (file: any) => {
  ticketForm.value.file = file.raw
}

const submitTicket = async () => {
  if (!ticketForm.value.instanceId) {
    ElMessage.warning('请输入目标实例ID')
    return
  }

  submitLoading.value = true
  try {
    const formData = new FormData()
    formData.append('instanceId', ticketForm.value.instanceId)
    formData.append('type', ticketForm.value.type)
    if (ticketForm.value.reason) {
      formData.append('reason', ticketForm.value.reason)
    }

    // 如果是 SQL 审核类，则附加文件。此处复用后端的 `/api/v1/ticket/submit` 逻辑
    if (ticketForm.value.file) {
      formData.append('file', ticketForm.value.file)
    } else {
      // 模拟一个空的占位文件，以便通过后端的 MultipartFile 校验
      const blob = new Blob(['-- 暂无附件'], { type: 'text/plain' })
      formData.append('file', blob, 'empty.sql')
    }

    await request.post('/v1/ticket/submit', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    ElMessage.success('工单提交成功')
    showNewTicketDialog.value = false
    loadTickets()
  } catch (error) {
    // 错误在 request 拦截器中已经提示
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadTickets()
})
</script>

<style scoped>
.ticket-list-container {
  padding: 20px;
}
.header-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.header-action h2 {
  margin: 0;
}
</style>
