import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { message } from 'antd'
import type { ApiResponse } from '@/types/diagnosis'

// 创建axios实例
const apiClient: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 可以在这里添加token等认证信息
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 防止重复显示错误消息的机制
let lastErrorMessage: string | null = null
let lastErrorTime = 0
const ERROR_MESSAGE_THROTTLE = 2000 // 2秒内不重复显示相同错误

const shouldShowError = (errorMsg: string): boolean => {
  const now = Date.now()
  if (errorMsg === lastErrorMessage && now - lastErrorTime < ERROR_MESSAGE_THROTTLE) {
    return false // 相同错误在2秒内不重复显示
  }
  lastErrorMessage = errorMsg
  lastErrorTime = now
  return true
}

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const { data } = response
    // 如果code不是200，显示错误信息
    if (data.code !== 200) {
      const errorMsg = data.message || '请求失败'
      if (shouldShowError(errorMsg)) {
        message.error(errorMsg)
      }
      return Promise.reject(new Error(errorMsg))
    }
    return response
  },
  (error) => {
    // 处理网络错误等
    let errorMsg = ''
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        errorMsg = '未授权，请重新登录'
        // 可以在这里处理登录跳转
      } else if (status === 403) {
        errorMsg = '没有权限'
      } else if (status === 404) {
        errorMsg = '请求的资源不存在'
      } else if (status === 500) {
        errorMsg = '服务器错误'
      } else {
        errorMsg = data?.message || '请求失败'
      }
    } else if (error.request) {
      errorMsg = '网络错误，请检查网络连接'
    } else {
      errorMsg = error.message || '请求失败'
    }

    // 防止重复显示相同错误
    if (shouldShowError(errorMsg)) {
      message.error(errorMsg)
    }

    return Promise.reject(error)
  }
)

export default apiClient

