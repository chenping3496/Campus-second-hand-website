<template>
  <div class="profile-page">
    <div class="container">
      <h2 class="page-title">个人中心</h2>

      <div class="profile-layout">
        <!-- Sidebar -->
        <div class="profile-sidebar card">
          <div class="sidebar-user">
            <img v-if="user?.avatar" :src="user.avatar" class="sidebar-avatar" />
            <span v-else class="sidebar-avatar-placeholder">{{ user?.username?.charAt(0) || '?' }}</span>
            <h3 class="sidebar-name">{{ user?.nickname || user?.username || '用户' }}</h3>
            <span :class="['tag', user?.role === 'ADMIN' ? 'tag-warning' : 'tag-primary']">
              {{ user?.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </span>
          </div>
          <div class="sidebar-links">
            <button :class="['side-link', { active: section === 'info' }]" @click="section = 'info'">基本信息</button>
            <button :class="['side-link', { active: section === 'password' }]" @click="section = 'password'">修改密码</button>
          </div>
        </div>

        <!-- Content -->
        <div class="profile-content">
          <!-- Info section -->
          <div v-if="section === 'info'" class="card">
            <h3 class="section-title">基本信息</h3>
            <form @submit.prevent="handleUpdateInfo">
              <div class="form-group">
                <label>用户名</label>
                <input :value="user?.username" type="text" class="form-input" disabled />
              </div>
              <div class="form-group">
                <label>昵称</label>
                <input v-model="profileForm.nickname" type="text" class="form-input" placeholder="设置昵称" />
              </div>
              <div class="form-group">
                <label>手机号</label>
                <input v-model="profileForm.phone" type="text" class="form-input" placeholder="绑定手机号" />
              </div>
              <div class="form-group">
                <label>邮箱</label>
                <input v-model="profileForm.email" type="email" class="form-input" placeholder="绑定邮箱" />
              </div>
              <div class="form-group">
                <label>学号</label>
                <input v-model="profileForm.studentId" type="text" class="form-input" placeholder="输入学号" />
              </div>
              <div class="form-group">
                <label>宿舍</label>
                <input v-model="profileForm.dormitory" type="text" class="form-input" placeholder="如：北区3号楼" />
              </div>
              <button type="submit" class="btn btn-primary" :disabled="updating">
                {{ updating ? '保存中...' : '保存修改' }}
              </button>
            </form>
          </div>

          <!-- Password section -->
          <div v-if="section === 'password'" class="card">
            <h3 class="section-title">修改密码</h3>
            <form @submit.prevent="handleChangePassword">
              <div class="form-group">
                <label>当前密码</label>
                <input v-model="pwdForm.oldPassword" type="password" class="form-input" />
              </div>
              <div class="form-group">
                <label>新密码</label>
                <input v-model="pwdForm.newPassword" type="password" class="form-input" placeholder="6-20个字符" />
              </div>
              <div class="form-group">
                <label>确认新密码</label>
                <input v-model="pwdForm.confirmPassword" type="password" class="form-input" />
              </div>
              <p v-if="pwdError" class="form-error">{{ pwdError }}</p>
              <button type="submit" class="btn btn-primary" :disabled="changingPwd">
                {{ changingPwd ? '修改中...' : '修改密码' }}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { updateUserInfo, changePassword } from '@/api/user'

const auth = useAuthStore()

const user = ref(null)
const section = ref('info')

const profileForm = reactive({ nickname: '', phone: '', email: '', studentId: '', dormitory: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdError = ref('')
const updating = ref(false)
const changingPwd = ref(false)

onMounted(async () => {
  try {
    user.value = await auth.fetchUserInfo()
    profileForm.nickname = user.value.nickname || ''
    profileForm.phone = user.value.phone || ''
    profileForm.email = user.value.email || ''
    profileForm.studentId = user.value.studentId || ''
    profileForm.dormitory = user.value.dormitory || ''
  } catch { /* ignore */ }
})

async function handleUpdateInfo() {
  updating.value = true
  try {
    await updateUserInfo({
      nickname: profileForm.nickname || undefined,
      phone: profileForm.phone || undefined,
      email: profileForm.email || undefined,
      studentId: profileForm.studentId || undefined,
      dormitory: profileForm.dormitory || undefined
    })
    window.$toast?.success('信息已更新')
    auth.fetchUserInfo()
  } catch (err) {
    window.$toast?.error(err.message || '更新失败')
  } finally {
    updating.value = false
  }
}

async function handleChangePassword() {
  pwdError.value = ''
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    pwdError.value = '请填写完整信息'
    return
  }
  if (pwdForm.newPassword.length < 6) {
    pwdError.value = '新密码至少6个字符'
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  changingPwd.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    window.$toast?.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
  } catch (err) {
    window.$toast?.error(err.message || '修改失败')
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: 600; margin-bottom: 24px; }

.profile-layout {
  display: grid; grid-template-columns: 240px 1fr; gap: 24px;
}

.profile-sidebar { padding: 24px; }

.sidebar-user { text-align: center; }

.sidebar-avatar { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; }
.sidebar-avatar-placeholder {
  display: inline-flex; align-items: center; justify-content: center;
  width: 72px; height: 72px; border-radius: 50%;
  background: var(--primary); color: #fff; font-size: 28px; font-weight: 600;
}

.sidebar-name { font-size: 18px; font-weight: 600; margin-top: 10px; }

.sidebar-links {
  display: flex; flex-direction: column; gap: 4px;
  margin-top: 24px;
}

.side-link {
  padding: 10px 14px; text-align: left;
  background: none; border-radius: var(--radius);
  font-size: 14px; color: var(--text-secondary);
  transition: all 0.2s;
}

.side-link:hover { background: var(--bg); }
.side-link.active { background: var(--primary-light); color: var(--primary); font-weight: 500; }

.profile-content .card { padding: 28px; }

.section-title { font-size: 18px; font-weight: 600; margin-bottom: 20px; }

@media (max-width: 768px) {
  .profile-layout { grid-template-columns: 1fr; }
  .profile-sidebar { padding: 16px; }
  .sidebar-links { flex-direction: row; flex-wrap: wrap; }
}
</style>
