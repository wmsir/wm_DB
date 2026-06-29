<template>
  <div class="license-container">
    <h2>商业授权状态 (License)</h2>
    <el-card shadow="hover" v-loading="loading">
      <el-result v-if="licenseData.valid" icon="success" title="授权有效" sub-title="您的商业授权处于有效状态">
        <template #extra>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="版本信息">{{ licenseData.edition }}</el-descriptions-item>
            <el-descriptions-item label="到期时间">{{ licenseData.expiresAt }}</el-descriptions-item>
            <el-descriptions-item label="当前机器码">{{ licenseData.machineCode }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>
      <el-result v-else icon="error" title="授权无效或已过期" sub-title="请联系管理员或商务获取新的商业授权">
        <template #extra>
          <el-button type="primary">上传新证书</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(true)
const licenseData = ref({
  valid: false,
  edition: '',
  expiresAt: '',
  machineCode: ''
})

const fetchLicenseStatus = async () => {
  try {
    const res: any = await request.get('/v1/license/check')
    licenseData.value = res.data
  } catch (err) {
    ElMessage.error('无法获取 License 状态')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchLicenseStatus()
})
</script>

<style scoped>
.license-container {
  padding: 20px;
}
</style>
