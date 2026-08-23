<template>
  <el-container class="layout-container">
    <el-aside width="240px" class="layout-aside">
      <div class="logo">
        <el-icon style="margin-right: 8px; font-size: 20px;"><Platform /></el-icon>
        <span>wmDB 完美数据库</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :default-openeds="['/instances', '/tickets', '/data', '/perms', '/system']"
        class="el-menu-vertical"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
        unique-opened
      >
        <!-- 1. 平台总览 (仅超级管理员可查看) -->
        <el-menu-item index="/dashboard" v-if="userStore.isAdmin">
          <el-icon><Odometer /></el-icon>
          <span>平台总览</span>
        </el-menu-item>

        <!-- 2. 工单中心 -->
        <el-sub-menu index="/tickets" v-if="hasAnyPermission(['/ticket-list', '/ticket-create', '/ai-sql-review'])">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>工单中心</span>
          </template>
          <el-menu-item index="/ticket-list" v-if="userStore.hasPermission('/ticket-list')">
            <el-icon><Tickets /></el-icon>
            <span>工单列表</span>
          </el-menu-item>
          <el-menu-item index="/ticket-create" v-if="userStore.hasPermission('/ticket-create')">
            <el-icon><EditPen /></el-icon>
            <span>新建工单</span>
          </el-menu-item>
          <el-menu-item index="/ai-sql-review" v-if="userStore.hasPermission('/ai-sql-review')">
            <el-icon><MagicStick /></el-icon>
            <span>AI 智能审核</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 3. 数据操作与安全 -->
        <el-sub-menu index="/data" v-if="hasAnyPermission(['/data-query', '/data-masking', '/audit-dashboard'])">
          <template #title>
            <el-icon><Search /></el-icon>
            <span>数据操作与安全</span>
          </template>
          <el-menu-item index="/data-query" v-if="userStore.hasPermission('/data-query')">
            <el-icon><Search /></el-icon>
            <span>数据查询控制台</span>
          </el-menu-item>
          <el-menu-item index="/data-masking" v-if="userStore.hasPermission('/data-masking')">
            <el-icon><Hide /></el-icon>
            <span>动态脱敏配置</span>
          </el-menu-item>
          <el-menu-item index="/audit-dashboard" v-if="userStore.hasPermission('/audit-dashboard')">
            <el-icon><DataAnalysis /></el-icon>
            <span>SQL 审计与合规大屏</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 4. 实例管理 -->
        <el-sub-menu index="/instances" v-if="hasAnyPermission(['/instance-list', '/instance-sessions', '/instance-databases', '/instance-accounts', '/instance-params'])">
          <template #title>
            <el-icon><Coin /></el-icon>
            <span>实例管理</span>
          </template>
          <el-menu-item index="/instance-list" v-if="userStore.hasPermission('/instance-list')">
            <el-icon><List /></el-icon>
            <span>实例列表</span>
          </el-menu-item>
          <el-menu-item index="/instance-config" v-if="userStore.hasPermission('/instance-config') || userStore.hasPermission('/instance-list')">
            <el-icon><Setting /></el-icon>
            <span>参数配置</span>
          </el-menu-item>
          <el-menu-item index="/instance-sessions" v-if="userStore.hasPermission('/instance-sessions')">
            <el-icon><DataLine /></el-icon>
            <span>会话管理</span>
          </el-menu-item>
          <el-menu-item index="/instance-databases" v-if="userStore.hasPermission('/instance-databases')">
            <el-icon><FolderOpened /></el-icon>
            <span>数据库管理</span>
          </el-menu-item>
          <el-menu-item index="/instance-accounts" v-if="userStore.hasPermission('/instance-accounts')">
            <el-icon><User /></el-icon>
            <span>账号管理</span>
          </el-menu-item>
          <el-menu-item index="/instance-params" v-if="userStore.hasPermission('/instance-params')">
            <el-icon><Monitor /></el-icon>
            <span>全局参数查看</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 5. 权限与组织 -->
        <el-sub-menu index="/perms" v-if="hasAnyPermission(['/resource-group-list', '/user-list', '/role-list'])">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>权限与组织</span>
          </template>
          <el-menu-item index="/resource-group-list" v-if="userStore.hasPermission('/resource-group-list')">
            <el-icon><Suitcase /></el-icon>
            <span>业务资源组</span>
          </el-menu-item>
          <el-menu-item index="/user-list" v-if="userStore.hasPermission('/user-list')">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/role-list" v-if="userStore.hasPermission('/role-list')">
            <el-icon><Lock /></el-icon>
            <span>角色权限</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 6. 系统与流程 -->
        <el-sub-menu index="/system" v-if="hasAnyPermission(['/workflow-designer', '/license', '/settings'])">
          <template #title>
            <el-icon><Operation /></el-icon>
            <span>系统与流程</span>
          </template>
          <el-menu-item index="/workflow-designer" v-if="userStore.hasPermission('/workflow-designer')">
            <el-icon><Share /></el-icon>
            <span>流程设计与模板</span>
          </el-menu-item>
          <el-menu-item index="/license" v-if="userStore.hasPermission('/license')">
            <el-icon><Key /></el-icon>
            <span>授权证书</span>
          </el-menu-item>
          <el-menu-item index="/settings" v-if="userStore.hasPermission('/settings')">
            <el-icon><Brush /></el-icon>
            <span>自定义主题</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container class="inner-container">
      <el-header class="layout-header">
        <div class="header-left">
          <span class="system-tag">商业企业版 V2.0</span>
        </div>
        <div class="header-right">
          <!-- 🔔 待办审批提醒通知中心 -->
          <el-popover
            placement="bottom-end"
            :width="380"
            trigger="click"
            popper-class="notification-popover"
          >
            <template #reference>
              <div class="bell-badge-wrapper">
                <el-badge :value="pendingApprovals.length" :hidden="pendingApprovals.length === 0" :max="99" class="notification-badge">
                  <el-button circle class="bell-btn" :type="pendingApprovals.length > 0 ? 'danger' : 'default'" plain>
                    <el-icon :size="18"><Bell /></el-icon>
                  </el-button>
                </el-badge>
              </div>
            </template>

            <!-- 待办审批列表弹窗内容 -->
            <div class="notification-card">
              <div class="n-header">
                <div class="n-title">
                  <el-icon color="#F56C6C" style="margin-right: 6px;"><BellFilled /></el-icon>
                  <span>待办审批工单提醒</span>
                  <el-tag size="small" type="danger" effect="dark" round style="margin-left: 8px;">
                    {{ pendingApprovals.length }} 待处理
                  </el-tag>
                </div>
                <el-button link type="primary" size="small" :icon="Refresh" @click="fetchPendingApprovals">刷新</el-button>
              </div>

              <div class="n-body" v-if="pendingApprovals.length > 0">
                <div
                  v-for="item in pendingApprovals"
                  :key="item.id"
                  class="n-item"
                  @click="handleGoToApproval(item.id)"
                >
                  <div class="n-item-top">
                    <span class="n-item-type">[{{ formatType(item.type) }}]</span>
                    <span class="n-item-id">#{{ item.id }} {{ item.businessKey }}</span>
                    <el-tag size="small" type="warning" effect="plain" class="n-item-node">
                      {{ item.currentNodeName || '待初审/复审' }}
                    </el-tag>
                  </div>
                  <div class="n-item-meta">
                    <span>目标库：<b>{{ item.instanceName }} / {{ item.dbName }}</b></span>
                  </div>
                  <div class="n-item-desc" :title="item.reason">
                    {{ item.reason || '无描述' }}
                  </div>
                  <div class="n-item-footer">
                    <span class="n-item-applicant">申请人：{{ item.applicantName || item.applicantIdCard }}</span>
                    <span class="n-item-time">{{ formatTime(item.createTime) }}</span>
                  </div>
                </div>
              </div>

              <el-empty v-else description="暂无需要您审批的待办工单 🎉" :image-size="70" style="padding: 20px 0;" />

              <div class="n-footer" v-if="pendingApprovals.length > 0">
                <el-button type="primary" size="small" style="width: 100%;" @click="handleGoToAllTickets">
                  查看全部工单列表
                </el-button>
              </div>
            </div>
          </el-popover>

          <el-dropdown @command="handleLanguageChange" style="margin-right: 20px; cursor: pointer;">
            <span class="el-dropdown-link">
              语言 / Language<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh">中文</el-dropdown-item>
                <el-dropdown-item command="en">English</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown @command="handleUserCommand" style="margin-left: 10px; cursor: pointer;">
            <div class="user-profile-badge">
              <el-avatar size="small" style="background: #409EFF; margin-right: 8px;">
                {{ userRealName ? userRealName.substring(0, 1) : '管' }}
              </el-avatar>
              <span style="margin-right: 6px; font-weight: 600;">{{ userRealName }}</span>
              <el-tag size="small" :type="getRoleTagType(userStore.userRole)" effect="dark" round>
                {{ formatRoleNameZh(userStore.userRole) }}
              </el-tag>
              <el-icon class="el-icon--right" style="margin-left: 6px;"><arrow-down /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { useI18n } from 'vue-i18n'
