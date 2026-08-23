<template>
  <div class="param-config-container page-container">
    <!-- 管理员全局安全与预检策略配置卡片 -->
    <el-card class="policy-card" shadow="never">
      <template #header>
        <div class="policy-header">
          <div class="policy-title-wrap">
            <span class="policy-icon">🛡️</span>
            <span class="policy-title">平台全局变更安全与预执行策略配置 (Global SQL Safety Policies)</span>
            <el-tag size="small" type="success" effect="light">管理员全局生效</el-tag>
          </div>
          <el-button type="primary" size="small" :loading="policySaving" @click="savePolicies">保存策略变更</el-button>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8">
          <div class="policy-item-box">
            <div class="policy-item-top">
              <span class="item-name">⚡ 事务级预执行校验是否强制</span>
              <el-switch v-model="safetyPolicies.enforceDryRun" active-text="强制校验" inactive-text="非强制" />
            </div>
            <div class="item-desc">开启后，开发人员提交工单前<b>必须点击并通过</b>事务级预执行校验（影响行数与语法精确匹配），否则系统禁止提交。</div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="policy-item-box">
            <div class="policy-item-top">
              <span class="item-name">📦 DML/DDL 变更回滚与备份方案必填</span>
              <el-switch v-model="safetyPolicies.requireBackup" active-text="必须提供" inactive-text="可选" />
            </div>
            <div class="item-desc">开启后，所有数据变更必须在线编写回滚 SQL 或上传备份附件，保障故障秒级应急回退。</div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="policy-item-box">
            <div class="policy-item-top">
              <span class="item-name">🔍 在线只读查询单次最大行数</span>
              <el-input-number v-model="safetyPolicies.maxQueryRows" :min="50" :max="5000" :step="50" size="small" />
            </div>
            <div class="item-desc">限制在线 SQL 工作台每次 SELECT 查询返回的最大记录数，防止大表全表扫描击穿数据库内存。</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 顶部操作栏 -->
    <div class="header-action" style="margin-top: 24px;">
      <div class="title-area">
        <h2 class="page-title">全局参数查看 (Global System Variables)</h2>
        <div class="page-subtitle">在线检索和查看底层数据库实例的全局运行参数与环境变量（连接数、缓冲区、超时时间、日志设置等）</div>
      </div>
      <div class="action-area">
        <el-select
          v-model="selectedInstanceId"
          placeholder="请选择目标数据库实例"
          style="width: 280px; margin-right: 12px;"
          @change="fetchVariables"
        >
          <el-option
            v-for="item in instanceList"
            :key="item.id"
            :label="`${item.name} (${item.dbType || 'mysql'})`"
            :value="item.id"
          />
        </el-select>

        <el-input
          v-model="searchKeyword"
          placeholder="搜索参数名 (如 max_connections)..."
          clearable
          style="width: 260px; margin-right: 12px;"
          :prefix-icon="Search"
          @clear="fetchVariables"
          @keyup.enter="fetchVariables"
        />

        <el-button :icon="Refresh" :loading="loading" @click="fetchVariables">刷新</el-button>
      </div>
    </div>

    <!-- 参数分类过滤标签 -->
    <div class="category-tabs" v-if="selectedInstanceId">
      <el-radio-group v-model="selectedCategory" size="small">
        <el-radio-button value="ALL">全部参数 ({{ variables.length }})</el-radio-button>
        <el-radio-button value="连接与超时">连接与超时</el-radio-button>
        <el-radio-button value="缓冲与内存">缓冲与内存</el-radio-button>
        <el-radio-button value="InnoDB 引擎">InnoDB 引擎</el-radio-button>
        <el-radio-button value="日志与复制">日志与复制</el-radio-button>
        <el-radio-button value="字符集与排序">字符集与排序</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 参数列表表格 -->
    <div class="table-wrapper">
      <el-table :data="pagedVariables" border stripe style="width: 100%" v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />

        <el-table-column prop="name" label="参数名称 (Variable Name)" min-width="260" show-overflow-tooltip>
          <template #default="scope">
            <span style="font-family: monospace; font-weight: 600; color: #409EFF;">{{ scope.row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="value" label="当前运行值 (Value)" min-width="240" show-overflow-tooltip>
          <template #default="scope">
            <span style="font-family: monospace; font-weight: 500; color: #303133;">{{ scope.row.value }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="category" label="所属分类" width="140" align="center">
          <template #default="scope">
            <el-tag size="small" type="info" effect="plain">{{ scope.row.category }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="参数中文说明与调优建议" min-width="260" show-overflow-tooltip>
          <template #default="scope">
            <span style="color: #606266; font-size: 13px;">{{ scope.row.description }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页控制栏 -->
      <div class="pagination-bar" style="display: flex; justify-content: flex-end; margin-top: 16px;">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredVariables.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import request from '../utils/request'

interface VariableItem {
  name: string
  value: string
  category: string
  description: string
}

const loading = ref(false)
const selectedInstanceId = ref<number | null>(null)
const instanceList = ref<any[]>([])
const searchKeyword = ref('')
const selectedCategory = ref('ALL')
const variables = ref<VariableItem[]>([])

const currentPage = ref(1)
const pageSize = ref(20)

const filteredVariables = computed(() => {
  let list = variables.value
  if (selectedCategory.value !== 'ALL') {
    list = list.filter(v => v.category === selectedCategory.value)
  }
  if (searchKeyword.value.trim()) {
    const q = searchKeyword.value.trim().toLowerCase()
    list = list.filter(v => v.name.toLowerCase().includes(q) || (v.description && v.description.toLowerCase().includes(q)))
  }
  return list
})

const pagedVariables = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredVariables.value.slice(start, start + pageSize.value)
})

watch(() => [selectedCategory.value, searchKeyword.value, selectedInstanceId.value], () => {
  currentPage.value = 1
})

const fetchInstances = async () => {
  try {
    const res: any = await request.get('/v1/instance/list')
    instanceList.value = Array.isArray(res.data) ? res.data : []
    if (instanceList.value.length > 0 && !selectedInstanceId.value) {
      selectedInstanceId.value = instanceList.value[0].id
      fetchVariables()
    }
  } catch (error) {
    ElMessage.error('获取实例列表失败')
  }
}

const fetchVariables = async () => {
  if (!selectedInstanceId.value) return
  loading.value = true
  try {
    const res: any = await request.get(`/v1/instance/${selectedInstanceId.value}/variables`, {
      params: { keyword: searchKeyword.value.trim() }
    })
    variables.value = Array.isArray(res.data) ? res.data : []
  } catch (error) {
    ElMessage.error('获取全局参数列表失败')
  } finally {
    loading.value = false
  }
}

const policySaving = ref(false)
const safetyPolicies = ref({
  enforceDryRun: true,
  requireBackup: true,
  maxQueryRows: 1000
})

const fetchSafetyPolicies = async () => {
  try {
    const res: any = await request.get('/v1/config/safety-policies')
    if (res.data) {
      safetyPolicies.value = {
        enforceDryRun: res.data.enforceDryRun ?? true,
        requireBackup: res.data.requireBackup ?? true,
        maxQueryRows: res.data.maxQueryRows ?? 1000
      }
    }
  } catch (error) {
    // fallback
  }
}

const savePolicies = async () => {
  policySaving.value = true
  try {
    await request.post('/v1/config/safety-policies', safetyPolicies.value)
    ElMessage.success('平台全局安全与预检策略已成功更新并实时生效！')
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存策略配置失败')
  } finally {
    policySaving.value = false
  }
}

onMounted(() => {
  fetchSafetyPolicies()
  fetchInstances()
})
</script>

<style scoped>
.policy-card {
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.policy-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.policy-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.policy-icon {
  font-size: 18px;
}

.policy-title {
  font-weight: 700;
  font-size: 15px;
  color: #1e293b;
}

.policy-item-box {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  box-sizing: border-box;
}

.policy-item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-name {
  font-weight: 600;
  font-size: 13px;
  color: #334155;
}

.item-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
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

.action-area {
  display: flex;
  align-items: center;
}

.category-tabs {
  margin-bottom: 16px;
}

.table-wrapper {
  background: #ffffff;
  border-radius: 8px;
  overflow-x: auto;
}
</style>
