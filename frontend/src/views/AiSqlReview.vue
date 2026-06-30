<template>
  <div class="ai-sql-review-container">
    <h2>AI SQL 智能审查 (DeepSeek / Qwen / OpenAI)</h2>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover" class="box-card">
          <template #header>
            <div class="card-header">
              <span>输入区 (SQL / 执行计划)</span>
            </div>
          </template>
          <el-input
            v-model="inputContent"
            type="textarea"
            :rows="15"
            placeholder="请在此粘贴您的 SQL 语句或执行计划 (EXPLAIN 输出)..."
          ></el-input>
          <div class="action-buttons">
            <el-button type="primary" :loading="loading" @click="handleAction('explain')">SQL 解释</el-button>
            <el-button type="success" :loading="loading" @click="handleAction('optimize')">SQL 优化</el-button>
            <el-button type="warning" :loading="loading" @click="handleAction('rewrite')">SQL 重写</el-button>
            <el-button type="danger" :loading="loading" @click="handleAction('risk')">风险分析</el-button>
            <el-button type="info" :loading="loading" @click="handleAction('explain-plan')">解析执行计划</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="box-card">
          <template #header>
            <div class="card-header">
              <span>AI 诊断结果</span>
            </div>
          </template>
          <div class="result-area" v-loading="loading">
             <div v-if="aiResult" class="markdown-body" style="white-space: pre-wrap; font-family: monospace;">{{ aiResult }}</div>
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
  padding: 20px;
}
.box-card {
  height: calc(100vh - 150px);
  display: flex;
  flex-direction: column;
}
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: auto;
}
.action-buttons {
  margin-top: 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.result-area {
  flex: 1;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;
  overflow-y: auto;
}
</style>
