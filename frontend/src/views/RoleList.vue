<template>
  <div class="role-list-container page-container">
    <!-- 多页签切换栏系统 (Tab Workbench) -->
    <el-tabs
      v-model="activeTab"
      type="card"
      class="role-workbench-tabs"
      @tab-remove="handleTabRemove"
    >
      <!-- 主页签 1: 系统角色列表 (固定不可关闭) -->
      <el-tab-pane name="list" :closable="false">
        <template #label>
          <div class="tab-label-item">
            <el-icon color="#409EFF"><Lock /></el-icon>
            <span>系统角色列表 (Roles)</span>
          </div>
        </template>

        <div class="tab-inner-content">
          <div class="header-action">
            <div>
              <h2 class="page-title">系统角色与权限管理</h2>
              <div class="page-subtitle">配置系统角色及其可访问与使用的功能页签、菜单及功能模块边界</div>
            </div>
            <div class="action-btns">
              <el-input
                v-model="searchQuery"
                placeholder="搜索角色编码/名称/页签..."
                clearable
                style="width: 240px; margin-right: 12px;"
                :prefix-icon="Search"
              />
              <el-button @click="fetchRoles" :icon="Refresh">刷新</el-button>
              <el-button type="primary" @click="handleOpenCreateTab" :icon="Plus">新增系统角色</el-button>
            </div>
          </div>

          <!-- 角色与页签权限列表 -->
          <div class="table-wrapper">
            <el-table :data="pagedRoles" border stripe style="width: 100%" v-loading="loading">
              <el-table-column prop="id" label="ID" width="60" align="center"></el-table-column>
              
              <el-table-column prop="roleCode" label="角色编码" width="150">
                <template #default="scope">
                  <el-tag :type="getRoleTagType(scope.row.roleCode)" effect="dark">
                    {{ scope.row.roleCode }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column prop="roleName" label="角色名称" width="160">
                <template #default="scope">
                  <el-link type="primary" :underline="false" style="font-weight: 600;" @click="handleOpenEditTab(scope.row)">
                    {{ scope.row.roleName }}
                  </el-link>
                </template>
              </el-table-column>

              <el-table-column label="已授权的系统功能页签与模块 (Permissions)" min-width="380">
                <template #default="scope">
                  <div class="perm-summary-container">
                    <template v-if="parseSummary(scope.row.permissions).isAll">
                      <el-tag type="danger" effect="dark" class="all-perm-badge">
                        👑 拥有全量 17 个系统功能页签 (超级特权)
                      </el-tag>
                    </template>
                    <template v-else>
                      <div class="module-tags-wrap">
                        <el-tag
                          v-for="mod in parseSummary(scope.row.permissions).modules"
                          :key="mod.name"
                          size="small"
                          type="primary"
                          effect="light"
                          class="mod-tag"
                        >
                          {{ mod.name }} ({{ mod.count }}/{{ mod.total }})
                        </el-tag>
                        <span class="total-tab-count">
                          共 {{ parseSummary(scope.row.permissions).totalCount }} 个页签
                        </span>
                      </div>
                    </template>
                  </div>
                </template>
              </el-table-column>

              <el-table-column prop="description" label="角色定位与权限说明" min-width="220" show-overflow-tooltip></el-table-column>

              <el-table-column label="操作" width="200" fixed="right" align="center">
                <template #default="scope">
                  <el-button size="small" type="primary" plain :icon="Lock" @click="handleOpenEditTab(scope.row)">
                    配置页签权限
                  </el-button>
                  <el-button
                    size="small"
                    type="danger"
                    plain
                    :disabled="isSystemRole(scope.row.roleCode)"
                    @click="handleDelete(scope.row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页控制栏 -->
            <div class="pagination-bar" style="display: flex; justify-content: flex-end; margin-top: 16px;">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50]"
                :total="filteredRoles.length"
                layout="total, sizes, prev, pager, next, jumper"
                background
              />
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- 动态编辑/新增角色页签 (支持多任务切换与随时关闭) -->
      <el-tab-pane
        v-for="tab in dynamicTabs"
        :key="tab.name"
        :name="tab.name"
        :closable="true"
      >
        <template #label>
          <div class="tab-label-item">
            <el-icon :color="tab.isEdit ? '#E6A23C' : '#67C23A'">
              <component :is="tab.isEdit ? Lock : Plus" />
            </el-icon>
            <span>{{ tab.title }}</span>
          </div>
        </template>

        <div class="tab-inner-content edit-tab-content">
          <!-- 顶部一体化吸顶导航条 (无遮挡设计) -->
          <div class="edit-nav-top-bar">
            <div class="nav-left">
              <el-button :icon="ArrowLeft" @click="activeTab = 'list'" plain size="default">
                返回角色列表
              </el-button>
              <div class="nav-title-group">
                <span class="nav-main-title">
                  {{ tab.isEdit ? `配置角色【${tab.form.roleName || tab.form.roleCode}】权限` : '新增系统角色与页签授权' }}
                </span>
                <span class="nav-sub-desc">
                  配置角色编码、展示名称以及可访问的 17 个微应用功能页签
                </span>
              </div>
            </div>
            <div class="nav-actions">
              <el-button @click="handleCloseTab(tab.name)" :icon="CloseBold" plain type="info">
                关闭页签
              </el-button>
              <el-button
                type="primary"
                :icon="Check"
                :loading="tab.saveLoading"
                @click="handleSaveTab(tab)"
              >
                保存权限配置
              </el-button>
            </div>
          </div>

          <!-- 编辑表单 -->
          <el-form
            :ref="(el: any) => tab.formRef = el"
            :model="tab.form"
            :rules="rules"
            label-position="top"
            class="role-edit-form"
          >
            <!-- 1. 基础信息卡片 -->
            <el-card shadow="hover" class="config-card mb-20">
              <template #header>
                <div class="card-header-title">
                  <span class="step-num-badge">1</span>
                  <el-icon color="#409EFF"><UserFilled /></el-icon>
                  <span>角色基本信息定义 (Role Profile)</span>
                </div>
              </template>

              <el-row :gutter="20">
                <el-col :xs="24" :sm="12">
                  <el-form-item label="角色编码 (唯一英文标识)" prop="roleCode">
                    <el-input
                      v-model="tab.form.roleCode"
                      placeholder="如：ADMIN / DBA / DEV / AUDITOR"
                      :disabled="tab.isEdit && isSystemRole(tab.form.roleCode)"
                    />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :sm="12">
                  <el-form-item label="角色展示名称" prop="roleName">
                    <el-input v-model="tab.form.roleName" placeholder="如：核心数据库管理员 / 业务初审组长" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-form-item label="角色定位与职责说明" prop="description">
                <el-input
                  type="textarea"
                  v-model="tab.form.description"
                  :rows="2"
                  placeholder="描述该角色的主要业务定位、数据审批职责及安全等级..."
                />
              </el-form-item>
            </el-card>

            <!-- 2. 快捷权限包与页签勾选面板 -->
            <el-card shadow="hover" class="config-card mb-20">
              <template #header>
                <div class="card-header-title" style="justify-content: space-between; width: 100%;">
                  <div style="display: flex; align-items: center; gap: 8px;">
                    <span class="step-num-badge">2</span>
                    <el-icon color="#67C23A"><Lock /></el-icon>
                    <span>可使用功能页签与权限矩阵 (Tab Permissions Matrix)</span>
                  </div>
                  <div class="footer-stats">
                    <span style="font-size: 13px; color: #64748b; margin-right: 6px;">已授权：</span>
                    <el-tag type="primary" size="small" effect="dark">
                      {{ isTabAllChecked(tab) ? '全部 17 个页签' : `${tab.selectedPermissions.length} / 17 个页签` }}
                    </el-tag>
                  </div>
                </div>
              </template>

              <!-- 快捷权限包工具条 -->
              <div class="preset-box-row" style="margin-bottom: 16px;">
                <span class="preset-label" style="font-size: 13px; font-weight: 700; color: #334155; margin-right: 12px;">
                  ⚡ 一键载入快捷权限模板：
                </span>
                <el-button size="small" type="success" plain @click="applyPresetToTab(tab, 'ADMIN')">
                  👑 超级管理员 (全选 17个)
                </el-button>
                <el-button size="small" type="warning" plain @click="applyPresetToTab(tab, 'DBA')">
                  💾 核心 DBA (13个页签)
                </el-button>
                <el-button size="small" type="primary" plain @click="applyPresetToTab(tab, 'DEV_LEAD')">
                  🛡️ 开发组长 (8个页签)
                </el-button>
                <el-button size="small" type="info" plain @click="applyPresetToTab(tab, 'DEV')">
                  💻 研发人员 (5个页签)
                </el-button>
                <el-button size="small" type="danger" plain @click="applyPresetToTab(tab, 'AUDITOR')">
                  🔍 安全审计员 (6个页签)
                </el-button>
                <el-button size="small" link type="danger" @click="handleClearTabPermissions(tab)">
                  清空所选
                </el-button>
              </div>

              <!-- 模块化页签与功能权限勾选面板 -->
              <div class="permission-tree-box">
                <div
                  v-for="moduleNode in SYSTEM_PERMISSION_TREE"
                  :key="moduleNode.id"
                  class="module-group-card"
                >
                  <div class="module-group-header">
                    <el-checkbox
                      :model-value="isModuleFullyCheckedForTab(tab, moduleNode)"
                      :indeterminate="isModuleIndeterminateForTab(tab, moduleNode)"
                      @change="(val: boolean) => toggleModuleCheckForTab(tab, moduleNode, val)"
                    >
                      <span class="module-title-text">{{ moduleNode.name }}</span>
                    </el-checkbox>
                    <span class="module-desc-text">{{ moduleNode.description }}</span>
                  </div>

                  <!-- 子页签复选框网格 -->
                  <div class="child-tabs-grid">
                    <template v-if="moduleNode.children && moduleNode.children.length > 0">
                      <div
                        v-for="child in moduleNode.children"
                        :key="child.id"
                        class="tab-checkbox-pill"
                        :class="{ active: tab.selectedPermissions.includes(child.id) }"
                        @click="toggleSinglePermissionForTab(tab, child.id)"
                      >
                        <el-checkbox
                          :model-value="tab.selectedPermissions.includes(child.id)"
                          @click.stop="toggleSinglePermissionForTab(tab, child.id)"
                        />
                        <div class="tab-pill-text">
                          <span class="tab-name">{{ child.name }}</span>
                          <span class="tab-path">{{ child.id }}</span>
                        </div>
                      </div>
                    </template>
                    <template v-else>
                      <div
                        class="tab-checkbox-pill single"
                        :class="{ active: tab.selectedPermissions.includes(moduleNode.id) }"
                        @click="toggleSinglePermissionForTab(tab, moduleNode.id)"
                      >
                        <el-checkbox
                          :model-value="tab.selectedPermissions.includes(moduleNode.id)"
                          @click.stop="toggleSinglePermissionForTab(tab, moduleNode.id)"
                        />
                        <div class="tab-pill-text">
                          <span class="tab-name">{{ moduleNode.name }}</span>
                          <span class="tab-path">{{ moduleNode.id }}</span>
                        </div>
                      </div>
                    </template>
                  </div>
                </div>
              </div>
            </el-card>

            <!-- 底部保存按钮 -->
            <div class="tab-bottom-action-bar">
              <el-button size="large" @click="handleCloseTab(tab.name)">取消并关闭</el-button>
              <el-button
                type="primary"
                size="large"
                :icon="Check"
                :loading="tab.saveLoading"
                @click="handleSaveTab(tab)"
              >
                保存权限配置
              </el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  Search,
  Refresh,
  Plus,
  Lock,
  ArrowLeft,
  Check,
  UserFilled,
  CloseBold
} from '@element-plus/icons-vue'
import request from '../utils/request'
import {
  SYSTEM_PERMISSION_TREE,
  PRESET_PERMISSION_PACKAGES,
  getAllTabKeys,
  getPermissionSummary,
  type PermissionNode
} from '../utils/permissions'