import request from '../utils/request'
import {
  ArrowDown,
  Platform,
  Odometer,
  Document,
  Tickets,
  EditPen,
  MagicStick,
  Search,
  Coin,
  List,
  DataLine,
  FolderOpened,
  User,
  Setting,
  Monitor,
  UserFilled,
  Suitcase,
  Lock,
  Operation,
  Share,
  Key,
  Brush,
  Hide,
  Bell,
  BellFilled,
  Refresh,
  SwitchButton,
  DataAnalysis
} from '@element-plus/icons-vue'

const { locale } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const hasAnyPermission = (paths: string[]) => {
  if (userStore.isAdmin) return true
  return paths.some(p => userStore.hasPermission(p))
}

const getRoleTagType = (role: string) => {
  switch (role?.toUpperCase()) {
    case 'ADMIN':
    case 'ROLE_ADMIN': return 'danger'
    case 'DBA':
    case 'ROLE_DBA': return 'warning'
    case 'DEV_LEAD':
    case 'LEAD': return 'primary'
    case 'DEV':
    case 'ROLE_DEV': return 'info'
    case 'AUDITOR':
    case 'ROLE_AUDITOR': return 'success'
    case 'OPS':
    case 'ROLE_OPS': return 'success'
    default: return 'info'
  }
}

