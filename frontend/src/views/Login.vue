<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <div class="brand-logo">
          <span class="logo-icon">🛡️</span>
        </div>
        <h2 class="title">wmDB 完美数据库</h2>
        <div class="subtitle">企业级智能数据库运维与安全审计平台</div>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs" stretch @tab-change="handleTabChange">
        <!-- Tab 1: 账号密码登录 -->
        <el-tab-pane label="账号密码" name="account">
          <el-form
            ref="accountFormRef"
            :model="accountForm"
            :rules="accountRules"
            label-position="top"
          >
            <el-form-item label="登录账号" prop="account">
              <el-input
                v-model="accountForm.account"
                placeholder="用户名 / 手机号 / 邮箱 / 身份证号"
                clearable
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item label="登录密码" prop="password">
              <el-input
                v-model="accountForm.password"
                type="password"
                placeholder="请输入登录密码"
                show-password
                :prefix-icon="Lock"
                @keyup.enter="handleAccountLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" @click="handleAccountLogin" :loading="loading">
                登 录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Tab 2: 手机/邮箱验证码登录 -->
        <el-tab-pane label="验证码登录" name="phone">
          <el-form
            ref="phoneFormRef"
            :model="phoneForm"
            :rules="phoneRules"
            label-position="top"
          >
            <el-form-item label="手机号或邮箱" prop="phone">
              <el-input
                v-model="phoneForm.phone"
                placeholder="请输入11位手机号或企业邮箱"
                clearable
                :prefix-icon="Iphone"
              />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
              <div class="code-input-row">
                <el-input
                  v-model="phoneForm.code"
                  placeholder="请输入6位验证码"
                  clearable
                  :prefix-icon="Key"
                  maxlength="6"
                  @keyup.enter="handlePhoneLogin"
                />
                <el-button
                  type="primary"
                  plain
                  class="send-code-btn"
                  :disabled="countdown > 0 || sendLoading"
                  :loading="sendLoading"
                  @click="handleSendCode"
                >
                  {{ countdown > 0 ? `${countdown}s 后重新获取` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" @click="handlePhoneLogin" :loading="loading">
                登 录 / 快速验证
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- Tab 3: APP 扫码登录 (企微/钉钉/飞书/统一SSO) -->
        <el-tab-pane label="APP 扫码登录" name="qrcode">
          <div class="qr-login-container">
            <!-- 渠道选择器 -->
            <div class="qr-channel-selector">
              <div
                v-for="ch in channels"
                :key="ch.key"
                class="channel-tab"
                :class="{ active: currentChannel === ch.key }"
                @click="switchChannel(ch.key)"
              >
                <span class="ch-icon">{{ ch.icon }}</span>
                <span class="ch-label">{{ ch.name }}</span>
              </div>
            </div>

            <!-- 二维码展示核心区 -->
            <div class="qr-card-wrapper">
              <div class="qr-box" :class="{ 'is-expired': qrInfo?.status === 'EXPIRED' }" @click="qrInfo?.status === 'EXPIRED' && refreshQr()">
                <!-- 二维码图形模拟与 SVG 渲染 -->
                <div class="qr-code-graphic" v-if="qrInfo">
                  <svg viewBox="0 0 100 100" class="qr-svg">
                    <rect x="0" y="0" width="100" height="100" fill="#ffffff" />
                    <!-- 定位角标 1 (左上) -->
                    <rect x="8" y="8" width="26" height="26" fill="#1e293b" rx="4" />
                    <rect x="13" y="13" width="16" height="16" fill="#ffffff" rx="2" />
                    <rect x="17" y="17" width="8" height="8" fill="#1e293b" rx="1" />

                    <!-- 定位角标 2 (右上) -->
                    <rect x="66" y="8" width="26" height="26" fill="#1e293b" rx="4" />
                    <rect x="71" y="13" width="16" height="16" fill="#ffffff" rx="2" />
                    <rect x="75" y="17" width="8" height="8" fill="#1e293b" rx="1" />

                    <!-- 定位角标 3 (左下) -->
                    <rect x="8" y="66" width="26" height="26" fill="#1e293b" rx="4" />
                    <rect x="13" y="71" width="16" height="16" fill="#ffffff" rx="2" />
                    <rect x="17" y="75" width="8" height="8" fill="#1e293b" rx="1" />

                    <!-- 数据矩阵点阵 -->
                    <circle cx="42" cy="12" r="3" fill="#3b82f6" />
                    <circle cx="50" cy="18" r="3" fill="#1e293b" />
                    <circle cx="40" cy="28" r="3" fill="#1e293b" />
                    <circle cx="55" cy="30" r="3" fill="#3b82f6" />
                    <circle cx="15" cy="45" r="3" fill="#1e293b" />
                    <circle cx="26" cy="48" r="3" fill="#3b82f6" />
                    <circle cx="38" cy="42" r="3" fill="#1e293b" />
                    <circle cx="50" cy="50" r="4" fill="#2563eb" />
                    <circle cx="62" cy="42" r="3" fill="#1e293b" />
                    <circle cx="75" cy="48" r="3" fill="#3b82f6" />
                    <circle cx="85" cy="45" r="3" fill="#1e293b" />
                    <circle cx="42" cy="62" r="3" fill="#1e293b" />
                    <circle cx="55" cy="70" r="3" fill="#3b82f6" />
                    <circle cx="68" cy="72" r="3" fill="#1e293b" />
                    <circle cx="82" cy="65" r="3" fill="#1e293b" />
                    <circle cx="72" cy="85" r="3" fill="#3b82f6" />
                    <circle cx="85" cy="82" r="3" fill="#1e293b" />
                    <circle cx="45" cy="85" r="3" fill="#1e293b" />
                  </svg>
                  <div class="qr-center-logo">
                    <span>{{ getChannelIcon(currentChannel) }}</span>
                  </div>
                </div>

                <!-- 过期遮罩 -->
                <div class="qr-mask expired-mask" v-if="qrInfo?.status === 'EXPIRED'">
                  <el-icon :size="28"><Refresh /></el-icon>
                  <span class="mask-text">二维码已过期</span>
                  <span class="mask-sub">点击重新获取</span>
                </div>

                <!-- 扫码成功待确认遮罩 -->
                <div class="qr-mask scanned-mask" v-else-if="qrInfo?.status === 'SCANNED'">
                  <el-icon :size="32" color="#67C23A"><Check /></el-icon>
                  <span class="mask-text">已扫描</span>
                  <span class="mask-sub">请在移动端点击确认登录</span>
                </div>
              </div>

              <!-- 状态提示与有效倒计时 -->
              <div class="qr-status-info">
                <div class="qr-tip-text">
                  请使用 <b>{{ getChannelName(currentChannel) }}</b> 扫描二维码
                </div>
                <div class="qr-expire-tag" v-if="qrInfo && qrInfo.status === 'WAITING'">
                  二维码有效期剩余：<b>{{ qrRemainSeconds }}s</b>
                </div>
              </div>

              <!-- 测试模拟扫码授权按钮 -->
              <div class="mock-scan-area">
                <el-button
                  type="success"
                  size="small"
                  plain
                  :icon="Cellphone"
                  :loading="mockScanLoading"
                  @click="handleMockScanConfirm"
                >
                  ⚡ 模拟移动端一键扫码授权
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 底部注册与账号链接 -->
      <div class="login-footer-action">
        <span>还没有账号？</span>
        <el-button link type="primary" style="font-weight: 600;" @click="registerDialogVisible = true">
          立即注册新账号
        </el-button>
      </div>

      <!-- 快速测试账号提示区 (覆盖全流程审批与开发角色) -->
      <div class="test-account-tip">
        <div class="tip-header">
          <span style="font-weight: 700;">🎯 审批节点与全流程演练预置账号</span>
        </div>
        <div class="tip-body">
          <div class="tip-item">
            <span class="tip-text"><b>👑 超级管理员</b> (全节点特权)：<code>testadmin1</code> / <code>123456</code></span>
            <el-button link type="primary" size="small" @click="fillTestAccount('testadmin1', '123456')">一键填入</el-button>
          </div>
          <div class="tip-item">
            <span class="tip-text"><b>👔 开发组长</b> (初审审批节点)：<code>testadmin2</code> / <code>123456</code></span>
            <el-button link type="primary" size="small" @click="fillTestAccount('testadmin2', '123456')">一键填入</el-button>
          </div>
          <div class="tip-item">
            <span class="tip-text"><b>🛡️ 核心 DBA</b> (复核/执行节点)：<code>testadmin3</code> / <code>123456</code></span>
            <el-button link type="primary" size="small" @click="fillTestAccount('testadmin3', '123456')">一键填入</el-button>
          </div>
          <div class="tip-item">
            <span class="tip-text"><b>🔍 安全合规官</b> (合规审查节点)：<code>test_auditor</code> / <code>123456</code></span>
            <el-button link type="primary" size="small" @click="fillTestAccount('test_auditor', '123456')">一键填入</el-button>
          </div>
          <div class="tip-item">
            <span class="tip-text"><b>💻 普通开发</b> (工单申请/查询)：<code>test_dev</code> / <code>123456</code></span>
            <el-button link type="primary" size="small" @click="fillTestAccount('test_dev', '123456')">一键填入</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户多渠道注册弹窗 -->
    <el-dialog
      title="注册系统账号 (User Registration)"
      v-model="registerDialogVisible"
      width="520px"
      destroy-on-close
    >
      <el-alert
        title="重要说明：姓名必填，系统支持同名同姓智能消歧（将自动显示身份证后6位/手机尾号）"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="100px"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="必填，如：张伟、李明" />
        </el-form-item>

        <el-form-item label="注册方式" prop="registerType">
          <el-radio-group v-model="registerForm.registerType">
            <el-radio value="ACCOUNT">自定义账号</el-radio>
            <el-radio value="PHONE">手机号注册</el-radio>
            <el-radio value="EMAIL">企业邮箱注册</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="登录账号" prop="username" v-if="registerForm.registerType === 'ACCOUNT'">
          <el-input v-model="registerForm.username" placeholder="请输入登录账号名（字母数字下划线）" />
        </el-form-item>

        <el-form-item label="手机号码" prop="phone" :required="registerForm.registerType === 'PHONE'">
          <el-input v-model="registerForm.phone" placeholder="请输入11位手机号码" maxlength="11" />
        </el-form-item>

        <el-form-item label="电子邮箱" prop="email" :required="registerForm.registerType === 'EMAIL'">
          <el-input v-model="registerForm.email" placeholder="如：zhangsan@company.com" />
        </el-form-item>

        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="registerForm.idCard" placeholder="选填，用于重名时自动显示后6位消歧标识" maxlength="18" />
        </el-form-item>

        <el-form-item label="业务资源组" prop="resourceGroups">
          <el-select
            v-model="registerForm.resourceGroups"
            multiple
            collapse-tags
            collapse-tags-tooltip
            style="width: 100%;"
            placeholder="选择归属业务组（支持多选）"
          >
            <el-option label="车险承保资源组" value="车险承保资源组" />
            <el-option label="销管系统资源组" value="销管系统资源组" />
            <el-option label="理赔服务核心组" value="理赔服务核心组" />
            <el-option label="水险财产险1000条以下" value="水险财产险1000条以下" />
            <el-option label="农险理赔资源组" value="农险理赔资源组" />
            <el-option label="风勘中心资源组" value="风勘中心资源组" />
            <el-option label="互联网车主服务与理赔快处组" value="互联网车主服务与理赔快处组" />
            <el-option label="默认核心业务资源组" value="默认核心业务资源组" />
          </el-select>
        </el-form-item>

        <el-form-item label="初始角色" prop="role">
          <el-select v-model="registerForm.role" style="width: 100%;">
            <el-option label="DEV - 开发工程师 (工单申请/SQL查询)" value="DEV" />
            <el-option label="DEV_LEAD - 开发组长 (初审审批人)" value="DEV_LEAD" />
            <el-option label="DBA - 数据库管理员 (复审/执行管理)" value="DBA" />
            <el-option label="AUDITOR - 安全审计员 (合规审计)" value="AUDITOR" />
          </el-select>
        </el-form-item>

        <el-form-item label="登录密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入不少于6位密码" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegisterSubmit">
          立即注册并登录
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Iphone, Key, Refresh, Check, Cellphone } from '@element-plus/icons-vue'
import request from '../utils/request'
import { sm2 } from 'sm-crypto'
import { useUserStore } from '../store/user'

const PUBLIC_KEY = import.meta.env.VITE_SM2_PUBLIC_KEY || '04fea943c0bb2c03cefbf0e26eab00b5c7266c3fb7f47e8e80401a2b614315f2b89b0ba40eea69d3322e9942b317a7ecf8415ed7c73b026c02e3f568f0acdcc94e'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('account')
const loading = ref(false)
const sendLoading = ref(false)
const countdown = ref(0)
let timer: number | null = null

// 账号密码表单
const accountFormRef = ref<FormInstance>()
const accountForm = reactive({
  account: '',
  password: ''
})

const accountRules = reactive<FormRules>({
  account: [
    { required: true, message: '请输入用户名、手机号、邮箱或身份证号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ]
})

// 验证码登录表单
const phoneFormRef = ref<FormInstance>()
const phoneForm = reactive({
  phone: '',
  code: ''
})

const phoneRules = reactive<FormRules>({
  phone: [
    { required: true, message: '请输入手机号或邮箱', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入6位验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ]
})

// 扫码登录相关
const channels = [
  { key: 'WECOM', name: '企业微信', icon: '💼' },
  { key: 'DINGTALK', name: '钉钉', icon: '📌' },
  { key: 'FEISHU', name: '飞书', icon: '🕊️' },
  { key: 'SSO', name: '统一 SSO', icon: '🌐' }
]
const currentChannel = ref('WECOM')
const qrInfo = ref<any>(null)
const qrRemainSeconds = ref(180)
const mockScanLoading = ref(false)
let qrPollTimer: number | null = null
let qrCountdownTimer: number | null = null

const getChannelName = (key: string) => {
  const c = channels.find(item => item.key === key)
  return c ? c.name : '移动端 APP'
}

const getChannelIcon = (key: string) => {
  const c = channels.find(item => item.key === key)
  return c ? c.icon : '📱'
}

// 统一登录成功后角色路由分发
const handleLoginSuccess = async (token: string, welcomeMsg?: string) => {
  if (welcomeMsg) {
    ElMessage.success(welcomeMsg)
  } else {
    ElMessage.success('登录成功')
  }
  userStore.token = token
  localStorage.setItem('wmdb_token', token)
  await userStore.fetchUserInfo()

  // 只有超级管理员才跳转平台总览 (/dashboard)，其他角色默认跳转新建工单 (/ticket-create)
  if (userStore.isAdmin || userStore.userRole?.toUpperCase() === 'ADMIN' || userStore.userRole?.toUpperCase() === 'ROLE_ADMIN') {
    router.push('/dashboard')
  } else {
    router.push('/ticket-create')
  }
}

const handleTabChange = (tabName: any) => {
  if (tabName === 'qrcode') {
    refreshQr()
  } else {
    stopQrPolling()
  }
}

const switchChannel = (key: string) => {
  currentChannel.value = key
  refreshQr()
}

const refreshQr = async () => {
  stopQrPolling()
  try {
    const res: any = await request.post(`/v1/auth/qr/generate?channel=${currentChannel.value}`)
    qrInfo.value = res.data
    qrRemainSeconds.value = res.data?.expireSeconds || 180

    // 启动状态轮询与倒计时
    startQrPolling(res.data.qrKey)
  } catch (e) {
    ElMessage.error('生成二维码失败')
  }
}

const startQrPolling = (qrKey: string) => {
  stopQrPolling()

  // 1s 倒计时
  qrCountdownTimer = window.setInterval(() => {
    if (qrRemainSeconds.value > 0) {
      qrRemainSeconds.value--
    } else {
      if (qrInfo.value) qrInfo.value.status = 'EXPIRED'
      stopQrPolling()
    }
  }, 1000)

  // 2s 轮询状态
  qrPollTimer = window.setInterval(async () => {
    try {
      const res: any = await request.get('/v1/auth/qr/status', { params: { qrKey } })
      const data = res.data
      if (data) {
        qrInfo.value.status = data.status
        qrInfo.value.statusMsg = data.statusMsg

        if (data.status === 'CONFIRMED' && data.token) {
          stopQrPolling()
          await handleLoginSuccess(data.token, `扫码授权成功，欢迎【${data.user?.displayName || '用户'}】`)
        } else if (data.status === 'EXPIRED') {
          stopQrPolling()
        }
      }
    } catch (err) {
      // ignore
    }
  }, 2000)
}

const stopQrPolling = () => {
  if (qrPollTimer) {
    clearInterval(qrPollTimer)
    qrPollTimer = null
  }
  if (qrCountdownTimer) {
    clearInterval(qrCountdownTimer)
    qrCountdownTimer = null
  }
}

// 模拟扫码授权
const handleMockScanConfirm = async () => {
  if (!qrInfo.value || !qrInfo.value.qrKey) return
  mockScanLoading.value = true
  try {
    const res: any = await request.post('/v1/auth/qr/mock-scan', {
      qrKey: qrInfo.value.qrKey,
      account: 'testadmin1'
    })
    const data = res.data
    if (data && data.token) {
      await handleLoginSuccess(data.token, `模拟扫码授权成功：${data.user?.displayName || '管理员'}`)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '模拟扫码失败')
  } finally {
    mockScanLoading.value = false
  }
}

// 账号密码登录
const handleAccountLogin = async () => {
  if (!accountFormRef.value) return
  await accountFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      let encryptedPassword = accountForm.password
      try {
        const pk = PUBLIC_KEY.startsWith('04') ? PUBLIC_KEY.substring(2) : PUBLIC_KEY
        const cipher = sm2.doEncrypt(accountForm.password, pk, 1)
        encryptedPassword = cipher.startsWith('04') ? cipher : '04' + cipher
      } catch (e) {
        console.warn('SM2 encryption failed, falling back to plaintext')
      }

      const payload = {
        loginType: 'ACCOUNT_PASSWORD',
        account: accountForm.account.trim(),
        password: encryptedPassword
      }

      const response: any = await request.post('/v1/auth/login', payload)
      const token = response.data.token
      await handleLoginSuccess(token)
    } catch (error: any) {
      // handled
    } finally {
      loading.value = false
    }
  })
}

// 发送验证码
const handleSendCode = async () => {
  if (!phoneForm.phone) {
    ElMessage.warning('请先输入手机号或邮箱')
    return
  }

  sendLoading.value = true
  try {
    const res: any = await request.post('/v1/auth/send-code', { phone: phoneForm.phone.trim() })
    const code = res.data?.code || '123456'
    ElMessage.success(`验证码已发送（测试码：${code}）`)

    countdown.value = 60
    timer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        if (timer) clearInterval(timer)
        timer = null
      }
    }, 1000)
  } catch (error) {
    // handled
  } finally {
    sendLoading.value = false
  }
}

