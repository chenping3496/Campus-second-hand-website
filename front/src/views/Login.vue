<template>
  <div class="auth-page">
    <div class="auth-card card">
      <h2 class="auth-title">登录</h2>
      <p class="auth-subtitle">欢迎回到校园二手交易平台</p>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名</label>
          <input
            v-model="form.username"
            type="text"
            class="form-input"
            :class="{ error: errors.username }"
            placeholder="请输入用户名"
          />
          <p v-if="errors.username" class="form-error">{{ errors.username }}</p>
        </div>
        <div class="form-group">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            class="form-input"
            :class="{ error: errors.password }"
            placeholder="请输入密码"
          />
          <p v-if="errors.password" class="form-error">{{ errors.password }}</p>
        </div>
        <button type="submit" class="btn btn-primary btn-lg auth-submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <p class="auth-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({
  username: '',
  password: ''
})
const errors = reactive({})
const loading = ref(false)

function validate() {
  errors.username = ''
  errors.password = ''
  if (!form.username.trim()) {
    errors.username = '请输入用户名'
    return false
  }
  if (!form.password) {
    errors.password = '请输入密码'
    return false
  }
  return true
}

async function handleLogin() {
  if (!validate()) return
  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    window.$toast?.success('登录成功')
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null')
    const redirect = userInfo?.role === 'ADMIN' ? '/admin/users' : (route.query.redirect || '/')
    router.push(redirect)
  } catch (err) {
    window.$toast?.error(err.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - var(--header-height) - 80px);
  background: var(--bg);
  padding: 40px 16px;
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 36px;
}

.auth-title {
  font-size: 24px;
  font-weight: 700;
  text-align: center;
}

.auth-subtitle {
  font-size: 14px;
  color: var(--text-muted);
  text-align: center;
  margin-top: 8px;
}

.auth-submit {
  width: 100%;
  margin-top: 20px;
}

.auth-link {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--text-muted);
}

.auth-link a {
  color: var(--primary);
  font-weight: 500;
}

.auth-link a:hover {
  text-decoration: underline;
}
</style>
