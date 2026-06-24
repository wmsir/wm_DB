<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="title">wmDB 完美数据库</h2>
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
      >
        <el-form-item label="身份证号码" prop="idCard">
          <el-input
            v-model="loginForm.idCard"
            placeholder="请输入18位身份证号码"
            clearable
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import axios from 'axios'

const loginFormRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  idCard: '',
  password: ''
})

const validateIdCard = (_rule: any, value: any, callback: any) => {
  if (!value) {
    return callback(new Error('请输入身份证号码'))
  }
  // 18-digit ID card regex, supporting ending with X/x
  const idCardRegex = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/
  if (!idCardRegex.test(value)) {
    return callback(new Error('身份证号码格式不正确'))
  }
  callback()
}

const loginRules = reactive<FormRules>({
  idCard: [
    { required: true, validator: validateIdCard, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ]
})

const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const response = await axios.post('/api/v1/auth/login', loginForm)
        const token = response.data.token
        localStorage.setItem('wmdb_token', token)
        ElMessage.success('登录成功')
        // In a real app, router.push('/') would go here
      } catch (error: any) {
        ElMessage.error(error.response?.data || '登录失败')
      } finally {
        loading.value = false
      }
    } else {
      ElMessage.error('表单校验失败，请检查输入')
    }
  })
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.login-btn {
  width: 100%;
  margin-top: 20px;
}
</style>
