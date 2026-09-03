<template>
  <div class="ticket-list-container page-container">
    <div class="header-action">
      <div>
        <h2 class="page-title">SQL 工单中心</h2>
        <div class="page-subtitle">企业级 SQL 变更与安全审批流转中心，支持全生命周期追踪、多库实例级联与精细化组合筛选</div>
      </div>
      <div class="action-btns">
        <el-button :icon="CopyDocument" @click="handleOpenNewWindow">在新窗口创建工单</el-button>
        <el-button type="primary" :icon="Plus" @click="handleGoCreate">创建工单 (新页面)</el-button>
      </div>
    </div>

    <!-- 0. 顶部工单数据权限范围标识与快捷视角筛选 -->
    <div class="data-scope-banner" v-if="dataScopeInfo">
      <div class="scope-banner-left">
        <el-tag :type="getDataScopeTagType(dataScopeInfo.scope)" effect="dark" size="default">
          {{ dataScopeInfo.scopeName || '数据权限范围' }}
        </el-tag>
        <span class="scope-banner-text">{{ dataScopeInfo.description }}</span>
      </div>
      <div class="scope-banner-right">
        <el-radio-group v-model="userPerspectiveFilter" size="small">
          <el-radio-button value="ALL">全部权限工单 ({{ tickets.length }})</el-radio-button>
          <el-radio-button value="MY_SUBMIT">我发起的 ({{ mySubmitCount }})</el-radio-button>
          <el-radio-button value="PENDING_ME">待我初审/复核 ({{ pendingMeCount }})</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 1. 顶部状态分类快捷 Tabs 筛选 -->
    <el-card shadow="hover" class="filter-tabs-card">
      <el-tabs v-model="activeStatusTab" @tab-change="handleTabChange" class="ticket-status-tabs">
        <el-tab-pane name="ALL">
          <template #label>
            <span class="tab-label">全部工单 <el-badge :value="statusCounts.ALL" class="tab-badge" type="info" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="AUDITING">
          <template #label>
            <span class="tab-label">待初审 / 待复核 <el-badge :value="statusCounts.AUDITING" class="tab-badge" type="danger" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="MANUAL_PROCESSING">
          <template #label>
            <span class="tab-label">待 DBA 线下反馈 <el-badge :value="statusCounts.MANUAL_PROCESSING" class="tab-badge" type="warning" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="WAITING_EXECUTION">
          <template #label>
            <span class="tab-label">待定时触发 <el-badge :value="statusCounts.WAITING_EXECUTION" class="tab-badge" type="primary" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="APPROVED_OR_EXEC">
          <template #label>
            <span class="tab-label">审批通过 / 执行中 <el-badge :value="statusCounts.APPROVED_OR_EXEC" class="tab-badge" type="primary" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="EXECUTED">
          <template #label>
            <span class="tab-label">已执行归档 <el-badge :value="statusCounts.EXECUTED" class="tab-badge" type="success" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="REJECTED">
          <template #label>
            <span class="tab-label">已驳回 <el-badge :value="statusCounts.REJECTED" class="tab-badge" type="info" /></span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="FAILED">
          <template #label>
            <span class="tab-label">执行失败 <el-badge :value="statusCounts.FAILED" class="tab-badge" type="danger" /></span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 2. 多维组合过滤工具栏 -->
      <div class="filter-toolbar">
        <div class="filter-inputs">
          <!-- 资源组筛选 -->
          <el-select
            v-model="filterResourceGroup"
            placeholder="筛选业务资源组"
            clearable
            style="width: 220px;"
          >
            <el-option label="全部业务资源组" value="" />
            <el-option
              v-for="rg in availableResourceGroups"
              :key="rg"
              :label="`🗂️ ${rg}`"
              :value="rg"
            />
          </el-select>

          <el-select
            v-model="filterInstanceId"
            placeholder="筛选目标数据库实例"
            clearable
            style="width: 240px;"
          >
            <el-option
              v-for="inst in instances"
              :key="inst.id"
              :label="`${inst.name} (${inst.env || 'PROD'})`"
              :value="inst.id"
            />
          </el-select>

          <el-select
            v-model="filterType"
            placeholder="工单类型"
            clearable
            style="width: 150px;"
          >
            <el-option label="全部类型" value="" />
            <el-option label="SQL 变更审核" value="SQL_AUDIT" />
            <el-option label="敏感数据导出" value="DATA_EXPORT" />
            <el-option label="权限申请" value="PERMISSION" />
            <el-option label="账号申请" value="ACCOUNT" />
            <el-option label="库表申请" value="DB_TABLE" />
            <el-option label="应急数据恢复" value="DATA_RECOVERY" />
          </el-select>

          <el-input
            v-model="searchKeyword"
            placeholder="搜索工单ID / 申请人 / 说明 / 目标库..."
            clearable
            :prefix-icon="Search"
            style="width: 260px;"
          />

          <el-button type="primary" :icon="Search" @click="handleApplyFilter">查询</el-button>
          <el-button :icon="RefreshRight" @click="handleResetFilter">重置</el-button>
        </div>

        <div class="filter-actions">
          <el-button :icon="Refresh" :loading="loading" @click="loadTickets">刷新数据</el-button>
        </div>
      </div>
    </el-card>

    <!-- 3. 工单列表数据呈现卡片 -->
    <el-card shadow="hover" style="margin-top: 16px;">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-weight: 700; font-size: 15px;">工单数据列表</span>
            <el-tag size="small" type="info" effect="plain">共 {{ filteredTickets.length }} 条匹配</el-tag>
          </div>
        </div>
      </template>

      <div class="table-wrapper">
        <el-table
          :data="pagedTickets"
          style="width: 100%"
          v-loading="loading"
          stripe
          border
          empty-text="暂无匹配的工单记录"
        >
          <!-- 0. 工单号 (点击可直接跳转到工单详情) -->
          <el-table-column label="工单号" prop="id" width="130" fixed="left" align="center">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                style="font-weight: 700; font-family: monospace; font-size: 13px;"
                @click="viewDetail(row.id)"
              >
                #{{ row.id }}
              </el-button>
            </template>
          </el-table-column>

          <!-- 1. 申请人 -->
          <el-table-column label="申请人" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="applicant-cell">
                <el-avatar :size="26" class="applicant-avatar">
                  {{ getAvatarInitial(row.applicantName || row.applicantIdCard) }}
                </el-avatar>
                <div class="applicant-info">
                  <span class="applicant-name-text">{{ cleanApplicantName(row.applicantName || row.applicantIdCard) }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <!-- 2. 变更类型 -->
          <el-table-column prop="type" label="变更类型" width="130" align="center">
            <template #default="{ row }">
              <el-tag effect="plain" :type="getTicketTypeTagType(row.type)">{{ getTicketTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>

          <!-- 3. 所属业务资源组 (独立字段) -->
          <el-table-column label="所属业务资源组" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag size="small" type="warning" effect="plain" style="font-weight: 600;">
                🗂️ {{ getInstanceResourceGroup(row) }}
              </el-tag>
            </template>
          </el-table-column>

          <!-- 4. 所属部门与业务系统 (独立字段) -->
          <el-table-column label="所属部门与业务系统" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <span style="font-size: 12px; color: #475569; font-weight: 500;">
                🏛️ {{ getInstanceDeptOrSystem(row) }}
              </span>
            </template>
          </el-table-column>

          <!-- 5. 目标数据库实例 -->
          <el-table-column label="目标数据库实例" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <div style="display: flex; align-items: center; justify-content: space-between; gap: 6px;">
                <span class="instance-name-text" style="font-weight: 600; color: #1e293b;">
                  {{ getInstanceObj(row.instanceId)?.name || getInstanceName(row.instanceId) }}
                </span>
                <el-tag
                  size="small"
                  :type="getInstanceObj(row.instanceId)?.env === 'PROD' ? 'danger' : 'info'"
                  effect="light"
                  style="font-size: 10px; font-weight: 600;"
                >
                  {{ getInstanceObj(row.instanceId)?.env || 'PROD' }}
                </el-tag>
              </div>
            </template>
          </el-table-column>

          <!-- 6. 目标执行数据库 -->
          <el-table-column prop="dbName" label="目标数据库" min-width="130" show-overflow-tooltip>
            <template #default="{ row }">
              <el-tag v-if="row.dbName" size="small" type="success" effect="plain" style="font-family: monospace; font-weight: 600;">
                🗃️ {{ row.dbName }}
              </el-tag>
              <span v-else style="color: #94a3b8; font-size: 12px;">全局 / 默认库</span>
            </template>
          </el-table-column>

          <!-- 5. 申请说明 / 变更原因 -->
          <el-table-column prop="reason" label="申请说明 / 变更原因" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="reason-text">{{ row.reason || '无申请说明' }}</span>
            </template>
          </el-table-column>

          <!-- 6. 流程状态列 (增强展示待执行权限组与定时窗口) -->
          <el-table-column prop="status" label="流程状态 / 待执行权限组" min-width="190" align="center">
            <template #default="{ row }">
              <div class="status-column-cell" style="display: flex; flex-direction: column; align-items: center; gap: 4px;">
                <!-- 主状态 Tag -->
                <el-tag :type="getStatusType(row.status)" effect="dark" size="small">
                  {{ getStatusLabel(row.status) }}
                </el-tag>

                <!-- 审批中：显示待执行权限组 -->
                <div v-if="row.status === 'AUDITING' || row.status === 'PENDING_APPROVAL'" style="margin-top: 2px;">
                  <el-tag
                    size="small"
                    :type="getPendingApprovalRole(row).tagType"
                    effect="plain"
                    class="pending-role-pill"
                    style="font-size: 11px; font-weight: 600;"
                  >
                    {{ getPendingApprovalRole(row).icon }} 待{{ getPendingApprovalRole(row).roleName }}
                  </el-tag>
                </div>

                <!-- 定时等待执行：显示计划时间 -->
                <div v-else-if="row.status === 'WAITING_EXECUTION'" style="font-size: 11px; color: #d97706; font-family: monospace; margin-top: 2px;">
                  <span>⏰ {{ formatExecutionWindow(row.executionWindow) }}</span>
                </div>

                <!-- 转 DBA 线下执行 -->
                <div v-else-if="row.status === 'MANUAL_PROCESSING'" style="margin-top: 2px;">
                  <el-tag size="small" type="warning" effect="plain" style="font-size: 11px;">
                    🛠️ 待 DBA 线下执行
                  </el-tag>
                </div>
              </div>
            </template>
          </el-table-column>

          <!-- 7. 提交时间标准 年月日时分秒格式 -->
          <el-table-column label="提交时间" width="175" align="center">
            <template #default="{ row }">
              <span class="time-text" style="font-family: monospace; font-size: 12px;">{{ row.createTime || formatTicketTime(row.id) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="230" fixed="right" align="center">
            <template #default="{ row }">
              <div style="display: flex; justify-content: center; align-items: center; gap: 8px; flex-wrap: nowrap;">
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :icon="Tickets"
                  style="font-weight: 600; padding: 4px 10px;"
                  @click="viewDetail(row.id)"
                >
                  审批 / 查看
                </el-button>
                
                <!-- 催办审批（处于审批流转中的工单） -->
                <el-button
                  v-if="['AUDITING', 'PENDING_APPROVAL', 'SUBMITTED'].includes(row.status)"
                  size="small"
                  type="danger"
                  plain
                  :icon="Bell"
                  style="font-weight: 600; padding: 4px 10px;"
                  :loading="urgingMap[row.id]"
                  :disabled="(urgeCooldownMap[row.id] || 0) > 0"
                  @click="handleUrgeTicket(row)"
                >
                  {{ (urgeCooldownMap[row.id] || 0) > 0 ? `催办 (${urgeCooldownMap[row.id]}s)` : '催办' }}
                </el-button>

                <!-- 申请人撤回工单（仅限自己提交且处于审批中的工单） -->
                <el-button
                  v-if="isMySubmittedTicket(row) && (row.status === 'AUDITING' || row.status === 'PENDING_APPROVAL')"
                  size="small"
                  type="warning"
                  plain
                  :icon="Back"
                  style="font-weight: 600; padding: 4px 10px;"
                  @click="handleWithdrawTicket(row)"
                >
                  撤回
                </el-button>

                <!-- 终态工单再来一单 -->
                <el-button
                  v-if="['TERMINATED', 'FAILED', 'REJECTED', 'EXECUTED'].includes(row.status)"
                  size="small"
                  type="success"
                  plain
                  :icon="RefreshRight"
                  style="font-weight: 600; padding: 4px 10px;"
                  @click="cloneTicket(row.id)"
                >
                  再来一单
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页控制栏 -->
        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="filteredTickets.length"
            layout="total, sizes, prev, pager, next, jumper"
            background
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import request from '../utils/request'
import { useUserStore } from '../store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, CopyDocument, Search, RefreshRight, Tickets, Back, Bell } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const tickets = ref<any[]>([])
const instances = ref<any[]>([])
const loading = ref(false)

// 分页状态
const currentPage = ref(1)
const pageSize = ref(10)

// 数据权限范围元数据
const dataScopeInfo = ref<any>({
  scope: 'RESOURCE_GROUP',
  scopeName: '所属业务资源组工单',
  description: '仅展示归属业务资源组相关及本人发起的工单'
})

// 用户视角筛选：ALL (全部权限工单), MY_SUBMIT (我发起的), PENDING_ME (待我初审/复核)
const userPerspectiveFilter = ref('ALL')

// 筛选字段
const activeStatusTab = ref('ALL')
const filterResourceGroup = ref('')
const filterInstanceId = ref<any>('')
const filterType = ref('')
const searchKeyword = ref('')
const availableResourceGroups = ref<string[]>([])

const getDataScopeTagType = (scope: string) => {
  if (scope === 'ALL') return 'danger'
  if (scope === 'RESOURCE_GROUP') return 'warning'
  return 'primary'
}

const loadResourceGroups = async () => {
  try {
    const res: any = await request.get('/v1/resource-group/list')
    const list = Array.isArray(res.data) ? res.data : []
    availableResourceGroups.value = list.map((rg: any) => rg.groupName).filter(Boolean)
  } catch (e) {
    availableResourceGroups.value = ['车险承保资源组', '销管系统资源组']
  }
}

const loadDataScopeInfo = async () => {
  try {
    const res: any = await request.get('/v1/ticket/data-scope')
    if (res.data) {
      dataScopeInfo.value = res.data
    }
  } catch (e) {
    // fallback
  }
}

const loadInstances = async () => {
  try {
    const res: any = await request.get('/v1/instance/list')
    instances.value = Array.isArray(res.data) ? res.data : []
  } catch (e) {
    // ignore
  }
}

const getInstanceObj = (id: number | string) => {
  return instances.value.find(i => String(i.id) === String(id)) || null
}

const getInstanceName = (id: number | string) => {
  const found = getInstanceObj(id)
  return found ? `${found.name} (${found.env || 'PROD'})` : `数据库实例 #${id}`
}

const getInstanceResourceGroup = (ticket: any) => {
  if (ticket.resourceGroup) return ticket.resourceGroup
  const inst = getInstanceObj(ticket.instanceId)
  if (inst?.resourceGroups) {
    try {
      const arr = JSON.parse(inst.resourceGroups)
      if (Array.isArray(arr) && arr.length > 0) return arr.join('、')
    } catch (e) {
      return inst.resourceGroups
    }
  }
  return '默认核心业务资源组'
}

const getInstanceDeptOrSystem = (ticket: any) => {
  const inst = getInstanceObj(ticket.instanceId)
  if (inst?.instanceConfig) {
    try {
      const conf = JSON.parse(inst.instanceConfig)
      if (conf.systemName || conf.department) {
        return `${conf.department || '产险研发中心'} · ${conf.systemName || '核心业务系统'}`
      }
    } catch (e) {}
  }
  if (inst?.description && (inst.description.includes('系统') || inst.description.includes('部'))) {
    return inst.description
  }
  const rg = getInstanceResourceGroup(ticket)
  if (rg.includes('车险') || (inst?.name && inst.name.includes('车险'))) {
    return '产险研发部 · 车险核心承保系统'
  }
  if (rg.includes('销管') || (inst?.name && inst.name.includes('销管'))) {
    return '渠道营销技术部 · 综合销管业务系统'
  }
  if (rg.includes('理赔') || (inst?.name && inst.name.includes('理赔'))) {
    return '运营理赔技术部 · 智能理赔核心系统'
  }
  return '基础架构部 · 企业核心数据中台'
}

const getPendingApprovalRole = (ticket: any) => {
  const type = ticket.type || 'SQL_AUDIT'
  const reason = (ticket.reason || '').toUpperCase()
  const hasDdl = type === 'DDL' || type === 'DDL_CHANGE' || reason.includes('CREATE') || reason.includes('ALTER') || reason.includes('DROP') || reason.includes('TRUNCATE')
  const isHighRisk = hasDdl || (ticket.affectRowsEstimate && ticket.affectRowsEstimate > 1000)
  if (isHighRisk) {
    return {
      roleKey: 'DBA',
      roleName: '核心 DBA 安全复核',
      tagType: 'danger',
      icon: '🛡️'
    }
  }
  return {
    roleKey: 'DEV_LEAD',
    roleName: '业务开发组长初审',
    tagType: 'warning',
    icon: '👤'
  }
}

const formatExecutionWindow = (win?: string) => {
  if (!win) return '计划窗口待调度'
  return win.replace('scheduled:', '').replace('scheduled', '').trim()
}

const isMySubmittedTicket = (ticket: any) => {
  if (!userStore.userInfo) return false
  const myIdCard = userStore.userInfo.idCard
  const myUsername = userStore.userInfo.username
  const myRealName = userStore.userInfo.realName
  const app = ticket.applicantIdCard || ''
  return app === myIdCard || app === myUsername || app === myRealName
}

const mySubmitCount = computed(() => {
  return tickets.value.filter(isMySubmittedTicket).length
})

const pendingMeCount = computed(() => {
  return tickets.value.filter((t: any) => {
    const isAuditing = t.status === 'AUDITING' || t.status === 'PENDING_APPROVAL' || t.status === 'MANUAL_PROCESSING'
    return isAuditing
  }).length
})

const loadTickets = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/v1/ticket/list')
    tickets.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    ElMessage.error('加载工单列表失败')
  } finally {
    loading.value = false
  }
}

const handleGoCreate = () => {
  router.push('/ticket-create')
}

const handleOpenNewWindow = () => {
  window.open('/ticket-create', '_blank')
}

const viewDetail = (id: number | string) => {
  router.push(`/ticket/${id}`)
}

const handleTabChange = (tabName: any) => {
  activeStatusTab.value = tabName
}

const cleanApplicantName = (name?: string) => {
  if (!name) return '系统'
  return name.replace(/[\(（].*?[\)）]/g, '').trim() || name
}

const getAvatarInitial = (name?: string) => {
  const clean = cleanApplicantName(name)
  return clean ? clean.charAt(0) : '用'
}

const handleApplyFilter = () => {
  // 保持当前计算属性实时生效
}

const handleResetFilter = () => {
  activeStatusTab.value = 'ALL'
  userPerspectiveFilter.value = 'ALL'
  filterResourceGroup.value = ''
  filterInstanceId.value = ''
  filterType.value = ''
  searchKeyword.value = ''
}

// 统计各状态工单数量
const statusCounts = computed(() => {
  const counts = {
    ALL: tickets.value.length,
    AUDITING: 0,
    MANUAL_PROCESSING: 0,
    WAITING_EXECUTION: 0,
    APPROVED_OR_EXEC: 0,
    EXECUTED: 0,
    REJECTED: 0,
    FAILED: 0
  }

  for (const t of tickets.value) {
    const st = t.status
    if (st === 'AUDITING' || st === 'PENDING_APPROVAL') counts.AUDITING++
    else if (st === 'MANUAL_PROCESSING') counts.MANUAL_PROCESSING++
    else if (st === 'WAITING_EXECUTION') counts.WAITING_EXECUTION++
    else if (st === 'APPROVED' || st === 'EXECUTING') counts.APPROVED_OR_EXEC++
    else if (st === 'EXECUTED') counts.EXECUTED++
    else if (st === 'REJECTED') counts.REJECTED++
    else if (st === 'FAILED') counts.FAILED++
  }

  return counts
})

// 多维组合筛选逻辑
const filteredTickets = computed(() => {
  return tickets.value.filter((item: any) => {
    // 0. 用户视角快捷筛选 (ALL / MY_SUBMIT / PENDING_ME)
    if (userPerspectiveFilter.value === 'MY_SUBMIT') {
      if (!isMySubmittedTicket(item)) return false
    } else if (userPerspectiveFilter.value === 'PENDING_ME') {
      const isPending = item.status === 'AUDITING' || item.status === 'PENDING_APPROVAL' || item.status === 'MANUAL_PROCESSING'
      if (!isPending) return false
    }

    // 1. 状态 Tab 筛选
    if (activeStatusTab.value === 'AUDITING') {
      if (item.status !== 'AUDITING' && item.status !== 'PENDING_APPROVAL') return false
    } else if (activeStatusTab.value === 'MANUAL_PROCESSING') {
      if (item.status !== 'MANUAL_PROCESSING') return false
    } else if (activeStatusTab.value === 'WAITING_EXECUTION') {
      if (item.status !== 'WAITING_EXECUTION') return false
    } else if (activeStatusTab.value === 'APPROVED_OR_EXEC') {
      if (item.status !== 'APPROVED' && item.status !== 'EXECUTING') return false
    } else if (activeStatusTab.value === 'EXECUTED') {
      if (item.status !== 'EXECUTED') return false
    } else if (activeStatusTab.value === 'REJECTED') {
      if (item.status !== 'REJECTED') return false
    } else if (activeStatusTab.value === 'FAILED') {
      if (item.status !== 'FAILED') return false
    }

    // 1.5 业务资源组筛选
    if (filterResourceGroup.value && item.resourceGroup !== filterResourceGroup.value) {
      return false
    }

    // 2. 目标实例筛选
    if (filterInstanceId.value && String(item.instanceId) !== String(filterInstanceId.value)) {
      return false
    }

    // 3. 工单类型筛选
    if (filterType.value && item.type !== filterType.value) {
      return false
    }

    // 4. 关键字搜索
    if (searchKeyword.value && searchKeyword.value.trim()) {
      const q = searchKeyword.value.trim().toLowerCase()
      const matchId = String(item.id).includes(q)
      const matchReason = item.reason && item.reason.toLowerCase().includes(q)
      const matchApplicant = item.applicantIdCard && item.applicantIdCard.toLowerCase().includes(q)
      const matchKey = item.businessKey && item.businessKey.toLowerCase().includes(q)
      const matchInst = getInstanceName(item.instanceId).toLowerCase().includes(q)
      if (!matchId && !matchReason && !matchApplicant && !matchKey && !matchInst) {
        return false
      }
    }

    return true
  })
})

const pagedTickets = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredTickets.value.slice(start, start + pageSize.value)
})

watch(() => [activeStatusTab.value, userPerspectiveFilter.value, filterInstanceId.value, filterType.value, searchKeyword.value], () => {
  currentPage.value = 1
})

const urgingMap = ref<Record<number, boolean>>({})
const urgeCooldownMap = ref<Record<number, number>>({})

const startUrgeCooldown = (ticketId: number, seconds = 60) => {
  urgeCooldownMap.value[ticketId] = seconds
  const timer = setInterval(() => {
    if ((urgeCooldownMap.value[ticketId] || 0) <= 1) {
      delete urgeCooldownMap.value[ticketId]
      clearInterval(timer)
    } else {
      urgeCooldownMap.value[ticketId]--
    }
  }, 1000)
}

const handleUrgeTicket = async (row: any) => {
  if ((urgeCooldownMap.value[row.id] || 0) > 0) {
    ElMessage.warning(`您刚已发起过催办，请等待 ${urgeCooldownMap.value[row.id]} 秒后再次催办`)
    return
  }

  try {
    const { value: reason } = await ElMessageBox.prompt(
      `即将向工单 #${row.id}（目标库: ${row.dbName || '默认库'}）当前节点的所有待审批责任人发送加急催办通知（支持企业微信工作消息、飞书富文本互动卡片、阿里钉钉）。\n\n您可补充催办加急说明：`,
      '⏰ 工单审批加急催办',
      {
        confirmButtonText: '立即发送催办通知',
        cancelButtonText: '取消',
        inputPlaceholder: '如：生产上线窗口临近，劳烦领导尽快协助审批！',
        inputValue: '生产上线窗口临近，劳烦领导尽快协助审批！',
        type: 'warning'
      }
    )

    urgingMap.value[row.id] = true
    const res: any = await request.post(`/v1/ticket/${row.id}/urge`, { reason: reason || '请尽快审批' })
    const msg = res?.data?.message || '加急催办通知已通过启用的企业微信、飞书、钉钉成功下发给当前待审批人！'
    ElMessage.success(msg)
    startUrgeCooldown(row.id, 60)
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err?.response?.data?.message || err?.message || '发起催办失败')
    }
  } finally {
    urgingMap.value[row.id] = false
  }
}