const formatRoleNameZh = (role: string) => {
  if (!role) return '研发工程师'
  const r = role.toUpperCase().trim()
  if (r === 'ADMIN' || r === 'ROLE_ADMIN' || r.includes('ADMIN')) return '系统管理员'
  if (r === 'DBA' || r === 'ROLE_DBA' || r.includes('DBA')) return '核心 DBA'
  if (r === 'DEV_LEAD' || r === 'LEAD' || r.includes('LEAD')) return '开发组长'
  if (r === 'AUDITOR' || r === 'SECURITY_AUDITOR' || r.includes('AUDIT')) return '合规审计员'
  if (r === 'OPS' || r === 'ROLE_OPS' || r.includes('OPS')) return '运维工程师'
  if (r === 'DEV' || r === 'DEVELOPER' || r === 'ROLE_DEV') return '研发工程师'
  return role
}

const pendingApprovals = ref<any[]>([])
let pollTimer: number | null = null

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/instance/edit') || path === '/instance/create') {
    return '/instance-list'
  }
  return path
})

const userRealName = computed(() => userStore.realName || '管理员')

const fetchPendingApprovals = async () => {
  if (!userStore.token) return
  try {
    const res: any = await request.get('/v1/ticket/pending-approvals')
    pendingApprovals.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    // ignore
  }
}

const handleGoToApproval = (ticketId: number | string) => {
  router.push(`/ticket/${ticketId}`)
}

const handleGoToAllTickets = () => {
  router.push({ path: '/ticket-list', query: { status: 'AUDITING' } })
}

const formatType = (type: string) => {
  const map: Record<string, string> = {
    SQL_AUDIT: 'SQL 变更审核',
    DML_CHANGE: 'DML 数据变更',
    DDL_CHANGE: 'DDL 结构变更',
    DATA_EXPORT: '敏感数据导出',
    PERMISSION: '权限申请',
    ACCOUNT: '账号申请',
    DB_TABLE: '库表申请',
    DATA_RECOVERY: '应急数据恢复',
    DATA_QUERY: '数据查询'
  }
  return map[type] || type || 'SQL 变更审核'
}

const formatTime = (timeStr: any) => {
  if (!timeStr) return ''
  const str = String(timeStr).trim()
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

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const handleUserCommand = (cmd: string) => {
  if (cmd === 'profile') {
    router.push('/user-profile')
  } else if (cmd === 'logout') {
    handleLogout()
  }
}

const handleLanguageChange = (lang: string) => {
  locale.value = lang
  localStorage.setItem('language', lang)
}

onMounted(() => {
  userStore.fetchUserInfo()
  fetchPendingApprovals()
  pollTimer = window.setInterval(fetchPendingApprovals, 15000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.layout-container {
  height: 100%;
  width: 100%;
  display: flex;
  flex-direction: row !important;
  overflow: hidden;
}

.layout-aside {
  background-color: #304156;
  width: 240px !important;
  flex-shrink: 0;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.15);
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  background-color: #2b3643;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  padding: 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.el-menu-vertical {
  border-right: none;
  width: 100%;
}

.el-menu-vertical :deep(.el-sub-menu .el-menu-item) {
  padding-left: 48px !important;
  background-color: #1f2d3d !important;
}

.el-menu-vertical :deep(.el-sub-menu .el-menu-item:hover) {
  background-color: #001528 !important;
}

.inner-container {
  display: flex;
  flex: 1;
  flex-direction: column !important;
  height: 100%;
  overflow: hidden;
  min-width: 0;
}

.layout-header {
  height: 60px !important;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  flex-shrink: 0;
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
}

.system-tag {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  color: #409EFF;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
}

.bell-badge-wrapper {
  margin-right: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.bell-btn {
  border: 1px solid #e4e7ed;
}

.bell-btn:hover {
  background: #fdf6ec;
  border-color: #f56c6c;
  color: #f56c6c;
}

.notification-card {
  padding: 4px 0;
}

.n-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 10px;
}

.n-title {
  display: flex;
  align-items: center;
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.n-body {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.n-item {
  padding: 10px 12px;
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.n-item:hover {
  background: #ecf5ff;
  border-color: #409eff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.n-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.n-item-type {
  font-weight: 600;
  color: #409eff;
  font-size: 12px;
}

.n-item-id {
  font-family: monospace;
  font-size: 12px;
  color: #475569;
  flex: 1;
  margin: 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.n-item-meta {
  font-size: 12px;
  color: #334155;
  margin-bottom: 4px;
}

.n-item-desc {
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}

.n-item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: #94a3b8;
}

.n-footer {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

.user-profile-badge {
  display: flex;
  align-items: center;
  font-weight: 500;
  color: #303133;
}

.layout-main {
  background-color: #f0f2f5;
  padding: 20px;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  box-sizing: border-box;
  min-width: 0;
  width: 100%;
}
</style>
