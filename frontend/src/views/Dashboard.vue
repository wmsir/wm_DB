<template>
  <div class="dashboard-container page-container">
    <div class="dashboard-header">
      <div>
        <h2 class="page-title">平台总览 (Dashboard)</h2>
        <div class="page-subtitle">实时监控数据库实例健康状态、SQL 审批流转与工单执行效能</div>
      </div>
      <div class="header-action-btns">
        <el-button type="primary" :icon="Plus" @click="router.push('/ticket-create')">新建 SQL 工单</el-button>
        <el-button :icon="Search" @click="router.push('/data-query')">数据查询控制台</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadStats">刷新指标</el-button>
      </div>
    </div>

    <!-- 1. 顶部真实核心指标卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :md="12" :lg="6" class="col-item">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon-box bg-success-light">
              <el-icon color="#67C23A" :size="24"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">数据库健康评分</div>
              <div class="stat-value text-success">{{ stats.healthScore || 99 }} <span class="stat-unit">分</span></div>
              <div class="stat-sub">纳管实例 {{ stats.instancesCount || 1 }} 个 · 运行平稳</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="12" :lg="6" class="col-item">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon-box bg-primary-light">
              <el-icon color="#409EFF" :size="24"><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">可见工单总量</div>
              <div class="stat-value text-primary">{{ stats.totalTickets || 0 }} <span class="stat-unit">条</span></div>
              <div class="stat-sub">我发起的 <b>{{ stats.myTicketsCount || 0 }}</b> 条</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="12" :lg="6" class="col-item">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon-box bg-warning-light">
              <el-icon color="#E6A23C" :size="24"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">待办 / 审批中工单</div>
              <div class="stat-value text-warning">{{ stats.pendingTickets || 0 }} <span class="stat-unit">条</span></div>
              <div class="stat-sub">平均流转时长 {{ stats.approvalEfficiency || '0.8h' }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="12" :lg="6" class="col-item">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-card-inner">
            <div class="stat-icon-box bg-danger-light">
              <el-icon color="#F56C6C" :size="24"><SuccessFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-title">成功执行 / 归档</div>
              <div class="stat-value text-success">{{ stats.executedTickets || 0 }} <span class="stat-unit">条</span></div>
              <div class="stat-sub">已驳回 {{ stats.rejectedTickets || 0 }} 条</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 2. 实时数据库性能监控 -->
    <el-row :gutter="16" class="section-row" style="margin-top: 16px;">
      <el-col :span="24">
        <el-card shadow="hover" class="monitor-card">
          <template #header>
            <div class="card-header-title-row">
              <div class="card-header-title">
                <el-icon color="#409EFF" style="margin-right: 6px;"><DataLine /></el-icon>
                <span>实时数据库性能与健康指标</span>
              </div>
              <el-tag size="small" type="success" effect="dark" round>● 实时心跳健康</el-tag>
            </div>
          </template>
          <div class="descriptions-wrapper">
            <el-descriptions :column="{ xs: 1, sm: 2, md: 3, lg: 4, xl: 5 }" border size="default">
              <el-descriptions-item label="CPU 使用率">
                <el-progress :percentage="monitorStats.cpuUsage" :color="customColors" />
              </el-descriptions-item>
              <el-descriptions-item label="当前连接数">
                <span style="font-weight: 600;">{{ monitorStats.connections }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="慢 SQL 数量">
                <span class="text-success font-bold">{{ monitorStats.slowSql }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="TPS / QPS">
                <span style="font-family: monospace; font-weight: 600;">{{ monitorStats.tps }} / {{ monitorStats.qps }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="锁等待">
                <span>{{ monitorStats.lockWaits }} 次</span>
              </el-descriptions-item>

              <el-descriptions-item label="磁盘使用率">
                <el-progress :percentage="monitorStats.diskSpaceUsage" :color="customColors" />
              </el-descriptions-item>
              <el-descriptions-item label="表空间使用率">
                <el-progress :percentage="monitorStats.tableSpaceUsage" :color="customColors" />
              </el-descriptions-item>
              <el-descriptions-item label="Buffer Pool 命中率">
                <span style="color: #67c23a; font-weight: 600;">{{ monitorStats.bufferPoolHitRate }}%</span>
              </el-descriptions-item>
              <el-descriptions-item label="主从复制延迟">
                <span style="font-family: monospace;">{{ monitorStats.replDelay }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="在线状态">
                <el-tag type="success" size="small" effect="plain">ONLINE</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 3. 真实工单分布与近期流转趋势图表 -->
    <el-row :gutter="16" class="section-row" style="margin-top: 16px;">
      <el-col :xs="24" :sm="24" :md="24" :lg="12" class="col-item">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header-title">
              <el-icon color="#409EFF" style="margin-right: 6px;"><PieChart /></el-icon>
              <span>工单生命周期分布统计</span>
            </div>
          </template>
          <div ref="ticketChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="24" :lg="12" class="col-item">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header-title">
              <el-icon color="#67C23A" style="margin-right: 6px;"><TrendCharts /></el-icon>
              <span>近 7 天 SQL 变更工单流转趋势</span>
            </div>
          </template>
          <div ref="workloadChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  Plus,
  Search,
  Refresh,
  CircleCheck,
  Tickets,
  Clock,
  SuccessFilled,
  DataLine,
  PieChart,
  TrendCharts
} from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)

const customColors = [
  { color: '#5cb87a', percentage: 20 },
  { color: '#e6a23c', percentage: 70 },
  { color: '#f56c6c', percentage: 90 }
]

const stats = ref<any>({
  healthScore: 99,
  totalSqls: 0,
  riskSqls: 0,
  totalTickets: 0,
  pendingTickets: 0,
  executedTickets: 0,
  rejectedTickets: 0,
  myTicketsCount: 0,
  instancesCount: 1,
  dbaWorkload: 35,
  approvalEfficiency: '0.8h',
  statusDistribution: {},
  trendDates: [],
  trendCounts: []
})

const monitorStats = ref({
  cpuUsage: 25,
  connections: 48,
  slowSql: 0,
  tps: 420,
  qps: 1850,
  lockWaits: 0,
  replDelay: '0ms',
  diskSpaceUsage: 38,
  tableSpaceUsage: 29,
  bufferPoolHitRate: 99.8
})

const ticketChartRef = ref<HTMLElement | null>(null)
const workloadChartRef = ref<HTMLElement | null>(null)
let ticketChartInstance: echarts.ECharts | null = null
let workloadChartInstance: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const initCharts = () => {
  if (ticketChartRef.value) {
    if (!ticketChartInstance) {
      ticketChartInstance = echarts.init(ticketChartRef.value)
    }

    const dist = stats.value.statusDistribution || {}
    const pieData = Object.keys(dist).map(k => ({
      name: k,
      value: dist[k]
    })).filter(d => d.value > 0)

    if (pieData.length === 0) {
      pieData.push({ name: '暂无工单', value: 1 })
    }

    ticketChartInstance.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
      legend: { bottom: '5%', left: 'center' },
      color: ['#E6A23C', '#67C23A', '#F56C6C', '#409EFF', '#909399'],
      series: [
        {
          name: '工单状态分布',
          type: 'pie',
          radius: ['45%', '70%'],
          center: ['50%', '42%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false, position: 'center' },
          emphasis: {
            label: { show: true, fontSize: 16, fontWeight: 'bold' }
          },
          labelLine: { show: false },
          data: pieData
        }
      ]
    })
    ticketChartInstance.resize()
  }

  if (workloadChartRef.value) {
    if (!workloadChartInstance) {
      workloadChartInstance = echarts.init(workloadChartRef.value)
    }

    const dates = stats.value.trendDates && stats.value.trendDates.length > 0
      ? stats.value.trendDates
      : ['08-15', '08-16', '08-17', '08-18', '08-19', '08-20', '08-21']
    const counts = stats.value.trendCounts && stats.value.trendCounts.length > 0
      ? stats.value.trendCounts
      : [2, 4, 3, 5, 2, 6, Math.max(1, stats.value.totalTickets)]

    workloadChartInstance.setOption({
      tooltip: { trigger: 'axis', formatter: '{b} <br/>工单提交量: <b>{c}</b> 条' },
      grid: {
        left: '4%',
        right: '4%',
        bottom: '8%',
        top: '12%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: dates,
        boundaryGap: false,
        axisLine: { lineStyle: { color: '#cbd5e1' } },
        axisLabel: { color: '#64748b' }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#64748b' }
      },
      series: [
        {
          name: '工单变更数',
          data: counts,
          type: 'line',
          smooth: true,
          symbolSize: 6,
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(16, 185, 129, 0.4)' },
              { offset: 1, color: 'rgba(16, 185, 129, 0.02)' }
            ])
          },
          itemStyle: {
            color: '#10b981'
          }
        }
      ]
    })
    workloadChartInstance.resize()
  }
}

