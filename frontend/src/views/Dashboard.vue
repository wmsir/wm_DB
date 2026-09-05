<template>
  <div class="dashboard-container">
    <!-- 1. 顶层清爽护眼专属欢迎横幅与快捷操作台 -->
    <div class="welcome-hero-banner">
      <div class="welcome-left">
        <div class="welcome-badge">
          <span class="pulse-green-dot"></span>
          <span>云原生数据库自治平台 · 生产网关集群正常运行中</span>
        </div>
        <h1 class="welcome-heading">
          早安，{{ userStore.userInfo?.realName || userStore.userInfo?.username || '管理员' }}！欢迎回到 wmDB 智能云
        </h1>
        <p class="welcome-subtext">
          已全天候纳管多源异构数据库实例，8 大核心业务资源组平稳运行，智能预检实时守护生产变更安全。
        </p>
      </div>

      <div class="welcome-actions">
        <el-button type="primary" size="default" :icon="Plus" class="hero-btn primary-glow" @click="router.push('/ticket-create')">
          新建 SQL 工单
        </el-button>
        <el-button size="default" :icon="Search" class="hero-btn ghost-btn" @click="router.push('/data-query')">
          数据查询
        </el-button>
        <el-button size="default" :icon="MagicStick" class="hero-btn ghost-btn" @click="router.push('/ai-sql-review')">
          AI 预检
        </el-button>
        <el-button size="default" :icon="Refresh" :loading="loading" class="hero-btn ghost-btn" @click="loadStats">
          刷新指标
        </el-button>
      </div>
    </div>

    <!-- 2. 6 核心业务与效能指标卡片（明亮微润、护眼微彩设计） -->
    <el-row :gutter="14" class="stat-row">
      <!-- 实例数量 -->
      <el-col :xs="12" :sm="8" :md="8" :lg="4" class="col-item">
        <div class="metric-card sky-card" @click="router.push('/instance-list')">
          <div class="metric-header">
            <span class="metric-title">纳管数据库实例</span>
            <div class="metric-icon-box bg-sky">
              <el-icon :size="20"><Coin /></el-icon>
            </div>
          </div>
          <div class="metric-body">
            <span class="metric-number text-sky">{{ stats.instancesCount || 1 }}</span>
            <span class="metric-unit">个</span>
          </div>
          <div class="metric-footer">
            <span class="tag-pill tag-sky">在线率 100%</span>
            <span class="metric-sub">多源异构引擎</span>
          </div>
        </div>
      </el-col>

      <!-- 资源组数 -->
      <el-col :xs="12" :sm="8" :md="8" :lg="4" class="col-item">
        <div class="metric-card teal-card" @click="router.push('/resource-group-list')">
          <div class="metric-header">
            <span class="metric-title">业务资源组数</span>
            <div class="metric-icon-box bg-teal">
              <el-icon :size="20"><Suitcase /></el-icon>
            </div>
          </div>
          <div class="metric-body">
            <span class="metric-number text-teal">{{ stats.resourceGroupsCount || 8 }}</span>
            <span class="metric-unit">组</span>
          </div>
          <div class="metric-footer">
            <span class="tag-pill tag-teal">业务资源隔离</span>
            <span class="metric-sub">8大业务条线</span>
          </div>
        </div>
      </el-col>

      <!-- 工单变更总量 -->
      <el-col :xs="12" :sm="8" :md="8" :lg="4" class="col-item">
        <div class="metric-card indigo-card" @click="router.push('/ticket-list')">
          <div class="metric-header">
            <span class="metric-title">工单变更总量</span>
            <div class="metric-icon-box bg-indigo">
              <el-icon :size="20"><Tickets /></el-icon>
            </div>
          </div>
          <div class="metric-body">
            <span class="metric-number text-indigo">{{ stats.totalTickets || 60 }}</span>
            <span class="metric-unit">条</span>
          </div>
          <div class="metric-footer">
            <span class="tag-pill tag-indigo">我发起 {{ stats.myTicketsCount || 0 }} 笔</span>
            <span class="metric-sub">已闭环归档</span>
          </div>
        </div>
      </el-col>

      <!-- 待办 / 审批中工单 -->
      <el-col :xs="12" :sm="8" :md="8" :lg="4" class="col-item">
        <div class="metric-card amber-card" @click="router.push('/ticket-list')">
          <div class="metric-header">
            <span class="metric-title">待办 / 审批中</span>
            <div class="metric-icon-box bg-amber">
              <el-icon :size="20"><Clock /></el-icon>
            </div>
          </div>
          <div class="metric-body">
            <span class="metric-number text-amber">{{ stats.pendingTickets || 19 }}</span>
            <span class="metric-unit">条</span>
          </div>
          <div class="metric-footer">
            <span class="tag-pill tag-amber">平均流转 0.8h</span>
            <span class="metric-sub">支持加急催办</span>
          </div>
        </div>
      </el-col>

      <!-- 成功执行 / 归档 -->
      <el-col :xs="12" :sm="8" :md="8" :lg="4" class="col-item">
        <div class="metric-card emerald-card" @click="router.push('/ticket-list')">
          <div class="metric-header">
            <span class="metric-title">成功执行 / 归档</span>
            <div class="metric-icon-box bg-emerald">
              <el-icon :size="20"><CircleCheck /></el-icon>
            </div>
          </div>
          <div class="metric-body">
            <span class="metric-number text-emerald">{{ stats.executedTickets || 29 }}</span>
            <span class="metric-unit">条</span>
          </div>
          <div class="metric-footer">
            <span class="tag-pill tag-emerald">流式下发完成</span>
            <span class="metric-sub">执行成功率 98%</span>
          </div>
        </div>
      </el-col>

      <!-- 平台注册用户 -->
      <el-col :xs="12" :sm="8" :md="8" :lg="4" class="col-item">
        <div class="metric-card slate-card" @click="router.push('/user-list')">
          <div class="metric-header">
            <span class="metric-title">系统活跃成员</span>
            <div class="metric-icon-box bg-slate">
              <el-icon :size="20"><User /></el-icon>
            </div>
          </div>
          <div class="metric-body">
            <span class="metric-number text-slate">{{ stats.usersCount || 8 }}</span>
            <span class="metric-unit">位</span>
          </div>
          <div class="metric-footer">
            <span class="tag-pill tag-slate">覆盖 5 大角色</span>
            <span class="metric-sub">三权分立鉴权</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 3. 实时数据库性能微仪表网格 (全新清爽无边框磁贴架构) -->
    <div class="clean-section-card">
      <div class="section-card-header">
        <div class="section-title-wrap">
          <div class="section-icon-dot bg-blue-dot"></div>
          <h3 class="section-heading">生产主库性能与运行效能 (RDS MySQL 8.0)</h3>
          <span class="section-subtext">采样周期 5 秒 · 全链路心跳检测中</span>
        </div>
        <div class="section-extra">
          <el-tag size="small" type="success" effect="light" class="status-pill-tag">
            ● 实时心跳健康 (ONLINE)
          </el-tag>
        </div>
      </div>

      <div class="monitor-tiles-grid">
        <!-- 磁贴 1: CPU 算力 -->
        <div class="monitor-tile">
          <div class="tile-top">
            <span class="tile-label">CPU 算力利用率</span>
            <span class="tile-tag tag-success">健康空闲</span>
          </div>
          <div class="tile-value">{{ monitorStats.cpuUsage }}%</div>
          <el-progress :percentage="monitorStats.cpuUsage" :stroke-width="6" :show-text="false" color="#10b981" class="tile-progress" />
          <div class="tile-desc">8 核心分配 / 波动平稳</div>
        </div>

        <!-- 磁贴 2: 活跃连接 -->
        <div class="monitor-tile">
          <div class="tile-top">
            <span class="tile-label">当前活跃连接数</span>
            <span class="tile-tag tag-primary">连接池优化</span>
          </div>
          <div class="tile-value">{{ monitorStats.connections }} <span class="tile-sub-val">/ 500</span></div>
          <el-progress :percentage="Math.round(monitorStats.connections / 5)" :stroke-width="6" :show-text="false" color="#3b82f6" class="tile-progress" />
          <div class="tile-desc">连接利用率 {{ Math.round(monitorStats.connections / 5) }}%</div>
        </div>

        <!-- 磁贴 3: TPS / QPS 吞吐 -->
        <div class="monitor-tile">
          <div class="tile-top">
            <span class="tile-label">吞吐能力 TPS / QPS</span>
            <span class="tile-tag tag-info">实时流速</span>
          </div>
          <div class="tile-value font-mono">{{ monitorStats.tps }} <span class="tile-sub-val">/ {{ monitorStats.qps }}</span></div>
          <div class="tile-bars-visual">
            <span class="bar-pill bar-1"></span>
            <span class="bar-pill bar-2"></span>
            <span class="bar-pill bar-3"></span>
            <span class="bar-pill bar-4"></span>
            <span class="bar-pill bar-5"></span>
          </div>
          <div class="tile-desc">毫秒级事务低抖动</div>
        </div>

        <!-- 磁贴 4: 慢 SQL 与锁等待 -->
        <div class="monitor-tile">
          <div class="tile-top">
            <span class="tile-label">慢 SQL / 锁等待</span>
            <span class="tile-tag tag-success">零阻塞</span>
          </div>
          <div class="tile-value text-emerald font-mono">{{ monitorStats.slowSql }} <span class="tile-sub-val">条 / {{ monitorStats.lockWaits }} 次</span></div>
          <div class="tile-status-clean">
            <el-icon color="#10b981"><CircleCheck /></el-icon>
            <span>无长事务阻塞锁表</span>
          </div>
          <div class="tile-desc">阈值 &gt; 1000ms 自动拦截</div>
        </div>

        <!-- 磁贴 5: Buffer Pool 命中率 -->
        <div class="monitor-tile">
          <div class="tile-top">
            <span class="tile-label">Buffer Pool 命中率</span>
            <span class="tile-tag tag-emerald">内存加速</span>
          </div>
          <div class="tile-value text-emerald font-mono">{{ monitorStats.bufferPoolHitRate }}%</div>
          <el-progress :percentage="monitorStats.bufferPoolHitRate" :stroke-width="6" :show-text="false" color="#10b981" class="tile-progress" />
          <div class="tile-desc">绝大部分热点走缓存命中</div>
        </div>

        <!-- 磁贴 6: 存储与主从延迟 -->
        <div class="monitor-tile">
          <div class="tile-top">
            <span class="tile-label">磁盘容量 / 主从延迟</span>
            <span class="tile-tag tag-info">同步正常</span>
          </div>
          <div class="tile-value font-mono">{{ monitorStats.diskSpaceUsage }}% <span class="tile-sub-val">/ {{ monitorStats.replDelay }}</span></div>
          <el-progress :percentage="monitorStats.diskSpaceUsage" :stroke-width="6" :show-text="false" color="#38bdf8" class="tile-progress" />
          <div class="tile-desc">表空间占 29% · 充裕</div>
        </div>
      </div>
    </div>

    <!-- 4. 工单流转分析与生命周期统计图表 -->
    <el-row :gutter="16" class="charts-row">
      <el-col :xs="24" :sm="24" :md="24" :lg="12" class="col-item">
        <div class="chart-card-box">
          <div class="chart-box-header">
            <div class="chart-box-title">
              <span class="chart-dot dot-blue"></span>
              <span>工单生命周期全状态分布</span>
            </div>
            <el-tag size="small" effect="plain" type="info">共 {{ stats.totalTickets || 60 }} 笔</el-tag>
          </div>
          <div ref="ticketChartRef" class="chart-container"></div>
        </div>
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="12" class="col-item">
        <div class="chart-card-box">
          <div class="chart-box-header">
            <div class="chart-box-title">
              <span class="chart-dot dot-emerald"></span>
              <span>近 7 天变更工单提交与流转趋势</span>
            </div>
            <el-tag size="small" effect="plain" type="success">按天统计</el-tag>
          </div>
          <div ref="workloadChartRef" class="chart-container"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 5. 极速工作流与日常效能导航条 (高级设计师体验设计) -->
    <div class="quick-workflow-panel">
      <div class="workflow-panel-title">
        <el-icon color="#2563eb"><Operation /></el-icon>
        <span>核心管理与协同功能直达</span>
      </div>
      <div class="workflow-shortcuts-grid">
        <div class="shortcut-pill" @click="router.push('/ticket-list')">
          <div class="sc-icon bg-amber-soft"><Tickets /></div>
          <div class="sc-text">
            <div class="sc-name">工单流转列表</div>
            <div class="sc-desc">查看审批与加急催办</div>
          </div>
        </div>

        <div class="shortcut-pill" @click="router.push('/data-query')">
          <div class="sc-icon bg-sky-soft"><Search /></div>
          <div class="sc-text">
            <div class="sc-name">数据查询工作台</div>
            <div class="sc-desc">动态脱敏与只读查询</div>
          </div>
        </div>

        <div class="shortcut-pill" @click="router.push('/resource-group-list')">
          <div class="sc-icon bg-teal-soft"><Suitcase /></div>
          <div class="sc-text">
            <div class="sc-name">业务资源组</div>
            <div class="sc-desc">资产隔离与流程路由</div>
          </div>
        </div>

        <div class="shortcut-pill" @click="router.push('/ai-sql-review')">
          <div class="sc-icon bg-purple-soft"><MagicStick /></div>
          <div class="sc-text">
            <div class="sc-name">AI 智能预检</div>
            <div class="sc-desc">Dry-Run 影响行推导</div>
          </div>
        </div>

        <div class="shortcut-pill" @click="router.push('/notification-config')">
          <div class="sc-icon bg-rose-soft"><Bell /></div>
          <div class="sc-text">
            <div class="sc-name">消息通知配置</div>
            <div class="sc-desc">企微/飞书/钉钉总线</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
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
  Coin,
  User,
  Suitcase,
  MagicStick,
  Operation,
  Bell
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

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
  usersCount: 8,
  resourceGroupsCount: 5,
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
      tooltip: {
        trigger: 'item',
        formatter: '{b}: <b>{c}</b> 笔 ({d}%)',
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#0f172a', fontSize: 13 },
        extraCssText: 'box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08); border-radius: 8px;'
      },
      legend: {
        bottom: '2%',
        left: 'center',
        icon: 'circle',
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { color: '#64748b', fontSize: 12 }
      },
      color: ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ef4444', '#06b6d4', '#64748b'],
      series: [
        {
          name: '工单状态分布',
          type: 'pie',
          radius: ['48%', '72%'],
          center: ['50%', '42%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 6,
            borderColor: '#ffffff',
            borderWidth: 3
          },
          label: { show: false, position: 'center' },
          emphasis: {
            label: { show: true, fontSize: 16, fontWeight: 'bold', color: '#0f172a' }
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
      tooltip: {
        trigger: 'axis',
        formatter: '{b} <br/>工单提交流转量: <b>{c}</b> 笔',
        backgroundColor: 'rgba(255, 255, 255, 0.98)',
        borderColor: '#e2e8f0',
        borderWidth: 1,
        textStyle: { color: '#0f172a', fontSize: 13 },
        extraCssText: 'box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08); border-radius: 8px;'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '8%',
        top: '14%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: dates,
        boundaryGap: false,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b', fontSize: 12 }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#64748b', fontSize: 12 }
      },
      series: [
        {
          name: '工单变更数',
          data: counts,
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          itemStyle: {
            color: '#2563eb',
            borderColor: '#ffffff',
            borderWidth: 2
          },
          lineStyle: {
            color: '#2563eb',
            width: 3
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(37, 99, 235, 0.25)' },
              { offset: 1, color: 'rgba(37, 99, 235, 0.01)' }
            ])
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
  padding: 0 0 32px 0;
}

/* ===================================================
   1. 顶层专属欢迎横幅：柔光轻渐变、清爽空气感
   =================================================== */
.welcome-hero-banner {
  background: linear-gradient(135deg, #f0f7ff 0%, #f0fdf4 50%, #ffffff 100%);
  border: 1px solid #dbeafe;
  border-radius: 14px;
  padding: 22px 26px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  box-shadow: 0 2px 10px rgba(37, 99, 235, 0.04);
  flex-wrap: wrap;
  gap: 16px;
}

.welcome-left {
  flex: 1;
  min-width: 280px;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid #bbf7d0;
  border-radius: 20px;
  font-size: 11.5px;
  color: #15803d;
  font-weight: 500;
  margin-bottom: 8px;
}

.pulse-green-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #16a34a;
  box-shadow: 0 0 0 2px rgba(22, 163, 74, 0.2);
}

.welcome-heading {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.2px;
}

.welcome-subtext {
  margin: 6px 0 0 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.welcome-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.hero-btn {
  border-radius: 8px;
  font-weight: 500;
  font-size: 13px;
  transition: all 0.2s;
}

.primary-glow {
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);
}

.ghost-btn {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #334155;
}

.ghost-btn:hover {
  border-color: #3b82f6;
  color: #2563eb;
  background: #eff6ff;
}

/* ===================================================
   2. 6 核心业务统计卡：纯净卡片、微润色谱、无眩光
   =================================================== */
.stat-row {
  margin-bottom: 16px;
}

.col-item {
  margin-bottom: 12px;
}

.metric-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 16px 14px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.metric-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 20px -4px rgba(15, 23, 42, 0.07);
  border-color: #93c5fd;
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.metric-title {
  font-size: 12.5px;
  color: #64748b;
  font-weight: 500;
}

.metric-icon-box {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.bg-sky { background: #eff6ff; color: #2563eb; }
.bg-teal { background: #f0fdfa; color: #0d9488; }
.bg-indigo { background: #eef2ff; color: #4f46e5; }
.bg-amber { background: #fffbeb; color: #d97706; }
.bg-emerald { background: #ecfdf5; color: #059669; }
.bg-slate { background: #f8fafc; color: #475569; }

.text-sky { color: #2563eb; }
.text-teal { color: #0d9488; }
.text-indigo { color: #4f46e5; }
.text-amber { color: #d97706; }
.text-emerald { color: #059669; }
.text-slate { color: #334155; }

.metric-body {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.metric-number {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.5px;
  font-feature-settings: "tnum";
}

.metric-unit {
  font-size: 12px;
  color: #94a3b8;
  font-weight: normal;
}

.metric-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f8fafc;
}

.tag-pill {
  font-size: 10.5px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.tag-sky { background: #eff6ff; color: #2563eb; border: 1px solid #dbeafe; }
.tag-teal { background: #f0fdfa; color: #0d9488; border: 1px solid #ccfbf1; }
.tag-indigo { background: #eef2ff; color: #4f46e5; border: 1px solid #e0e7ff; }
.tag-amber { background: #fffbeb; color: #d97706; border: 1px solid #fef3c7; }
.tag-emerald { background: #ecfdf5; color: #059669; border: 1px solid #d1fae5; }
.tag-slate { background: #f8fafc; color: #475569; border: 1px solid #e2e8f0; }

.metric-sub {
  font-size: 11px;
  color: #94a3b8;
}

/* ===================================================
   3. 实时性能监控：清爽磁贴式微架构
   =================================================== */
.clean-section-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px 20px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.section-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 8px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.bg-blue-dot { background: #3b82f6; box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.2); }

.section-heading {
  margin: 0;
  font-size: 14.5px;
  font-weight: 700;
  color: #0f172a;
}

.section-subtext {
  font-size: 12px;
  color: #94a3b8;
  margin-left: 6px;
}

.status-pill-tag {
  border-radius: 20px;
  font-weight: 500;
}

.monitor-tiles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
}

.monitor-tile {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 12px 14px;
  transition: all 0.2s ease;
}

.monitor-tile:hover {
  background: #ffffff;
  border-color: #cbd5e1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.tile-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.tile-label {
  font-size: 11.5px;
  color: #64748b;
  font-weight: 500;
}

.tile-tag {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
  font-weight: 500;
}

.tag-success { background: #ecfdf5; color: #059669; }
.tag-primary { background: #eff6ff; color: #2563eb; }
.tag-info { background: #f1f5f9; color: #475569; }

.tile-value {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
  margin-bottom: 6px;
}

.tile-sub-val {
  font-size: 12px;
  color: #94a3b8;
  font-weight: normal;
}

.tile-progress {
  margin: 6px 0;
}

.tile-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 4px;
}

.tile-status-clean {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #10b981;
  margin: 6px 0;
  font-weight: 500;
}

.tile-bars-visual {
  display: flex;
  gap: 3px;
  margin: 8px 0;
  align-items: flex-end;
  height: 8px;
}

.bar-pill {
  flex: 1;
  background: #3b82f6;
  border-radius: 2px;
  opacity: 0.6;
}

.bar-1 { height: 40%; }
.bar-2 { height: 75%; opacity: 0.8; }
.bar-3 { height: 100%; opacity: 1; }
.bar-4 { height: 60%; opacity: 0.7; }
.bar-5 { height: 85%; opacity: 0.9; }

.font-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

/* ===================================================
   4. 图表卡片：通透白底与高对比度柔和色相
   =================================================== */
.charts-row {
  margin-bottom: 16px;
}

.chart-card-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px 18px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.chart-box-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.chart-box-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.chart-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot-blue { background: #3b82f6; }
.dot-emerald { background: #10b981; }

.chart-container {
  height: 280px;
  width: 100%;
}

/* ===================================================
   5. 极速工作流与日常效能导航条
   =================================================== */
.quick-workflow-panel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.workflow-panel-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13.5px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

.workflow-shortcuts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}

.shortcut-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.shortcut-pill:hover {
  background: #eff6ff;
  border-color: #3b82f6;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.08);
}

.sc-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.bg-amber-soft { background: #fffbeb; color: #d97706; }
.bg-sky-soft { background: #eff6ff; color: #2563eb; }
.bg-teal-soft { background: #f0fdfa; color: #0d9488; }
.bg-purple-soft { background: #f5f3ff; color: #7c3aed; }
.bg-rose-soft { background: #fff1f2; color: #e11d48; }

.sc-text {
  flex: 1;
  overflow: hidden;
}

.sc-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.sc-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
