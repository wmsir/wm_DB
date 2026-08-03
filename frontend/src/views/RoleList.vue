<template>
  <div class="role-list-container">
    <h2>角色管理</h2>
    <el-button type="primary" @click="handleAdd" style="margin-bottom: 20px;">新增角色</el-button>

    <el-table :data="roles" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="roleCode" label="角色编码"></el-table-column>
      <el-table-column prop="roleName" label="角色名称"></el-table-column>
      <el-table-column prop="description" label="描述"></el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="isEdit ? '编辑角色' : '新增角色'" v-model="dialogVisible" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="角色编码">
          <el-input v-model="form.roleCode"></el-input>
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName"></el-input>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="form.description"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'

const roles = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  roleCode: '',
  roleName: '',
  description: '',
  tenantId: '1' // mock default tenant
})

const fetchRoles = async () => {
  try {
    const res = await request.get('/api/v1/role/list')
    roles.value = res.data
  } catch (error) {
    console.error('Failed to fetch roles', error)
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = {
    id: null,
    roleCode: '',
    roleName: '',
    description: '',
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
    await request.post('/api/v1/role/save', form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchRoles()
  } catch (error) {
    console.error('Failed to save role', error)
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确认删除该角色吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/api/v1/role/${row.id}`)
    ElMessage.success('删除成功')
    fetchRoles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('Failed to delete role', error)
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchRoles()
})
</script>

<style scoped>
.role-list-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
}
</style>
