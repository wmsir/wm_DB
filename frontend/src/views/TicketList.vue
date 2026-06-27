<template>
  <div class="ticket-list-container">
    <h2>工单中心</h2>
    <el-card shadow="hover">
      <template #header>我的工单</template>
      <el-table :data="tickets" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="工单ID" width="180" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const tickets = ref([])
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

onMounted(() => {
  loadTickets()
})
</script>

<style scoped>
.ticket-list-container {
  padding: 20px;
}
</style>