const handleWithdrawTicket = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要撤回工单 #${row.id} 吗？\n撤回后当前审批流将立即终止，并将自动载入该工单历史数据返回创建页面供您再次编辑。`,
      '撤回工单确认',
      {
        confirmButtonText: '确认撤回并编辑',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await request.post(`/v1/ticket/${row.id}/withdraw`)
    ElMessage.success('工单已成功撤回，正在为您加载原数据进入编辑模式...')
    router.push({ path: '/ticket-create', query: { fromTicketId: row.id } })
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '撤回工单失败')
    }
  }
}

const cloneTicket = (id: string | number) => {
  router.push({ path: '/ticket-create', query: { fromTicketId: id } })
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'APPROVED': return 'success'
    case 'EXECUTED': return 'success'
    case 'AUDITING': return 'warning'
    case 'PENDING_APPROVAL': return 'warning'
    case 'MANUAL_PROCESSING': return 'warning'
    case 'WAITING_EXECUTION': return 'primary'
    case 'EXECUTING': return 'primary'
    case 'REJECTED': return 'danger'
    case 'FAILED': return 'danger'
    case 'TERMINATED': return 'info'
    default: return 'info'
  }
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    'AUDITING': '待初审/复核',
    'PENDING_APPROVAL': '待初审/复核',
    'APPROVED': '已审批通过',
    'WAITING_EXECUTION': '待定时触发',
    'MANUAL_PROCESSING': '待DBA线下反馈',
    'EXECUTING': '正在流式执行',
    'EXECUTED': '已执行归档',
    'REJECTED': '已驳回',
    'FAILED': '执行失败',
    'TERMINATED': '已终止'
  }
  return map[status] || status
}

const getTicketTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    'SQL_AUDIT': 'SQL 变更审核',
    'DML_CHANGE': 'DML 数据变更',
    'DDL_CHANGE': 'DDL 结构变更',
    'DATA_EXPORT': '敏感数据导出',
    'PERMISSION': '权限申请',
    'ACCOUNT': '账号申请',
    'DB_TABLE': '库表申请',
    'DATA_RECOVERY': '应急数据恢复'
  }
  return map[type] || type || 'SQL 变更审核'
}

const getTicketTypeTagType = (type: string) => {
  switch (type) {
    case 'SQL_AUDIT':
    case 'DML_CHANGE':
      return 'primary'
    case 'DDL_CHANGE':
      return 'danger'
    case 'DATA_EXPORT':
      return 'warning'
    case 'DATA_RECOVERY':
      return 'danger'
    default:
      return 'info'
  }
}

const formatTicketTime = (timeOrId: any) => {
  if (!timeOrId) return '-'
  const str = String(timeOrId).trim()
  if (str.includes('-') && str.includes(':')) {
    return str
  }
  if (str.length >= 14 && (str.startsWith('2025') || str.startsWith('2026') || str.startsWith('2027'))) {
    const y = str.substring(0, 4)
    const m = str.substring(4, 6)
    const d = str.substring(6, 8)
    const h = str.substring(8, 10)
    const mi = str.substring(10, 12)
    const s = str.substring(12, 14)
    return `${y}-${m}-${d} ${h}:${mi}:${s}`
  }
  const num = Number(str)
  if (num && num > 1000000000000) {
    const d = new Date(num)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hours = String(d.getHours()).padStart(2, '0')
    const minutes = String(d.getMinutes()).padStart(2, '0')
    const seconds = String(d.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  }
  return str
}

// 监听路由参数中的 status
watch(
  () => route.query.status,
  (st) => {
    if (st && typeof st === 'string') {
      activeStatusTab.value = st
    }
  },
  { immediate: true }
)

onMounted(() => {
  loadDataScopeInfo()
  loadResourceGroups()
  loadInstances()
  loadTickets()
})
</script>

<style scoped>
.ticket-list-container {
  height: 100%;
  width: 100%;
}

.data-scope-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fafc 100%);
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 14px;
}