interface RoleItem {
  id: number | null
  roleCode: string
  roleName: string
  description: string
  permissions?: string
  tenantId?: string
}

interface DynamicRoleTabItem {
  name: string
  title: string
  isEdit: boolean
  saveLoading: boolean
  form: RoleItem
  formRef?: FormInstance
  selectedPermissions: string[]
}

const roles = ref<RoleItem[]>([])
const loading = ref(false)
const searchQuery = ref('')

// 多页签系统状态
const activeTab = ref('list')
const dynamicTabs = ref<DynamicRoleTabItem[]>([])

const rules = ref<FormRules>({
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
})

const allTabKeys = getAllTabKeys()

const getRoleTagType = (code: string) => {
  switch (code?.toUpperCase()) {
    case 'ADMIN': return 'danger'
    case 'DBA': return 'warning'
    case 'DEV_LEAD': return 'primary'
    case 'DEV': return 'info'
    case 'AUDITOR': return 'success'
    default: return 'info'
  }
}

const isSystemRole = (code: string) => {
  return ['ADMIN', 'DBA', 'DEV', 'DEV_LEAD', 'AUDITOR'].includes(code?.toUpperCase())
}

const parseSummary = (perms?: string) => {
  return getPermissionSummary(perms || '[]')
}

const currentPage = ref(1)
const pageSize = ref(10)

