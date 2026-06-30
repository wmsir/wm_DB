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
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header>实时数据库监控</template>
          <el-descriptions :column="5" border size="small">
            <el-descriptions-item label="CPU 使用率">
              <el-progress :percentage="monitorStats.cpuUsage" :color="customColors" />
            </el-descriptions-item>
            <el-descriptions-item label="当前连接数">{{ monitorStats.connections }}</el-descriptions-item>
            <el-descriptions-item label="慢 SQL 数量">
               <span class="text-danger">{{ monitorStats.slowSql }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="TPS / QPS">{{ monitorStats.tps }} / {{ monitorStats.qps }}</el-descriptions-item>
            <el-descriptions-item label="锁等待">{{ monitorStats.lockWaits }}</el-descriptions-item>

            <el-descriptions-item label="磁盘使用率">
              <el-progress :percentage="monitorStats.diskSpaceUsage" :color="customColors" />
            </el-descriptions-item>
            <el-descriptions-item label="表空间使用率">
              <el-progress :percentage="monitorStats.tableSpaceUsage" :color="customColors" />
            </el-descriptions-item>
            <el-descriptions-item label="Buffer Pool 命中率">{{ monitorStats.bufferPoolHitRate }}%</el-descriptions-item>
            <el-descriptions-item label="复制延迟">{{ monitorStats.replDelay }}</el-descriptions-item>
            <el-descriptions-item label="在线状态"><el-tag type="success">RUNNING</el-tag></el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>工单分布统计</template>
          <div ref="ticketChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>DBA 工作负载趋势</template>
          <div ref="workloadChartRef" style="width: 100%; height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

const customColors = [
  { color: '#5cb87a', percentage: 20 },
  { color: '#e6a23c', percentage: 70 },
  { color: '#f56c6c', percentage: 90 },
]

const stats = ref({
  healthScore: 0,
  totalSqls: 0,
  riskSqls: 0,
  totalTickets: 0,
  pendingTickets: 0,
  dbaWorkload: 0,
  approvalEfficiency: '0h'
})

const monitorStats = ref({
  cpuUsage: 0,
  connections: 0,
  slowSql: 0,
  tps: 0,
  qps: 0,
  lockWaits: 0,
  replDelay: '0ms',
  diskSpaceUsage: 0,
  tableSpaceUsage: 0,
  bufferPoolHitRate: 0
})

const ticketChartRef = ref<HTMLElement | null>(null)
const workloadChartRef = ref<HTMLElement | null>(null)

const initCharts = () => {
  if (ticketChartRef.value) {
    const ticketChart = echarts.init(ticketChartRef.value)
    ticketChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { top: '5%', left: 'center' },
      series: [
        {
          name: '工单状态',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          label: { show: false, position: 'center' },
          emphasis: {
            label: { show: true, fontSize: 20, fontWeight: 'bold' }
          },
          labelLine: { show: false },
          data: [
            { value: stats.value.totalTickets - stats.value.pendingTickets, name: '已处理' },
            { value: stats.value.pendingTickets, name: '待处理' }
          ]
        }
      ]
    })
  }

  if (workloadChartRef.value) {
    const workloadChart = echarts.init(workloadChartRef.value)
    workloadChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
      },
      yAxis: {
        type: 'value',
        max: 100
      },
      series: [
        {
          data: [20, 32, 41, stats.value.dbaWorkload, 60, 45, 30],
          type: 'line',
          smooth: true,
          areaStyle: {}
        }
      ]
    })
  }
}

const handleResize = () => {
  if (ticketChartRef.value) echarts.getInstanceByDom(ticketChartRef.value)?.resize()
  if (workloadChartRef.value) echarts.getInstanceByDom(workloadChartRef.value)?.resize()
}

const loadStats = async () => {
  try {
    const [statsRes, monitorRes]: any = await Promise.all([
      request.get('/v1/dashboard/stats'),
      request.get('/v1/dashboard/monitor')
    ])
    stats.value = statsRes.data
    monitorStats.value = monitorRes.data

    nextTick(() => {
      initCharts()
    })
  } catch (error) {
    ElMessage.error('加载大盘数据失败')
  }
}

let monitorTimer: number | null = null

const fetchMonitorData = async () => {
  try {
    const res: any = await request.get('/v1/dashboard/monitor')
    monitorStats.value = res.data
  } catch(e) {
    console.warn('监控数据获取失败, 下次重试', e)
  } finally {
    // 确保无论成功失败，当前请求完成后才开始下一个 10 秒倒计时，避免请求堆积
    monitorTimer = window.setTimeout(fetchMonitorData, 10000)
  }
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
  // 启动安全轮询
  monitorTimer = window.setTimeout(fetchMonitorData, 10000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (monitorTimer) clearTimeout(monitorTimer)
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