.scope-banner-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.scope-banner-text {
  font-size: 13px;
  color: #1e40af;
  font-weight: 500;
}

.scope-banner-right {
  display: flex;
  align-items: center;
}

.header-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-subtitle {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.action-btns {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-tabs-card :deep(.el-card__body) {
  padding: 16px 20px 12px 20px;
}

.ticket-status-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  padding: 0 16px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-badge :deep(.el-badge__content) {
  font-size: 11px;
  height: 16px;
  line-height: 16px;
  padding: 0 5px;
}

.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f0f2f5;
}

.filter-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.ticket-id-badge {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-weight: 700;
  color: #409eff;
  background: #ecf5ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.instance-name-text {
  font-weight: 600;
  color: #303133;
}

.reason-text {
  color: #606266;
  font-size: 13px;
}

.applicant-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.applicant-avatar {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%) !important;
  color: #ffffff !important;
  font-weight: 600 !important;
  font-size: 12px !important;
  box-shadow: 0 1px 3px rgba(59, 130, 246, 0.3);
  flex-shrink: 0;
}

.applicant-name-text {
  font-weight: 600;
  color: #1e293b;
  font-size: 13px;
}

.time-text {
  font-size: 12px;
  color: #909399;
}

.instance-composite-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pending-role-pill {
  border-radius: 12px;
  padding: 0 8px;
  height: 22px;
  line-height: 20px;
}
</style>