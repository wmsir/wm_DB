<template>
  <div class="ai-sql-review-container">
    <h2 class="page-title">AI SQL 智能审查 (DeepSeek / Qwen / OpenAI)</h2>
    <el-row :gutter="16">
      <el-col :xs="24" :sm="24" :md="12" class="col-item">
        <el-card shadow="hover" class="box-card">
          <template #header>
            <div class="card-header">
              <span>输入区 (SQL / 执行计划)</span>
            </div>
          </template>
          <div class="card-body-content">
            <el-input
              v-model="inputContent"
              type="textarea"
              :rows="12"
              placeholder="请在此粘贴您的 SQL 语句或执行计划 (EXPLAIN 输出)..."
            ></el-input>
            <div class="action-buttons">
              <el-button type="primary" :loading="loading" @click="handleAction('explain')">SQL 解释</el-button>
              <el-button type="success" :loading="loading" @click="handleAction('optimize')">SQL 优化</el-button>
              <el-button type="warning" :loading="loading" @click="handleAction('rewrite')">SQL 重写</el-button>
              <el-button type="danger" :loading="loading" @click="handleAction('risk')">风险分析</el-button>
              <el-button type="info" :loading="loading" @click="handleAction('explain-plan')">解析执行计划</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="12" class="col-item">
        <el-card shadow="hover" class="box-card">
          <template #header>
            <div class="card-header">
              <span>AI 诊断结果</span>
            </div>
          </template>
          <div class="result-area" v-loading="loading">
             <div v-if="aiResult" class="markdown-body">{{ aiResult }}</div>
             <el-empty v-else description="暂无分析结果" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const inputContent = ref('')
const aiResult = ref('')
const loading = ref(false)

const handleAction = async (action: string) => {
  if (!inputContent.value.trim()) {
    ElMessage.warning('请输入需要分析的内容')
    return
  }

  loading.value = true
  aiResult.value = ''

  try {
    const res: any = await request.post(`/v1/ai/${action}`, { sql: inputContent.value })
    aiResult.value = res.data
  } catch (error) {
    ElMessage.error('AI 服务请求失败，请检查配置')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-sql-review-container {
  width: 100%;
  box-sizing: border-box;
}

.col-item {
  margin-bottom: 16px;
}

.box-card {
  min-height: 480px;
  height: calc(100vh - 170px);
  display: flex;
  flex-direction: column;
}

:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 16px;
}

.card-body-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.action-buttons {
  margin-top: 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.result-area {
  flex: 1;
  padding: 14px;
  background-color: #f8f9fa;
  border-radius: 4px;
  overflow-y: auto;
  overflow-x: auto;
}

.markdown-body {
  white-space: pre-wrap;
  word-break: break-all;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
}
</style>
