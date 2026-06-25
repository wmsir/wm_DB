import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import router from '../router'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 10000 // 请求超时时间
})

// request 拦截器
request.interceptors.request.use(
  config => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}` // 让每个请求携带自定义 token
    }
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)

// response 拦截器
request.interceptors.response.use(
  response => {
    const res = response.data

    // 如果返回的 code 不是 200，说明业务接口有异常
    if (res.code && res.code !== 200) {
      ElMessage({
        message: res.message || 'Error',
        type: 'error',
        duration: 5 * 1000
      })
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      // 成功则直接返回内部 data（处理统一 Response Result 的解包）
      return res.data !== undefined ? res : response
    }
  },
  error => {
    console.log('err' + error) // for debug

    let message = error.message
    if (error.response) {
      const status = error.response.status
      if (status === 401) {
        message = '认证失败或 Token 已过期，请重新登录'
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      } else if (status === 403) {
        message = '权限不足，拒绝访问'
      } else if (status === 500) {
        message = error.response.data?.message || '系统内部异常'
      }
    }

    ElMessage({
      message: message,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default request