// 验证码快速登录
const handlePhoneLogin = async () => {
  if (!phoneFormRef.value) return
  await phoneFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const payload = {
        loginType: 'PHONE_CODE',
        phone: phoneForm.phone.trim(),
        code: phoneForm.code.trim()
      }

      const response: any = await request.post('/v1/auth/login', payload)
      const token = response.data.token
      await handleLoginSuccess(token)
    } catch (error: any) {
      // handled
    } finally {
      loading.value = false
    }
  })
}

// 多渠道自主注册
const registerDialogVisible = ref(false)
const registerLoading = ref(false)
const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  registerType: 'ACCOUNT',
  username: '',
  realName: '',
  idCard: '',
  phone: '',
  email: '',
  resourceGroups: ['车险承保资源组'],
  role: 'DEV',
  password: ''
})

const registerRules = reactive<FormRules>({
  realName: [
    { required: true, message: '真实姓名必填（用于身份辨识与同名消歧）', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于6位', trigger: 'blur' }
  ]
})

const handleRegisterSubmit = async () => {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    registerLoading.value = true
    try {
      const payload = {
        ...registerForm,
        resourceGroup: (registerForm.resourceGroups || []).join(', ')
      }
      const res: any = await request.post('/v1/auth/register', payload)
      const token = res.data?.token
      if (token) {
        registerDialogVisible.value = false
        await handleLoginSuccess(token, `账号注册成功！已为【${registerForm.realName}】自动登入系统`)
      }
    } catch (error: any) {
      ElMessage.error(error.message || '注册失败')
    } finally {
      registerLoading.value = false
    }
  })
}

