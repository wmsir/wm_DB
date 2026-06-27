<template>
  <div class="dashboard-container">
    <h2>平台总览 (Dashboard)</h2>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-title">数据库健康评分</div>
          <div class="stat-value text-success">{{ stats.healthScore }} 分</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-title">总 SQL 数量</div>
          <div class="stat-value">{{ stats.totalSqls }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-title">风险 SQL 拦截</div>
          <div class="stat-value text-danger">{{ stats.riskSqls }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-title">审批效率</div>
          <div class="stat-value text-primary">{{ stats.approvalEfficiency }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>工单统计</template>
          <div>
            <p>总工单数：{{ stats.totalTickets }}</p>
            <p>待处理工单：{{ stats.pendingTickets }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>DBA 工作负载</template>
          <div>
            <el-progress :percentage="stats.dbaWorkload" />
            <p style="margin-top: 10px; color: #666;">系统压力较高，建议增加节点</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const stats = ref({
  healthScore: 0,
  totalSqls: 0,
  riskSqls: 0,
  totalTickets: 0,
  pendingTickets: 0,
  dbaWorkload: 0,
  approvalEfficiency: '0h'
})

const loadStats = async () => {
  try {
    const res: any = await request.get('/v1/dashboard/stats')
    stats.value = res.data
  } catch (error) {
    ElMessage.error('加载大盘数据失败')
  }
}

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}
.stat-card {
  text-align: center;
}
.stat-title {
  color: #909399;
  font-size: 14px;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 24px;
  font-weight: bold;
}
.text-success { color: #67C23A; }
.text-danger { color: #F56C6C; }
.text-primary { color: #409EFF; }
</style>
