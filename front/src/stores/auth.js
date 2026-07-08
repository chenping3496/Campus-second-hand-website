import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, register as registerApi } from '@/api/auth'
import { getUserInfo as getUserInfoApi } from '@/api/user'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const username = computed(() => user.value?.nickname || user.value?.username || '')
  const avatar = computed(() => user.value?.avatar || '')

  function setAuth(authToken, userInfo) {
    token.value = authToken
    user.value = userInfo
    localStorage.setItem('token', authToken)
    localStorage.setItem('userInfo', JSON.stringify(userInfo))
  }

  function clearAuth() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function login(credentials) {
    const data = await loginApi(credentials)
    setAuth(data.token, data.userInfo || data.user)
    return data
  }

  async function register(formData) {
    const data = await registerApi(formData)
    return data
  }

  async function fetchUserInfo() {
    const data = await getUserInfoApi()
    user.value = data
    localStorage.setItem('userInfo', JSON.stringify(data))
    return data
  }

  function logout() {
    clearAuth()
  }

  return {
    user,
    token,
    isLoggedIn,
    isAdmin,
    username,
    avatar,
    login,
    register,
    fetchUserInfo,
    logout
  }
})
