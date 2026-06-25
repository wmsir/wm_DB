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
/**
 * 登录视图组件
 *
 * 提供基于身份证号码的实名制安全认证功能。
 * 包含 18 位身份证号码的正则校验（支持末尾为 X/x），以及基础的密码校验逻辑。
 * 验证通过后将获取到的 JWT Token 写入 localStorage 中。
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '../utils/request'
import JSEncrypt from 'jsencrypt'
import { useUserStore } from '../store/user'

// PUBLIC_KEY should be fetched dynamically from backend via API or injected via env vars.
// For architectural scaffold demonstration we define a variable to hold the dynamic value.
const PUBLIC_KEY = import.meta.env.VITE_RSA_PUBLIC_KEY || `MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC3/z/f5+hW4+L8+M2G2M2Z2m2r
2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m
2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m2r2Z2m
2Z2m2wIDAQAB`;

// 路由和表单引用
const router = useRouter()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const userStore = useUserStore()

// 登录表单数据
const loginForm = reactive({
  idCard: '',
  password: ''
})

/**
 * 自定义身份证校验规则
 */
const validateIdCard = (_rule: any, value: any, callback: any) => {
  if (!value) {
    return callback(new Error('请输入身份证号码'))
  }
  // 18位身份证正则，支持末尾为大/小写 X
  const idCardRegex = /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]$/
  if (!idCardRegex.test(value)) {
    return callback(new Error('身份证号码格式不正确'))
  }
  callback()
}

// 绑定至 Element Plus 表单的验证规则
const loginRules = reactive<FormRules>({
  idCard: [
    { required: true, validator: validateIdCard, trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ]
})

/**
 * 提交登录请求
 */
const handleLogin = async () => {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const encryptor = new JSEncrypt()
        encryptor.setPublicKey(PUBLIC_KEY)
        const encryptedPassword = encryptor.encrypt(loginForm.password) || loginForm.password

        const payload = {
            idCard: loginForm.idCard,
            password: encryptedPassword
        }

        const response: any = await request.post('/v1/auth/login', payload)
        const token = response.data.token

        userStore.setToken(token)

        ElMessage.success('登录成功')
        router.push('/ticket/1') // 跳转到示例详情页，实际应跳往仪表盘
      } catch (error: any) {
        // Axios interceptor handles generic errors, but we can do local fallbacks if needed
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
