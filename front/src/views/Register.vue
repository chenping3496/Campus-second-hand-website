<template>
  <div class="auth-page">
    <div class="auth-card card">
      <h2 class="auth-title">注册</h2>
      <p class="auth-subtitle">加入校园二手交易平台</p>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名 *</label>
          <input v-model="form.username" type="text" class="form-input" :class="{ error: errors.username }" placeholder="3-20个字符" />
          <p v-if="errors.username" class="form-error">{{ errors.username }}</p>
        </div>
        <div class="form-group">
          <label>密码 *</label>
          <input v-model="form.password" type="password" class="form-input" :class="{ error: errors.password }" placeholder="6-20个字符" />
          <p v-if="errors.password" class="form-error">{{ errors.password }}</p>
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input v-model="form.nickname" type="text" class="form-input" placeholder="选填" />
        </div>
        <div class="form-group">
          <label>手机号</label>
          <input v-model="form.phone" type="text" class="form-input" placeholder="选填" />
        </div>
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="form.email" type="email" class="form-input" placeholder="选填" />
        </div>
        <button type="submit" class="btn btn-primary btn-lg auth-submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p class="auth-link">
        已有账号？<router-link to="/login">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: '',
  email: ''
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
  if (form.username.trim().length < 3 || form.username.trim().length > 20) {
    errors.username = '用户名长度为3-20个字符'
    return false
  }
  if (!form.password) {
    errors.password = '请输入密码'
    return false
  }
  if (form.password.length < 6 || form.password.length > 20) {
    errors.password = '密码长度为6-20个字符'
    return false
  }
  return true
}

async function handleRegister() {
  if (!validate()) return
  loading.value = true
  try {
    await auth.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined
    })
    window.$toast?.success('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    window.$toast?.error(err.message || '注册失败')
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
  max-width: 420px;
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
