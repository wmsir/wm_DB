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
/**
 * 审批流工单详情视图组件
 *
 * 左侧展示基于 Flowable 生成的审批时间线，右侧使用 Monaco Editor（只读模式）渲染 SQL 语句，
 * 对于含有超大附件的工单，提供带有防盗链处理的临时预签名 URL 下载按钮。
 */
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as monaco from 'monaco-editor'
import { downloadTicketAttachment } from '../api/ticket'

const route = useRoute()
const editorContainer = ref<HTMLElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null

const ticketDetail = ref<any>(null)

// 模拟 Flowable 审批流时间线数据
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

/**
 * 初始化 Monaco Editor 并设置为 SQL 只读视图
 *
 * @param sqlContent 需要渲染的 SQL 文本内容
 */
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

/**
 * 拉取工单详细信息，包含 AST 解析后的安全审查摘要
 */
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

/**
 * 触发基于 MinIO 防盗链机制的附件安全下载
 */
const downloadAttachment = async () => {
  try {
    const id = route.params.id || '1'
    const url = await downloadTicketAttachment(id as string)
    // 防盗链实现：创建临时下载链接进行拉取
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