const filteredRoles = computed(() => {
  if (!searchQuery.value) return roles.value
  const q = searchQuery.value.toLowerCase()
  return roles.value.filter(item =>
    (item.roleCode && item.roleCode.toLowerCase().includes(q)) ||
    (item.roleName && item.roleName.toLowerCase().includes(q)) ||
    (item.description && item.description.toLowerCase().includes(q)) ||
    (item.permissions && item.permissions.toLowerCase().includes(q))
  )
})

const pagedRoles = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRoles.value.slice(start, start + pageSize.value)
})

watch(() => searchQuery.value, () => {
  currentPage.value = 1
})

const fetchRoles = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/v1/role/list')
    roles.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    console.error('Failed to fetch roles', error)
  } finally {
    loading.value = false
  }
}

// 打开新增角色页签
const handleOpenCreateTab = () => {
  const tabName = 'create_role_tab'
  const existing = dynamicTabs.value.find(t => t.name === tabName)
  if (existing) {
    activeTab.value = tabName
    return
  }

  const newTab: DynamicRoleTabItem = {
    name: tabName,
    title: '➕ 新增系统角色',
    isEdit: false,
    saveLoading: false,
    form: {
      id: null,
      roleCode: '',
      roleName: '',
      description: '',
      permissions: '[]',
      tenantId: '1'
    },
    selectedPermissions: [...PRESET_PERMISSION_PACKAGES.DEV.paths]
  }

  dynamicTabs.value.push(newTab)
  activeTab.value = tabName
}