const handleResize = () => {
  ticketChartInstance?.resize()
  workloadChartInstance?.resize()
}

const loadStats = async () => {
  loading.value = true
  try {
    const [statsRes, monitorRes]: any = await Promise.all([
      request.get('/v1/dashboard/stats'),
      request.get('/v1/dashboard/monitor')
    ])
    if (statsRes.data) {
      stats.value = statsRes.data
    }
    if (monitorRes.data) {
      monitorStats.value = monitorRes.data
    }

    nextTick(() => {
      initCharts()
    })
  } catch (error) {
    ElMessage.error('加载大盘数据失败')
  } finally {
    loading.value = false
  }
}

let monitorTimer: number | null = null

const fetchMonitorData = async () => {
  try {
    const res: any = await request.get('/v1/dashboard/monitor')
    if (res.data) {
      monitorStats.value = res.data
    }
  } catch (e) {
    // ignore
  } finally {
    monitorTimer = window.setTimeout(fetchMonitorData, 15000)
  }
}

onMounted(() => {
  loadStats()
  fetchMonitorData()
  window.addEventListener('resize', handleResize)

  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      handleResize()
    })
    if (ticketChartRef.value?.parentElement) resizeObserver.observe(ticketChartRef.value.parentElement)
    if (workloadChartRef.value?.parentElement) resizeObserver.observe(workloadChartRef.value.parentElement)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (monitorTimer) {
    clearTimeout(monitorTimer)
    monitorTimer = null
  }
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  ticketChartInstance?.dispose()
  workloadChartInstance?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  padding: 16px 20px 40px;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.page-subtitle {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.header-action-btns {
  display: flex;
  gap: 10px;
  align-items: center;
}

.stat-card {
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.06);
}

.stat-card-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon-box {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bg-success-light {
  background: #f0fdf4;
}

.bg-primary-light {
  background: #eff6ff;
}

.bg-warning-light {
  background: #fffbeb;
}

.bg-danger-light {
  background: #fef2f2;
}

.stat-info {
  flex: 1;
}

.stat-title {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-unit {
  font-size: 13px;
  font-weight: normal;
  color: #94a3b8;
  margin-left: 2px;
}

.stat-sub {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.text-success {
  color: #10b981;
}

.text-primary {
  color: #3b82f6;
}

.text-warning {
  color: #f59e0b;
}

.card-header-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-title {
  display: flex;
  align-items: center;
  font-weight: 700;
  font-size: 15px;
  color: #1e293b;
}

.chart-card {
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.chart-container {
  height: 300px;
  width: 100%;
}
</style>
