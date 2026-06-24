<template>
  <div class="ticket-detail">
    <div class="layout-left">
      <el-card class="timeline-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>审批时间线</span>
          </div>
        </template>
        <el-timeline>
          <el-timeline-item
            v-for="(activity, index) in activities"
            :key="index"
            :timestamp="activity.timestamp"
            :type="activity.type"
          >
            {{ activity.content }}
          </el-timeline-item>
        </el-timeline>
      </el-card>
    </div>

    <div class="layout-right">
      <el-card class="editor-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>SQL 审核内容</span>
            <el-button
              v-if="ticketDetail?.attachmentOssKey"
              type="primary"
              size="small"
              @click="downloadAttachment"
            >
              下载完整附件
            </el-button>
          </div>
        </template>
        <div ref="editorContainer" class="monaco-editor-container"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as monaco from 'monaco-editor'
import { downloadTicketAttachment } from '../api/ticket'

const route = useRoute()
const editorContainer = ref<HTMLElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null

const ticketDetail = ref<any>(null)

// Mock Flowable timeline data
const activities = ref([
  {
    content: '工单草稿创建',
    timestamp: '2023-10-25 10:00:00',
    type: 'info'
  },
  {
    content: '提交审批 (DBA审核中)',
    timestamp: '2023-10-25 10:05:00',
    type: 'primary'
  }
])

const initMonaco = (sqlContent: string) => {
  if (editorContainer.value) {
    editor = monaco.editor.create(editorContainer.value, {
      value: sqlContent,
      language: 'sql',
      theme: 'vs-dark',
      readOnly: true,
      minimap: { enabled: false },
      automaticLayout: true
    })
  }
}

const fetchTicketDetail = async () => {
  try {
    // const id = route.params.id || '1' // fallback for demo
    // const response = await axios.get(`/api/v1/ticket/${id}/detail`)
    // ticketDetail.value = response.data.detail
    // For demo, we mock the response
    ticketDetail.value = {
      sqlText: "SELECT * FROM users WHERE status = 'ACTIVE';\n-- Some other SQLs\nUPDATE products SET price = price * 1.1 WHERE category = 'ELEC';",
      attachmentOssKey: 'mock-uuid-large-file.sql'
    }

    initMonaco(ticketDetail.value.sqlText)
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const downloadAttachment = async () => {
  try {
    const id = route.params.id || '1'
    const url = await downloadTicketAttachment(id as string)
    // Anti-hotlinking implementation: create a temporary anchor element
    const link = document.createElement('a')
    link.href = url
    link.target = '_blank'
    link.download = ticketDetail.value.attachmentOssKey
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

onBeforeUnmount(() => {
  if (editor) {
    editor.dispose()
  }
})
</script>

<style scoped>
.ticket-detail {
  display: flex;
  height: 100vh;
  padding: 20px;
  box-sizing: border-box;
  background-color: #f5f7fa;
  gap: 20px;
}

.layout-left {
  flex: 0 0 300px;
}

.layout-right {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.timeline-card, .editor-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.monaco-editor-container {
  flex: 1;
  min-height: 500px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

/* Ensure the el-card body takes up remaining space */
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: auto;
}
</style>
