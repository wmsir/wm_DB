<template>
  <div class="ticket-detail">
    <div class="top-actions">
      <el-button>ODS拦截</el-button>
      <el-button>查看提交信息</el-button>
      <el-button type="primary" @click="downloadAttachment" v-if="ticketDetail?.detail?.attachmentOssKey">下载备份SQL</el-button>
    </div>

    <div class="section">
      <div class="section-title">审批流</div>
      <div class="approval-flow">
        西安非车甲方PM ➔ 非车运维PM ➔ 数据库PM ➔ DBA
      </div>
    </div>

    <div class="section">
      <div class="section-title">其他信息</div>
      <el-table :data="ticketInfoList" border style="width: 100%">
        <el-table-column prop="applicant" label="发起人" />
        <el-table-column prop="instance" label="目标实例" />
        <el-table-column prop="database" label="数据库" />
        <el-table-column prop="startTime" label="发起时间" />
        <el-table-column prop="executeWindow" label="可执行时间范围" width="300">
           <template #default="{ row }">
             <span style="color: #F56C6C;">{{ row.executeWindow }}</span>
           </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" />
        <el-table-column prop="backup" label="备份" width="60" />
        <el-table-column prop="type" label="工单类型" />
        <el-table-column prop="status" label="当前状态">
           <template #default="{ row }">
             <span style="color: #67C23A; font-weight: bold;">{{ row.status }}</span>
           </template>
        </el-table-column>
        <el-table-column prop="group" label="组" />
        <el-table-column prop="sqlType" label="SQL类型" />
      </el-table>
    </div>

    <div class="section">
      <div class="section-title">执行说明</div>
      <div class="execution-desc">
        {{ ticketDetail?.ticket?.reason || '-' }}
      </div>
    </div>

    <div class="tabs-section">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="工单详情" name="details">
          <div class="table-actions-bar">
            <el-button size="small">展开全部</el-button>
            <div class="right-actions">
              <el-input v-model="searchQuery" placeholder="搜索" size="small" style="width: 200px;" />
              <el-button size="small" :icon="Refresh"></el-button>
              <el-button size="small" :icon="Menu"></el-button>
            </div>
          </div>
          <el-table :data="filteredSqlList" style="width: 100%" size="small" border>
            <el-table-column type="expand">
              <template #default="props">
                <div style="padding: 10px;">{{ props.row.sqlContent }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="sqlContent" label="SQL内容" show-overflow-tooltip />
            <el-table-column prop="status" label="审核/执行状态" width="120" />
            <el-table-column prop="message" label="审核/执行信息" width="120" />
            <el-table-column prop="affectedRows" label="扫描/影响行数" width="120" />
            <el-table-column prop="executionTime" label="执行耗时" width="100" />
            <el-table-column prop="backupTime" label="备份耗时" width="100" />
            <el-table-column prop="currentStage" label="当前阶段" width="100" />
            <el-table-column prop="actions" label="操作" width="80" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="工单日志" name="logs">
          <div style="padding: 20px;">暂无日志</div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Menu } from '@element-plus/icons-vue'
import request from '../utils/request'
import { downloadTicketAttachment } from '../api/ticket'

const route = useRoute()
const activeTab = ref('details')
const searchQuery = ref('')
const ticketDetail = ref<any>(null)

const fetchTicketDetail = async () => {
  try {
    const id = route.params.id || '1'
    const response: any = await request.get(`/v1/ticket/${id}/detail`)
    ticketDetail.value = response.data

    if (!ticketDetail.value || !ticketDetail.value.ticket) {
      throw new Error('No data')
    }
  } catch (error) {
    ElMessage.error('获取详情失败，请检查工单 ID 或权限')
    // Prevent complete blank page by initializing with empty structure
    ticketDetail.value = {
      ticket: {},
      detail: {}
    }
  }
}

const ticketInfoList = computed(() => {
  if (!ticketDetail.value || !ticketDetail.value.ticket) return []
  const t = ticketDetail.value.ticket
  return [
    {
      applicant: t.applicantIdCard || '-',
      instance: t.instanceId || '-',
      database: '-',
      startTime: '-',
      executeWindow: t.executionWindow || '-',
      endTime: '-',
      backup: '-',
      type: t.type || '-',
      status: t.status === 'EXECUTED' ? '已正常结束' : (t.status || '-'),
      group: '-',
      sqlType: '-'
    }
  ]
})

const sqlList = computed(() => {
  if (!ticketDetail.value || !ticketDetail.value.detail || !ticketDetail.value.detail.sqlText) return []
  const text = ticketDetail.value.detail.sqlText
  const queries = text.split(';').map((s: string) => s.trim()).filter((s: string) => s.length > 0)

  return queries.map((query: string, index: number) => {
    return {
      id: index + 1,
      sqlContent: query,
      status: '-',
      message: '-',
      affectedRows: '-',
      executionTime: '-',
      backupTime: '-',
      currentStage: '-',
      actions: '-'
    }
  })
})

const filteredSqlList = computed(() => {
  if (!searchQuery.value) return sqlList.value
  return sqlList.value.filter((item: any) =>
    item.sqlContent.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const downloadAttachment = async () => {
  try {
    const id = route.params.id || '1'
    const url = await downloadTicketAttachment(id as string)
    const link = document.createElement('a')
    link.href = url
    link.target = '_blank'
    link.download = ticketDetail.value?.detail?.attachmentOssKey || 'backup.sql'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    ElMessage.error('获取附件下载链接失败')
  }
}

onMounted(() => {
  fetchTicketDetail()
})
</script>

<style scoped>
.ticket-detail {
  padding: 20px;
  background-color: #fff;
  min-height: 100vh;
}

.top-actions {
  margin-bottom: 20px;
}

.section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 10px;
}

.approval-flow {
  font-size: 14px;
  color: #333;
}

.execution-desc {
  background-color: #f2f2f2;
  padding: 15px;
  border-radius: 4px;
  min-height: 60px;
  color: #666;
}

.tabs-section {
  margin-top: 20px;
}

.table-actions-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.right-actions {
  display: flex;
  gap: 10px;
}

:deep(.el-table th.el-table__cell) {
  background-color: #f5f7fa;
}
</style>