// 打开编辑角色权限页签
const handleOpenEditTab = (row: RoleItem) => {
  const tabName = `edit_role_${row.id || row.roleCode}`
  const existing = dynamicTabs.value.find(t => t.name === tabName)
  if (existing) {
    activeTab.value = tabName
    return
  }

  let list: string[] = []
  if (row.permissions) {
    try {
      list = JSON.parse(row.permissions)
    } catch {
      list = [row.permissions]
    }
  }
  let perms = list.includes('*') || row.roleCode === 'ADMIN' ? [...allTabKeys] : [...list]

  const editTab: DynamicRoleTabItem = {
    name: tabName,
    title: `⚙️ ${row.roleName || row.roleCode}`,
    isEdit: true,
    saveLoading: false,
    form: { ...row },
    selectedPermissions: perms
  }

  dynamicTabs.value.push(editTab)
  activeTab.value = tabName
}

// 关闭页签
const handleCloseTab = (tabName: string) => {
  const idx = dynamicTabs.value.findIndex(t => t.name === tabName)
  if (idx !== -1) {
    dynamicTabs.value.splice(idx, 1)
  }
  if (activeTab.value === tabName) {
    activeTab.value = 'list'
  }
}

const handleTabRemove = (targetName: string) => {
  handleCloseTab(targetName)
}

