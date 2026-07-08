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
            <!-- Verification badge -->
            <div class="verify-badge-sidebar" v-if="user?.role !== 'ADMIN'">
              <span :class="['tag', verifyBadgeClass]">{{ verifyBadgeText }}</span>
            </div>
          </div>
          <div class="sidebar-links">
            <button :class="['side-link', { active: section === 'info' }]" @click="section = 'info'">基本信息</button>
            <button :class="['side-link', { active: section === 'verification' }]" @click="section = 'verification'">身份认证</button>
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

          <!-- Verification section -->
          <div v-if="section === 'verification'" class="card">
            <h3 class="section-title">身份认证</h3>

            <!-- Status banner -->
            <div :class="['verify-status-banner', 'banner-' + verifyStatus]">
              <span class="banner-icon">{{ verifyStatusIcon }}</span>
              <div>
                <strong>{{ verifyStatusTitle }}</strong>
                <p>{{ verifyStatusDesc }}</p>
                <p v-if="verifyInfo?.verificationRemark" class="remark-text">备注：{{ verifyInfo.verificationRemark }}</p>
              </div>
            </div>

            <!-- APPROVED: show details -->
            <div v-if="verifyStatus === 'APPROVED'" class="verify-details">
              <div class="detail-row"><span class="dl">真实姓名</span><span class="dv">{{ verifyInfo.realName || '-' }}</span></div>
              <div class="detail-row"><span class="dl">身份类型</span><span class="dv">{{ verifyInfo.identityType === 'TEACHER' ? '教师' : '学生' }}</span></div>
              <div class="detail-row"><span class="dl">{{ verifyInfo.identityType === 'TEACHER' ? '工号' : '学号' }}</span><span class="dv">{{ verifyInfo.identityNumber || '-' }}</span></div>
            </div>

            <!-- UNVERIFIED / REJECTED: submit form -->
            <div v-if="verifyStatus === 'UNVERIFIED' || verifyStatus === 'REJECTED'" class="verify-form">
              <div class="divider"></div>
              <h4>{{ verifyStatus === 'REJECTED' ? '重新提交认证' : '提交身份认证' }}</h4>
              <form @submit.prevent="handleSubmitVerify">
                <div class="form-group">
                  <label>真实姓名 *</label>
                  <input v-model="verifyForm.realName" type="text" class="form-input" placeholder="请输入真实姓名" />
                </div>
                <div class="form-group">
                  <label>身份类型 *</label>
                  <div class="identity-options">
                    <label :class="['id-option', { active: verifyForm.identityType === 'STUDENT' }]">
                      <input v-model="verifyForm.identityType" type="radio" value="STUDENT" /> 🎓 学生
                    </label>
                    <label :class="['id-option', { active: verifyForm.identityType === 'TEACHER' }]">
                      <input v-model="verifyForm.identityType" type="radio" value="TEACHER" /> 👨‍🏫 教师
                    </label>
                  </div>
                </div>
                <div class="form-group">
                  <label>{{ verifyForm.identityType === 'TEACHER' ? '工号' : '学号' }} *</label>
                  <input v-model="verifyForm.identityNumber" type="text" class="form-input" :placeholder="verifyForm.identityType === 'TEACHER' ? '请输入工号' : '请输入学号'" />
                </div>
                <div class="form-group">
                  <label>证件照上传</label>
                  <ImageUpload v-model="verifyForm.idCardImage" :max="1" />
                  <p class="hint">请上传学生证/校园卡/教师证照片</p>
                </div>
                <button type="submit" class="btn btn-primary" :disabled="verifySubmitting" style="width:100%">
                  {{ verifySubmitting ? '提交中...' : '提交认证' }}
                </button>
              </form>
            </div>

            <!-- REJECTED + no pending appeal: show appeal -->
            <div v-if="verifyStatus === 'REJECTED' && !verifyInfo?.hasPendingAppeal" class="appeal-section">
              <div class="divider"></div>
              <h4>提交申诉</h4>
              <p class="appeal-hint">如认为驳回有误，可提交申诉说明情况</p>
              <form @submit.prevent="handleSubmitAppeal">
                <div class="form-group">
                  <label>申诉原因 *</label>
                  <textarea v-model="appealForm.reason" class="form-textarea" placeholder="请详细说明..." rows="3"></textarea>
                </div>
                <div class="form-group">
                  <label>补充材料（可选）</label>
                  <ImageUpload v-model="appealForm.image" :max="1" />
                </div>
                <button type="submit" class="btn btn-warning" :disabled="appealSubmitting" style="width:100%">
                  {{ appealSubmitting ? '提交中...' : '提交申诉' }}
                </button>
              </form>
            </div>

            <!-- PENDING appeal notice -->
            <div v-if="verifyStatus === 'REJECTED' && verifyInfo?.hasPendingAppeal" class="pending-appeal-notice">
              ⏳ 申诉已提交，正在处理中，请耐心等待
            </div>

            <!-- FROZEN notice -->
            <div v-if="verifyStatus === 'FROZEN'" class="frozen-notice">
              ⚠️ 您的认证已被冻结，如有疑问请联系管理员
            </div>
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { updateUserInfo, changePassword } from '@/api/user'
import { getVerificationStatus, submitVerification, submitAppeal } from '@/api/verification'
import ImageUpload from '@/components/common/ImageUpload.vue'

