<template>
  <div class="instance-list-container">
    <h2>实例管理配置</h2>
    <el-button type="primary" @click="handleAdd" style="margin-bottom: 20px;">新增实例</el-button>

    <el-table :data="instances" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="name" label="实例名称"></el-table-column>
      <el-table-column prop="dbType" label="数据库类型"></el-table-column>
      <el-table-column prop="env" label="环境"></el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'APPROVED' ? 'success' : 'warning'">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="success" v-if="scope.row.status === 'AUDITING'" @click="handleApprove(scope.row)">审核通过</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="isEdit ? '编辑实例' : '新增实例'" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="实例名称">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="数据库类型">
          <el-input v-model="form.dbType"></el-input>
        </el-form-item>
        <el-form-item label="JDBC URL">
          <el-input v-model="form.jdbcUrl"></el-input>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" v-model="form.passwordCipher"></el-input>
        </el-form-item>
        <el-form-item label="环境">
          <el-select v-model="form.env">
            <el-option label="DEV" value="DEV"></el-option>
            <el-option label="TEST" value="TEST"></el-option>
            <el-option label="PROD" value="PROD"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">提交审核</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const instances = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  name: '',
  dbType: '',
  jdbcUrl: '',
  username: '',
  passwordCipher: '',
  env: 'DEV',
  tenantId: '1' // mock default tenant
})

const fetchInstances = async () => {
  try {
    const res = await request.get('/api/v1/instance/list')
    instances.value = res.data
  } catch (error) {
    console.error('Failed to fetch instances', error)
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    name: '',
    dbType: '',
    jdbcUrl: '',
    username: '',
    passwordCipher: '',
    env: 'DEV',
    tenantId: '1'
  }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    await request.post('/api/v1/instance/save', form.value)
    ElMessage.success('提交成功，等待审核')
    dialogVisible.value = false
    fetchInstances()
  } catch (error) {
    console.error('Failed to save instance', error)
    ElMessage.error('保存失败')
  }
}

const handleApprove = async (row: any) => {
  try {
    await request.post(`/api/v1/instance/${row.id}/approve`)
    ElMessage.success('审核通过')
    fetchInstances()
  } catch (error) {
    console.error('Failed to approve instance', error)
    ElMessage.error('审核失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该实例吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/api/v1/instance/${row.id}`)
    ElMessage.success('删除成功')
    fetchInstances()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete instance', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchInstances()
})
</script>

<style scoped>
.instance-list-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
}
</style>