// 快捷权限包应用到 Tab
const applyPresetToTab = (tab: DynamicRoleTabItem, presetKey: string) => {
  const pkg = PRESET_PERMISSION_PACKAGES[presetKey]
  if (!pkg) return
  if (pkg.paths.includes('*')) {
    tab.selectedPermissions = [...allTabKeys]
  } else {
    tab.selectedPermissions = [...pkg.paths]
  }
  ElMessage.success(`已载入【${pkg.name}】预置页签权限`)
}

const handleClearTabPermissions = (tab: DynamicRoleTabItem) => {
  tab.selectedPermissions = []
  ElMessage.info('已清空所选页签权限')
}

const isTabAllChecked = (tab: DynamicRoleTabItem) => {
  return tab.selectedPermissions.length === allTabKeys.length || tab.selectedPermissions.includes('*')
}

// 模块级多选与判断
const getModuleChildKeys = (moduleNode: PermissionNode): string[] => {
  if (moduleNode.children && moduleNode.children.length > 0) {
    return moduleNode.children.map(c => c.id)
  }
  return [moduleNode.id]
}

const isModuleFullyCheckedForTab = (tab: DynamicRoleTabItem, moduleNode: PermissionNode): boolean => {
  const childKeys = getModuleChildKeys(moduleNode)
  return childKeys.every(k => tab.selectedPermissions.includes(k))
}

const isModuleIndeterminateForTab = (tab: DynamicRoleTabItem, moduleNode: PermissionNode): boolean => {
  const childKeys = getModuleChildKeys(moduleNode)
  const checkedCount = childKeys.filter(k => tab.selectedPermissions.includes(k)).length
  return checkedCount > 0 && checkedCount < childKeys.length
}

const toggleModuleCheckForTab = (tab: DynamicRoleTabItem, moduleNode: PermissionNode, checked: boolean) => {
  const childKeys = getModuleChildKeys(moduleNode)
  if (checked) {
    const next = new Set([...tab.selectedPermissions, ...childKeys])
    tab.selectedPermissions = Array.from(next)
  } else {
    tab.selectedPermissions = tab.selectedPermissions.filter(k => !childKeys.includes(k))
  }
}

const toggleSinglePermissionForTab = (tab: DynamicRoleTabItem, permId: string) => {
  const idx = tab.selectedPermissions.indexOf(permId)
  if (idx !== -1) {
    tab.selectedPermissions.splice(idx, 1)
  } else {
    tab.selectedPermissions.push(permId)
  }
}

// 保存角色配置
const handleSaveTab = async (tab: DynamicRoleTabItem) => {
  if (!tab.form.roleCode || !tab.form.roleName) {
    ElMessage.warning('请填写角色编码和角色名称')
    return
  }

  tab.saveLoading = true
  try {
    let finalPerms = JSON.stringify(tab.selectedPermissions)
    if (tab.selectedPermissions.length === allTabKeys.length || tab.form.roleCode === 'ADMIN') {
      finalPerms = JSON.stringify(['*'])
    }

    const payload = {
      ...tab.form,
      permissions: finalPerms
    }

    await request.post('/v1/role/save', payload)
    ElMessage.success(`角色【${tab.form.roleName}】配置已成功保存生效！`)

    // 刷新列表
    await fetchRoles()

    // 关闭 Tab 并返回列表
    handleCloseTab(tab.name)
  } catch (error: any) {
    ElMessage.error(error.message || '保存角色配置失败')
  } finally {
    tab.saveLoading = false
  }
}