const auth = useAuthStore()

const user = ref(null)
const section = ref('info')
const verifyInfo = ref(null)

const profileForm = reactive({ nickname: '', phone: '', email: '', studentId: '', dormitory: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdError = ref('')
const updating = ref(false)
const changingPwd = ref(false)

// Verification form
const verifyForm = reactive({ realName: '', identityType: 'STUDENT', identityNumber: '', idCardImage: [] })
const verifySubmitting = ref(false)

// Appeal form
const appealForm = reactive({ reason: '', image: [] })
const appealSubmitting = ref(false)

const verifyStatus = computed(() => verifyInfo.value?.verificationStatus || 'UNVERIFIED')

const verifyStatusIcon = computed(() => {
  const m = { UNVERIFIED: '❓', PENDING: '⏳', APPROVED: '✅', REJECTED: '❌', FROZEN: '🔒' }
  return m[verifyStatus.value] || '❓'
})
const verifyStatusTitle = computed(() => {
  const m = { UNVERIFIED: '未认证', PENDING: '审核中', APPROVED: '已认证通过', REJECTED: '认证未通过', FROZEN: '认证已冻结' }
  return m[verifyStatus.value] || '未知'
})
const verifyStatusDesc = computed(() => {
  const m = {
    UNVERIFIED: '请完成身份认证以解锁全部交易功能',
    PENDING: '认证申请正在审核中，请耐心等待',
    APPROVED: '恭喜！您可以使用全部交易功能',
    REJECTED: '认证未通过审核，请根据反馈重新提交或发起申诉',
    FROZEN: '认证已被管理员冻结'
  }
  return m[verifyStatus.value] || ''
})

const verifyBadgeClass = computed(() => {
  const m = { UNVERIFIED: 'tag-default', PENDING: 'tag-warning', APPROVED: 'tag-success', REJECTED: 'tag-danger', FROZEN: 'tag-info' }
  return m[verifyStatus.value] || 'tag-default'
})
const verifyBadgeText = computed(() => {
  const m = { UNVERIFIED: '未认证', PENDING: '审核中', APPROVED: '已认证', REJECTED: '被驳回', FROZEN: '已冻结' }
  return m[verifyStatus.value] || '未认证'
})

async function fetchVerifyStatus() {
  if (user.value?.role === 'ADMIN') return
  try { verifyInfo.value = await getVerificationStatus() }
  catch { /* ignore */ }
}

onMounted(async () => {
  try {
    user.value = await auth.fetchUserInfo()
    profileForm.nickname = user.value.nickname || ''
    profileForm.phone = user.value.phone || ''
    profileForm.email = user.value.email || ''
    profileForm.studentId = user.value.studentId || ''
    profileForm.dormitory = user.value.dormitory || ''
    await fetchVerifyStatus()
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
  } finally { updating.value = false }
}

async function handleChangePassword() {
  pwdError.value = ''
  if (!pwdForm.oldPassword || !pwdForm.newPassword) { pwdError.value = '请填写完整信息'; return }
  if (pwdForm.newPassword.length < 6) { pwdError.value = '新密码至少6个字符'; return }
  if (pwdForm.newPassword !== pwdForm.confirmPassword) { pwdError.value = '两次输入的新密码不一致'; return }
  changingPwd.value = true
  try {
    await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    window.$toast?.success('密码修改成功')
    pwdForm.oldPassword = ''; pwdForm.newPassword = ''; pwdForm.confirmPassword = ''
  } catch (err) {
    window.$toast?.error(err.message || '修改失败')
  } finally { changingPwd.value = false }
}

async function handleSubmitVerify() {
  if (!verifyForm.realName.trim()) return window.$toast?.warning('请输入真实姓名')
  if (!verifyForm.identityNumber.trim()) return window.$toast?.warning('请输入学号/工号')
  verifySubmitting.value = true
  try {
    await submitVerification({
      realName: verifyForm.realName,
      identityType: verifyForm.identityType,
      identityNumber: verifyForm.identityNumber,
      idCardImage: verifyForm.idCardImage.length ? verifyForm.idCardImage[0] : null
    })
    window.$toast?.success('认证信息已提交，请等待审核')
    await fetchVerifyStatus()
  } catch (err) {
    window.$toast?.error(err.message || '提交失败')
  } finally { verifySubmitting.value = false }
}

async function handleSubmitAppeal() {
  if (!appealForm.reason.trim()) return window.$toast?.warning('请输入申诉原因')
  appealSubmitting.value = true
  try {
    await submitAppeal({
      reason: appealForm.reason,
      image: appealForm.image.length ? appealForm.image[0] : null
    })
    window.$toast?.success('申诉已提交')
    await fetchVerifyStatus()
    appealForm.reason = ''; appealForm.image = []
  } catch (err) {
    window.$toast?.error(err.message || '申诉提交失败')
  } finally { appealSubmitting.value = false }
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

.verify-badge-sidebar { margin-top: 8px; }

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

/* Verification status banner */
.verify-status-banner {
  display: flex; gap: 14px; align-items: flex-start;
  padding: 18px; border-radius: var(--radius); margin-bottom: 20px;
}
.verify-status-banner strong { font-size: 16px; }
.verify-status-banner p { font-size: 13px; color: var(--text-secondary); margin-top: 4px; }
.remark-text { font-size: 12px; font-style: italic; color: var(--text-muted) !important; margin-top: 6px !important; }

.banner-UNVERIFIED { background: #f5f5f5; }
.banner-PENDING { background: #FFF8E1; }
.banner-APPROVED { background: #E8F5E9; }
.banner-REJECTED { background: #FFEBEE; }
.banner-FROZEN { background: #ECEFF1; }
.banner-icon { font-size: 28px; flex-shrink: 0; }

.verify-details {
  margin-bottom: 16px;
}
.detail-row { display: flex; padding: 10px 0; border-bottom: 1px solid var(--border-light); font-size: 14px; }
.dl { color: var(--text-muted); width: 80px; flex-shrink: 0; }
.dv { color: var(--text); font-weight: 500; }

.divider { height: 1px; background: var(--border-light); margin: 20px 0; }

.verify-form h4, .appeal-section h4 { font-size: 16px; font-weight: 600; margin-bottom: 14px; }

.identity-options { display: flex; gap: 12px; }
.id-option {
  flex: 1; padding: 12px; border: 2px solid var(--border); border-radius: var(--radius);
  text-align: center; cursor: pointer; transition: all 0.2s; font-size: 14px;
}
.id-option input { display: none; }
.id-option.active { border-color: var(--primary); background: var(--primary-light); }

.hint { font-size: 12px; color: var(--text-muted); margin-top: 4px; }
.appeal-hint { font-size: 13px; color: var(--text-secondary); margin-bottom: 14px; }

.btn-warning { background: var(--secondary); color: #fff; }
.btn-warning:hover { background: #E65100; }

.pending-appeal-notice {
  padding: 16px; background: #FFF8E1; border-radius: var(--radius);
  text-align: center; font-size: 14px; color: #F57F17;
}
.frozen-notice {
  padding: 16px; background: #FFF3E0; border-radius: var(--radius);
  text-align: center; font-size: 14px; color: #E65100;
}

@media (max-width: 768px) {
  .profile-layout { grid-template-columns: 1fr; }
  .profile-sidebar { padding: 16px; }
  .sidebar-links { flex-direction: row; flex-wrap: wrap; }
}
</style>