const fillTestAccount = (account: string, pass: string) => {
  activeTab.value = 'account'
  accountForm.account = account
  accountForm.password = pass
}

onMounted(() => {
  if (activeTab.value === 'qrcode') {
    refreshQr()
  }
})

onUnmounted(() => {
  stopQrPolling()
})
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%);
  padding: 20px;
  box-sizing: border-box;
}

.login-box {
  width: 460px;
  max-width: 100%;
  padding: 36px 32px 28px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.25);
  box-sizing: border-box;
}

.login-header {
  text-align: center;
  margin-bottom: 22px;
}

.brand-logo {
  font-size: 36px;
  margin-bottom: 4px;
}

.title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 0.5px;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
}

.login-tabs {
  margin-bottom: 12px;
}

.code-input-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.send-code-btn {
  flex-shrink: 0;
  width: 130px;
}

.login-btn {
  width: 100%;
  height: 40px;
  font-size: 15px;
  font-weight: 600;
  margin-top: 6px;
}

/* 扫码登录样式 */
.qr-login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.qr-channel-selector {
  display: flex;
  background: #f1f5f9;
  padding: 4px;
  border-radius: 8px;
  margin-bottom: 16px;
  width: 100%;
}

.channel-tab {
  flex: 1;
  text-align: center;
  padding: 6px 4px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.channel-tab.active {
  background: #ffffff;
  color: #2563eb;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
}

.qr-card-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.qr-box {
  width: 190px;
  height: 190px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  position: relative;
  cursor: default;
}

.qr-box.is-expired {
  cursor: pointer;
}

.qr-code-graphic {
  width: 100%;
  height: 100%;
  position: relative;
}

.qr-svg {
  width: 100%;
  height: 100%;
}

.qr-center-logo {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 32px;
  height: 32px;
  background: #ffffff;
  border-radius: 6px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.qr-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  backdrop-filter: blur(4px);
}

.expired-mask {
  background: rgba(15, 23, 42, 0.85);
  color: #ffffff;
}

.scanned-mask {
  background: rgba(255, 255, 255, 0.92);
  color: #1e293b;
}

.mask-text {
  font-size: 14px;
  font-weight: 600;
}

.mask-sub {
  font-size: 11px;
  opacity: 0.8;
}

.qr-status-info {
  margin-top: 14px;
  text-align: center;
}

.qr-tip-text {
  font-size: 13px;
  color: #334155;
}

.qr-expire-tag {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.mock-scan-area {
  margin-top: 14px;
}

.login-footer-action {
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 13px;
  color: #64748b;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
}

.test-account-tip {
  margin-top: 16px;
  padding: 12px 14px;
  background-color: #f8fafc;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  font-size: 12px;
}

.tip-header {
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.tip-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tip-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #64748b;
}

.tip-item code {
  background: #e2e8f0;
  padding: 2px 5px;
  border-radius: 3px;
  color: #2563eb;
  font-family: monospace;
}
</style>