const handleDelete = (row: RoleItem) => {
  ElMessageBox.confirm(
    `确定要删除系统角色【${row.roleName} (${row.roleCode})】吗？`,
    '安全警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await request.post(`/v1/role/delete/${row.id}`)
      ElMessage.success('角色删除成功')
      fetchRoles()
    } catch (e: any) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchRoles()
})
</script>

<style scoped>
.role-list-container {
  width: 100%;
}

.role-workbench-tabs {
  width: 100%;
}

.role-workbench-tabs :deep(.el-tabs__content) {
  overflow: visible !important;
}

/* ==================== 企业级一体化置顶工作台头 (无冗余边框) ==================== */
.role-workbench-tabs > :deep(.el-tabs__header) {
  position: sticky;
  top: 0px;
  z-index: 100;
  margin-bottom: 0px;
  background: #ffffff;
  border-radius: 8px 8px 0 0;
  padding: 8px 14px 0 14px;
  border: 1px solid #e2e8f0;
  border-bottom: none;
}

.role-workbench-tabs :deep(.el-tabs__item) {
  font-weight: 600;
  font-size: 13px;
  transition: all 0.2s ease;
  background: #f8fafc;
  margin-right: 4px;
  border-radius: 6px 6px 0 0;
  border: 1px solid #e2e8f0;
  border-bottom: none;
}

.role-workbench-tabs :deep(.el-tabs__item.is-active) {
  background: #ffffff;
  color: #3b82f6;
  border-color: #cbd5e1;
}

.tab-label-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tab-inner-content {
  background: transparent;
}

.header-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 14px;
  margin-bottom: 16px;
  background: #ffffff;
  padding: 16px 20px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 12px;
  color: #64748b;
}

.action-btns {
  display: flex;
  align-items: center;
}

.table-wrapper {
  background: #ffffff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.perm-summary-container {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.all-perm-badge {
  font-weight: bold;
}

.module-tags-wrap {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.mod-tag {
  margin: 2px 0;
}

.total-tab-count {
  font-size: 12px;
  color: #64748b;
  margin-left: 6px;
}

/* ==================== 编辑页签特有样式 (一体化极简吸顶导航) ==================== */
.edit-tab-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.edit-nav-top-bar {
  position: sticky;
  top: 42px;
  z-index: 99;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  padding: 10px 16px;
  border-radius: 0 0 8px 8px;
  border: 1px solid #e2e8f0;
  border-top: 1px solid #f1f5f9;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 4px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-title-group {
  display: flex;
  flex-direction: column;
}

.nav-main-title {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
}

.nav-sub-desc {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.config-card {
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
}

.mb-20 {
  margin-bottom: 16px;
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
}

.step-num-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background: #3b82f6;
  color: #ffffff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 800;
}

.permission-tree-box {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.module-group-card {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 14px 16px;
  background: #f8fafc;
}

.module-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.module-title-text {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
  margin-left: 6px;
}

.module-desc-text {
  font-size: 12px;
  color: #64748b;
}

.child-tabs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.tab-checkbox-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ffffff;
  border: 1.5px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.tab-checkbox-pill:hover {
  border-color: #93c5fd;
  background: #f0f9ff;
}

.tab-checkbox-pill.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.tab-pill-text {
  display: flex;
  flex-direction: column;
}

.tab-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.tab-path {
  font-size: 11px;
  color: #94a3b8;
  font-family: monospace;
}

.tab-bottom-action-bar {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  background: #ffffff;
  padding: 16px 20px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  margin-top: 10px;
}
</style>
